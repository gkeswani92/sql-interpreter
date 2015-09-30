package parser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import operators.JoinOperator;
import operators.Operator;
import operators.ProjectOperator;
import operators.ScanOperator;
import operators.SelectOperator;
import utils.databaseCatalog;

/**
 * Class for getting started with JSQLParser. Reads SQL statements from
 * a file and extracts the elements of the SQL query to be further evaluated.
 * @author tmm259
 */
public class Interpreter {

	private static final String queriesFile = "sql/testQueries.sql";
	
	public static void main(String[] args) {
		String inputSrcDir;
		if(args.length == 2){
			inputSrcDir = args[0];
			databaseCatalog.getInstance().buildDbCatalog(inputSrcDir);
		}	
		try {
			CCJSqlParser parser = new CCJSqlParser(new FileReader(queriesFile));
			Statement statement;
			while ((statement = parser.Statement()) != null) {
				System.out.println("Read statement: " + statement);

				Select select = (Select) statement;
                PlainSelect body = (PlainSelect) select.getSelectBody();
                @SuppressWarnings("unchecked")
				List<SelectItem> selectAttr = body.getSelectItems();
                
                //If there are no joins in the query
                if (body.getJoins() == null) {
                	
	                //Decision statements to decide from which operator to enter
	                if (selectAttr.size() == 1 && selectAttr.get(0).toString() == "*") { 
	                	
	                	//Enter from the scan clause if query has no where clause
	                	if (body.getWhere() == null) {
	                		ScanOperator op = new ScanOperator(body.getFromItem().toString());
	                		op.dump();
	                	}
	                	
	                	// Enter from selectOperator if query has where clause
	                	else {  		
	                		System.out.println("Where clause is:  " + body.getWhere());
	                        ScanOperator scanOp = new ScanOperator(body.getFromItem().toString());
	                        SelectOperator selOp = new SelectOperator(body.getWhere(), scanOp);	                              
	                        selOp.dump();
	                	}
	                } 
	                
	                //If the query consists of projections
	                else {    	
	            		handleProjectWithoutJoin(body);
	                }     
				} 
                
                //If the query consists of joins
                else {
                	//Join expression needs to be created in any case
                	Operator root = handleJoin(body);
                	
                	//Decision statements to decide whether to flush the output or to make it a child of project
	                if (selectAttr.size() == 1 && selectAttr.get(0).toString() == "*") 
	                	root.dump();
	                else {
	                	System.out.println("Projection has been called on the join operator");
	                	ProjectOperator projOp = new ProjectOperator(body, root);
	                	projOp.dump();
	                }
                }
			}
		} catch (Exception e) {
			System.err.println("Exception occurred during parsing");
			e.printStackTrace();
		}
	}

	/**
	 * Depending on whether the where clause is present or not, we 
	 * decide the child i.e scan or select 
	 * @param body
	 */
	private static void handleProjectWithoutJoin(PlainSelect body) {
		             	
		ScanOperator scanOp = new ScanOperator(body.getFromItem().toString());
		ProjectOperator projOp = null;
		
		if(body.getWhere()!=null) {
		    SelectOperator child = new SelectOperator(body.getWhere(), scanOp);
		    projOp = new ProjectOperator(body, child);
		}
		else {
			projOp = new ProjectOperator(body, scanOp);
		}
		projOp.dump();
	}
	
	/**
	 * Takes in the select body, visits the where clause(to construct the query plan) and calls the joinOperator
	 * @param body select body
	 */
	public static Operator handleJoin(PlainSelect body) {
		Map<String, List<Operator>> tableOperators = new HashMap<String, List<Operator>>();
		List<Entry<List<String>,Expression>> joins = new ArrayList<>();
		
		// If where clause exists, call visitor class to get map of basic operators and list of join conditions
		if (body.getWhere() != null) {
			WhereBuilder where = new WhereBuilder(tableOperators, joins);
			body.getWhere().accept(where);
		}
		
		List<String> currentLeftJoinTables = new ArrayList<>();
		
		// First left table comes from the FromItem
		currentLeftJoinTables.add(body.getFromItem().toString());
		String currentRightJoinTable;
		
		Operator temp, root = null;
		
		for (Object exp: body.getJoins()) {
			
			currentRightJoinTable = ((Join)exp).getRightItem().toString();
			Expression finalJoinCondition = null;
			
			// If the root is null and there is just 1 table in the left list of tables, create
			// join operator tree with left and right table basic operators as left and right children
			if (root == null && currentLeftJoinTables.size() == 1) {
				finalJoinCondition = getJoinCondition(joins, currentLeftJoinTables.get(0), currentRightJoinTable);
				temp = new JoinOperator(finalJoinCondition, 
						getBasicOperator(tableOperators, currentLeftJoinTables.get(0)), 
						getBasicOperator(tableOperators, currentRightJoinTable));
				root = temp;
			} 
			// Add 1 table from join(joins from select body) into the left current join list
			// Check if any of the tables in the left list have a join condition with the right table
			// If they do, conjunct all the join conditions and create a join operator with the conjunct join condition
			// Use old root as the left child and right table as right child
			else {
				for (String leftTable : currentLeftJoinTables) {
					Expression currentTableJoin = getJoinCondition(joins, leftTable, currentRightJoinTable);
					if (finalJoinCondition == null) {
						finalJoinCondition = currentTableJoin;
					} else {
						finalJoinCondition = new AndExpression(finalJoinCondition, currentTableJoin);
					}
				}
				temp = new JoinOperator(finalJoinCondition, root, getBasicOperator(tableOperators, currentRightJoinTable));
				root = temp;
			}
			
			// Add the current right table into the list of left tables for next iteration
			currentLeftJoinTables.add(currentRightJoinTable);
		}
		return root;
	}
	
	/**
	 * Get the basic(select or scan) operator tree (could be multiple select operators with AND conjunct) for the input table
	 * @param tableOperators map of table name to basic operators from where clause
	 * @param tableName
	 * @return operator tree for table name
	 */
	public static Operator getBasicOperator(Map<String, List<Operator>> tableOperators, String tableName) {
		
		Operator op;
		// If entry for table name does not exist in the map, use scan operator
		if (tableOperators.get(tableName) == null || tableOperators.get(tableName).isEmpty()) {
			op = new ScanOperator(tableName);
		} else {
			op = tableOperators.get(tableName).get(0);
		}
		return op;
	}
	
	/**
	 * Get join condition on the input tables, if multiple join conditions exists, conjunct them using AND
	 * @param joins list of all joins from the where clause
	 * @param leftTable left join table
	 * @param rightTable right join table
	 * @return final join condition
	 */
	public static Expression getJoinCondition(List<Entry<List<String>,Expression>> joins, String leftTable, String rightTable) {
		
		Expression finalJoinExpression = null;
		for (Entry<List<String>,Expression> join: joins) {
			List<String> tables = join.getKey();
			// If join condition contains both tables
			if (tables.contains(leftTable) && tables.contains(rightTable)) {
				if (finalJoinExpression == null) {
					finalJoinExpression = join.getValue();
				}
				// If tables have multiple join conditions, conjunct using AND
				else {
					finalJoinExpression = new AndExpression(finalJoinExpression, join.getValue());
				}
			}
		}
		return finalJoinExpression;
	}
}
package parser;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;
import utils.Tuple;

/**
 * Implements the ExpressionVisitor. Takes in a tuple and evaluates the expression with
 * the values of the tuple. Updates the boolean variable in the tuple accordingly.
 * @author tanvimehta
 *
 */

// TODO: RIGHT SIDE OF EXPRESSION COULD BE ANOTHER COLUMN REFERENCE!!!!!!! 
// CHECK FIRST!!!!!!!!!!!!!!!!!!

public class ExpressionEvaluator implements ExpressionVisitor {

	Tuple currTuple;
	
	public ExpressionEvaluator(Tuple tuple) {
		currTuple = tuple;
	}
	
	@Override
	public void visit(EqualsTo arg0) {
		Long left = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getLeftExpression()).getColumnName()).toString());
		
		if (arg0.getRightExpression() instanceof LongValue) {
			LongValue rightLong = (LongValue)arg0.getRightExpression();
			if (left == rightLong.getValue())
				currTuple.setIsSatisfies(true);
		} else if (arg0.getRightExpression() instanceof Column) {
			Long rightColumn = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getRightExpression()).getColumnName()).toString());
			if (left == rightColumn) {
				currTuple.setIsSatisfies(true);
			}
		}
	}

	@Override
	public void visit(GreaterThan arg0) {
		Long left = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getLeftExpression()).getColumnName()).toString());
		
		if (arg0.getRightExpression() instanceof LongValue) {
			LongValue rightLong = (LongValue)arg0.getRightExpression();
			if (left > rightLong.getValue())
				currTuple.setIsSatisfies(true);
		} else if (arg0.getRightExpression() instanceof Column) {
			Long rightColumn = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getRightExpression()).getColumnName()).toString());
			if (left > rightColumn) {
				currTuple.setIsSatisfies(true);
			}
		}
		
	}

	@Override
	public void visit(GreaterThanEquals arg0) {
		Long left = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getLeftExpression()).getColumnName()).toString());
		
		if (arg0.getRightExpression() instanceof LongValue) {
			LongValue rightLong = (LongValue)arg0.getRightExpression();
			if (left >= rightLong.getValue())
				currTuple.setIsSatisfies(true);
		} else if (arg0.getRightExpression() instanceof Column) {
			Long rightColumn = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getRightExpression()).getColumnName()).toString());
			if (left >= rightColumn) {
				currTuple.setIsSatisfies(true);
			}
		}
		
	}
	
	@Override
	public void visit(MinorThan arg0) {
		Long left = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getLeftExpression()).getColumnName()).toString());
		
		if (arg0.getRightExpression() instanceof LongValue) {
			LongValue rightLong = (LongValue)arg0.getRightExpression();
			if (left < rightLong.getValue())
				currTuple.setIsSatisfies(true);
		} else if (arg0.getRightExpression() instanceof Column) {
			Long rightColumn = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getRightExpression()).getColumnName()).toString());
			if (left < rightColumn) {
				currTuple.setIsSatisfies(true);
			}
		}
		
	}

	@Override
	public void visit(MinorThanEquals arg0) {
		Long left = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getLeftExpression()).getColumnName()).toString());
		
		if (arg0.getRightExpression() instanceof LongValue) {
			LongValue rightLong = (LongValue)arg0.getRightExpression();
			if (left <= rightLong.getValue())
				currTuple.setIsSatisfies(true);
		} else if (arg0.getRightExpression() instanceof Column) {
			Long rightColumn = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getRightExpression()).getColumnName()).toString());
			if (left <= rightColumn) {
				currTuple.setIsSatisfies(true);
			}
		}		
	}

	@Override
	public void visit(NotEqualsTo arg0) {
		Long left = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getLeftExpression()).getColumnName()).toString());
		
		if (arg0.getRightExpression() instanceof LongValue) {
			LongValue rightLong = (LongValue)arg0.getRightExpression();
			if (left != rightLong.getValue())
				currTuple.setIsSatisfies(true);
		} else if (arg0.getRightExpression() instanceof Column) {
			Long rightColumn = Long.valueOf(currTuple.getValueForAttr(((Column)arg0.getRightExpression()).getColumnName()).toString());
			if (left != rightColumn) {
				currTuple.setIsSatisfies(true);
			}
		}		
	}
	
	@Override
	public void visit(LongValue arg0) {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void visit(Column arg0) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void visit(AndExpression arg0) {
		// Save the tuple boolean after left evaluation
		arg0.getLeftExpression().accept(this);
		Boolean left = currTuple.getIsSatisfies();
		
		// Save the tuple boolean after right evaluation
		arg0.getRightExpression().accept(this);
		Boolean right = currTuple.getIsSatisfies();
		
		// Compare left and right evaluations
		if (left && right) {
			currTuple.setIsSatisfies(true);
		} else {
			currTuple.setIsSatisfies(false);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void visit(NullValue arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Function arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(InverseExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(JdbcParameter arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DoubleValue arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DateValue arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(TimeValue arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(TimestampValue arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Parenthesis arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(StringValue arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Addition arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Division arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Multiplication arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Subtraction arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OrExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Between arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(InExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IsNullExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(LikeExpression arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(SubSelect arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(CaseExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(WhenClause arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ExistsExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AllComparisonExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AnyComparisonExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Concat arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Matches arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseAnd arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseOr arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseXor arg0) {
		// TODO Auto-generated method stub
		
	}

}

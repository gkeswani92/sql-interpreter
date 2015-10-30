package operators;

import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import utils.BinaryFileReader;
import utils.BinaryFileWriter;
import utils.Tuple;
import utils.TupleComparator;

public class ExternalSortOperator extends SortOperator {

	private Integer numBufferPages, randomInt, numAttributes;
	// TODO: ------ read from catalog
	private String tempdir = "D:/Database_Practicals/SQL-Interpreter/samples/input/temp";
	private String externalSortDir = tempdir + "/externalsort";
	private String sortedFile;
	private String tableName;
	private BinaryFileReader bfr;

	// External calls
	public ExternalSortOperator(List<String> sortConditions, Operator child, Integer numBufferPages) {
		super(sortConditions, child);
		this.numBufferPages = numBufferPages;
		Random randomGenerator = new Random();
		randomInt = randomGenerator.nextInt(100);
		externalSortDir = externalSortDir + "/" + randomInt;
		
		Tuple tuple = child.getNextTuple();
		if (tuple != null) {
			this.tableName = tuple.getTableName();
			this.numAttributes = tuple.getNumAttributes();
		}
		
		child.reset();
	}	

	@Override
	public Tuple getNextTuple() {
		if (sortedFile == null) {
			try {
				sortedFile = externalSort();
				bfr = new BinaryFileReader(sortedFile, tableName);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		Tuple tableLessTuple = null;
		tableLessTuple = bfr.getNextTuple();
		
		if (tableLessTuple != null) {
			tableLessTuple.setTableName(this.tableName);
			tableLessTuple.updateTuple(tableName);
		}
	
		return tableLessTuple;
	}

	@Override
	public void reset(int index) {
		
		if (sortedFile == null && index == 0) {
			return;
		}
		
		try {
			bfr = new BinaryFileReader(sortedFile, tableName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Integer sizeOfTuple = numAttributes * 4;
		Integer availablePageSize = 4096 - 8;
		Integer tuplesPerPage = Math.floorDiv(availablePageSize, sizeOfTuple);
		Integer pageIndex = Math.floorDiv(index+1, tuplesPerPage);
		bfr.setChannelToPage(pageIndex);
	}
	
	//External sort implementation
	private String externalSort() throws FileNotFoundException {
		List<String> runFilenames= executePass0();
		int runCount = runFilenames.size();
		int pass = 0;
		int mergeBufferPages = numBufferPages-1;
		while(runCount > 1){
			pass++;
			List<String> newRunFilenames = new LinkedList<String>();			
			int newRuncount = 0;
			for(int i=0;i<=(int)Math.ceil(runFilenames.size()/mergeBufferPages);i++){
				String outputFileName = externalSortDir+"/pass" +pass+"/" + i+1;
				newRunFilenames.add(outputFileName);
				if((i*mergeBufferPages+mergeBufferPages) <  runFilenames.size()){						
					mergeAndWrite(runFilenames.subList(i*mergeBufferPages, i*mergeBufferPages+mergeBufferPages),outputFileName);						
				}
				else {
					mergeAndWrite(runFilenames.subList(i*mergeBufferPages, runFilenames.size()),outputFileName);
				}	
				newRuncount++;
			}
			runCount = newRuncount;
			while (!runFilenames.isEmpty()) {
				((LinkedList<String>) runFilenames).removeFirst();
		    }
			runFilenames.addAll(newRunFilenames);			
		}
		
		return runFilenames.get(0);
	}

	private void mergeAndWrite(List<String> subList, String outputFileName) throws FileNotFoundException {
		TupleComparator comp;
		List<Entry<List<Tuple>,Integer>> tupleBuffers = new ArrayList<>();
		List<BinaryFileReader> readers = new ArrayList<BinaryFileReader>();
		// creating buffer readers for each of the input passes needed to merge
		for(int i=0;i<subList.size();i++){
			BinaryFileReader fileReader = new BinaryFileReader(subList.get(i),tableName);
			readers.add(fileReader);
			List<Tuple> tupleList = new ArrayList<Tuple>();
			tupleList = getBlockTuples(fileReader);	
			Entry<List<Tuple>, Integer> myKey = new AbstractMap.SimpleEntry<List<Tuple>, Integer>(tupleList, 0);
			tupleBuffers.add(myKey);			
		}
		BinaryFileWriter bfw = new BinaryFileWriter(outputFileName);
		
		//Tracking the buffers to read in 
		List<Integer> includedBuffers = new LinkedList<Integer>();
		for(int i =0;i<tupleBuffers.size();i++)
			includedBuffers.add(i);
		boolean flag = true;
		int emptylist = 0;	
		while(flag){
			int min = includedBuffers.get(0);					
			for(int i=0;i<tupleBuffers.size();i++){
				if(includedBuffers.contains(i)){
					if(tupleBuffers.get(i).getValue()<tupleBuffers.get(i).getKey().size()){
						Tuple tuple1 = tupleBuffers.get(i).getKey().get(tupleBuffers.get(i).getValue());
						Tuple minTuple = tupleBuffers.get(min).getKey().get(tupleBuffers.get(min).getValue());
						comp = new TupleComparator(new ArrayList<String>(tuple1.getArributeList()));
						if(comp.compare(tuple1, minTuple) < 0)
							min = i;
					}else{
						List<Tuple> newBlock = getBlockTuples(readers.get(i));
						if(newBlock==null){
							emptylist++;
							includedBuffers.remove(includedBuffers.indexOf(i));
							if(includedBuffers.size()==1){
								int start= tupleBuffers.get(includedBuffers.get(0)).getValue();
								for(int j = start;j<tupleBuffers.get(includedBuffers.get(0)).getKey().size();j++){
									System.out.println(tupleBuffers.get(includedBuffers.get(0)).getKey().get(j));
								}
								emptylist++;
								if(emptylist==tupleBuffers.size()){									
									flag=false;
									break;
								}
							}
							
						}else{
							Entry<List<Tuple>, Integer> myKey = new AbstractMap.SimpleEntry<List<Tuple>, Integer>(newBlock, 0);
							tupleBuffers.add(i, myKey);
						}
					}
				}									
			}
			bfw.writeNextTuple(tupleBuffers.get(min).getKey().get(tupleBuffers.get(min).getValue()));			
			tupleBuffers.get(min).setValue(tupleBuffers.get(min).getValue()+1);
		}		
	}



	private List<Tuple> getBlockTuples(BinaryFileReader fileReader) {
		int attributeCount = 0;	
		int numberOfTuples = 0;
		int tuplesRead = 0;
		Tuple tuple = fileReader.getNextTuple();
		this.tableName = tuple.getTableName();
		tuplesRead =1;
		List<Tuple> pageTuples = new ArrayList<Tuple>();
		if(tuple!=null){
			attributeCount = tuple.getArributeList().size();
			numberOfTuples = (int)Math.floor(4088/(numberOfTuples*4));			
			while(numberOfTuples > tuplesRead+1 && tuple!=null){
				pageTuples.add(tuple);
				tuplesRead++;
				tuple=fileReader.getNextTuple();
			}
			if(tuple!=null){
				pageTuples.add(tuple);
				tuplesRead++;
			}			
			return pageTuples;		
		} else {
			return null;
		}		
	}

	private List<String> executePass0() throws FileNotFoundException {		
		//Execute sort on the tuplesList of numBufferPages
		Integer runCount = 0;
		List<String> runFilenames = new LinkedList<String>();
		while(true){			
			List<Tuple> blockTuples = getBlockTuples();
			if(!blockTuples.isEmpty()){
				runCount++;
				// If query has no sort condition (in case of distinct), sort using all attributes
				// For further explanation refer DuplicateElimationOperator.java
				if (sortConditions.isEmpty()) {
					sortConditions = new ArrayList<String>(blockTuples.get(0).getArributeList());
				} else {
					// Adds all remaining attributes that aren't already sort conditions to the sort conditions
					// Preserves order of attributes in the tuple
					List<String> attributes = new ArrayList<String>(blockTuples.get(0).getArributeList());					
					for (String sort: sortConditions) {
						if (attributes.contains(sort)) {
							attributes.remove(sort);
						}
					}
					sortConditions.addAll(attributes);
				}
				// Sort using tuple comparator
				blockTuples.sort(new TupleComparator(sortConditions));
				// TO DO ------ read from catalog
				//String filename = tempdir+"/externalsort/"+randomInt+"/pass0/"+runCount;
				String filename = externalSortDir + "/pass0/"+runCount;
				runFilenames.add(filename);
				BinaryFileWriter bfw = new BinaryFileWriter(filename);
				Iterator<Tuple> myiterator = blockTuples.iterator();
				while(myiterator.hasNext()){
					Tuple tuple = myiterator.next();
					bfw.writeNextTuple(tuple);					
				}
				bfw.writeNextTuple(null);				
			}else{
				return runFilenames;
			}
			
		}	
		
	}

	private List<Tuple> getBlockTuples() {
		
		int attributeCount=0;
		int maxTuples=0;
		List<Tuple> blockTuples = new ArrayList<Tuple>();
		if (tuples.isEmpty()) {
			int tuplesRead=0;
			Tuple currTuple = child.getNextTuple();
			
			if(currTuple!=null){
				attributeCount = currTuple.getArributeList().size();
				maxTuples = (int) Math.floor((numBufferPages*4088)/(attributeCount*4));
				while(maxTuples > tuplesRead+1 && currTuple!=null){
					blockTuples.add(currTuple);
					tuplesRead++;
					currTuple=child.getNextTuple();
				}
				if(currTuple!=null){
					blockTuples.add(currTuple);
					tuplesRead++;
				}				
			}		 
		} 
		else {
			//get a subset from the tuples List 
			 attributeCount = tuples.get(0).getArributeList().size();
			 maxTuples = (int) Math.floor((numBufferPages*4096)/(attributeCount*4));
			 if(currIndex+maxTuples < tuples.size()){
				 blockTuples = tuples.subList(currIndex, currIndex+maxTuples);
				 currIndex+=maxTuples;
			 }else{
				 blockTuples = tuples.subList(currIndex, tuples.size());
				 currIndex=tuples.size();
			 }
		}		 
		return  blockTuples;
	}
}

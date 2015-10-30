package utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;


public class BinaryFileWriter implements TupleWriter {	
	
	private String fileName;
	ByteBuffer buffer;
	private FileInputStream fos;
	private FileChannel channel;
	private Integer noOfTuplesWritten;
	private Integer tupleSize;
	boolean append = true;
	Integer bufferPos;
	
	public BinaryFileWriter(String fileName) throws FileNotFoundException {		
		this.fileName = fileName;
		File outputFile = new File(fileName);
		if(!outputFile.exists()) {
			try {
				outputFile.getParentFile().mkdirs();
				outputFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Unable to create the output file");
			}
		}
		FileOutputStream fos = new FileOutputStream(fileName);
	    // allocate a channel to write that file
	    channel = fos.getChannel();
	    updateBufferWithNewByteBuffer();		
	}	

	private void updateBufferWithNewByteBuffer() {
		
		// TODO Auto-generated method stub
		noOfTuplesWritten = 0;
		tupleSize = 0;
		buffer = ByteBuffer.allocate( 1024 * 4 );	
		//buffer.clear();
		buffer.putInt(tupleSize);
		buffer.putInt(noOfTuplesWritten);	
	}

	@Override
	public int writeNextTuple(Tuple tuple) {		
		Integer element;
		// TODO Auto-generated method stub		
		// check if the buffer can take the entire tuple
		if(tuple != null && ! (buffer.remaining()>(tuple.getArributeList().size()*4))){			
			// get a new bytebuffer and set the value for prev page buffer with tuple details
			writeByteBufferToFile();
			updateBufferWithNewByteBuffer();			
		} else if(tuple == null){
			writeByteBufferToFile();
			try {
				channel.close();				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Unable to close streams after writing");
				e.printStackTrace();
			}			
			return 0;			
		}
		//add all the tuple values into the buffer
		Iterator iterator = tuple.getAttributeValues().values().iterator();
		while(iterator.hasNext()){
			element = (Integer) iterator.next();
			buffer.putInt(element);				
		}
		noOfTuplesWritten++;
		return tupleSize = tuple.getArributeList().size();		
	} 
	
	public void writeByteBufferToFile(){
		buffer.position(0);
		buffer.putInt(tupleSize);
		buffer.putInt(noOfTuplesWritten);
		buffer.position(0);
		try {
			channel.write(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Couldn't write the buffer to the output file"+ fileName);
			e.printStackTrace();
		}
	}
	


	
}
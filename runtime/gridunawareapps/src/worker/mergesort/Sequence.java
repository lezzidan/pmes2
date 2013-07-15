
package worker.mergesort;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;


public class Sequence {
	
	private int length;
	private int[] sequence;

	
	public Sequence(int maxLength) {
		length = 0;
		sequence = new int[maxLength];
	}

	public Sequence(int length, int[] sequence) {
		this.length = length;
		this.sequence = sequence;
	}
	
	public Sequence(String fileName) {
		try {
			FileReader fr = new FileReader(fileName );
			BufferedReader br = new BufferedReader(fr);

			// Read sequence length
			length = Integer.parseInt(br.readLine());
			
			// Read sequence
			sequence = new int[length];
			for (int i = 0; i < length; i++)
				sequence[i] = Integer.parseInt(br.readLine());

			br.close();
			fr.close();
		}
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public int getLength() {
		return length;
	}
	
	public int[] getSequence() {
		return sequence;
	}
	
	public void addElement(int elem) {
		if (length < sequence.length)
			sequence[length++] = elem;
	}
	
	public Integer getElement(int pos) {
		if (pos < length)
			return sequence[pos];
		else
			return null;
	}
	
	public void sequenceToDisk(String fileName) throws MergeSortAppException {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			String s = length + "\n";
			fos.write(s.getBytes());
			
			for (int i = 0; i < length; i++) {
				s = sequence[i] + "\n";
				fos.write(s.getBytes());
			}
			
			fos.close();
		}
		catch (FileNotFoundException fnfe) {
			throw new MergeSortAppException( fnfe.getMessage() );
		}
		catch (IOException ioe) {
			throw new MergeSortAppException( ioe.getMessage() );
		}
	}
	
	public String toString() {
		return "Sequence:\n" + 
			   "\t- length: " + length + "\n" +
			   "\t- elements: " + Arrays.toString(sequence);
	}
	
}

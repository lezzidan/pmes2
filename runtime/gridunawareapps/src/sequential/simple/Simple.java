
package sequential.simple;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import worker.simple.SimpleImpl;


public class Simple {
		
	public static void main(String[] args) {
		String counterName = "counter.txt";
		int initialValue = 1;		

		// Initialize counter (c -> 1)
		try {
			FileOutputStream fos = new FileOutputStream(counterName);
			fos.write(initialValue);
			System.out.println("Initial counter value is " + initialValue);
			fos.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}

		
		// Execute increment (c -> 2)
		SimpleImpl.increment(counterName);


		// Open the file and print final counter value (should be 2)
		try {
        	FileInputStream fis = new FileInputStream(counterName);
            System.out.println("Final counter value is " + fis.read());
			fis.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

}

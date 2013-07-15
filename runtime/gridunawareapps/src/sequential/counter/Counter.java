
package sequential.counter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import worker.simple.SimpleImpl;


public class Counter {

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

		// c -> 2
		SimpleImpl.increment(counterName);
		
		try {
			FileInputStream fis = new FileInputStream(counterName);
			int count = fis.read();
			System.out.println("Current counter value is " + count + " (2)");
			fis.close();
		
			// c -> 3
			FileOutputStream fos = new FileOutputStream(counterName);
            fos.write(++count);			
			fos.close();
			
			fis = new FileInputStream(counterName);
			System.out.println("Current counter value is " + fis.read() + " (3)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		// c -> 4
		SimpleImpl.increment(counterName);
		
		try {
			// c -> 9
			FileOutputStream fos = new FileOutputStream(counterName);
            fos.write(9);			
			fos.close();
			
			FileInputStream fis = new FileInputStream(counterName);
			System.out.println("Current counter value is " + fis.read() + " (9)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		// c -> 10
		SimpleImpl.increment(counterName);
		
		// Open the file and print final counter value (should be 10)
		try {
        	FileInputStream fis = new FileInputStream(counterName);
            System.out.println("Final counter value is " + fis.read() + " (10)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
}

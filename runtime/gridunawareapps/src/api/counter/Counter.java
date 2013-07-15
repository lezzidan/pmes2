
package api.counter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import worker.simple.SimpleImpl;

import integratedtoolkit.api.IntegratedToolkit;
import integratedtoolkit.api.IntegratedToolkit.OpenMode;
import integratedtoolkit.api.impl.IntegratedToolkitImpl;


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
		
		// First local increment
		SimpleImpl.increment(counterName);
		
		// Try with URI syntax
		//counterName = "file://bscgrid01.bsc.es/home/etejedor/IT/api.counter.Counter/counter.txt";
		
		// Start IT
		IntegratedToolkit it = new IntegratedToolkitImpl();
		it.startIT();
		
		// Execute first remote increment (c -> 2)
		SimpleImpl.increment(counterName);
		
		// Open the file and perform a local increment (c -> 3)
		String currentName = it.openFile(counterName, OpenMode.APPEND);
		try {
			FileInputStream fis = new FileInputStream(currentName);
			int count = fis.read();
			System.out.println("Current counter value is " + count + " (3)");
			fis.close();
			
			FileOutputStream fos = new FileOutputStream(currentName);
            fos.write(++count);			
			fos.close();
			
			fis = new FileInputStream(currentName);
			System.out.println("Current counter value is " + fis.read() + " (4)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		// Execute second remote increment (c -> 4)
		SimpleImpl.increment(counterName);
		
		// Open the file and change the value of the counter locally (c -> 9)
		currentName = it.openFile(counterName, OpenMode.WRITE);
		try {
			FileOutputStream fos = new FileOutputStream(currentName);
            fos.write(9);			
			fos.close();
			
			FileInputStream fis = new FileInputStream(currentName);
			System.out.println("Current counter value is " + fis.read() + " (9)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		// Execute third remote increment (c -> 10)
		SimpleImpl.increment(counterName);
		
		// Stop IT temporarily
        it.stopIT(false);

        counterName = "counter.txt";
        
        // Execute second local increment (c -> 11)
		SimpleImpl.increment(counterName);
        
		// Execute third local increment (c -> 12)
		SimpleImpl.increment(counterName);
		
		// Open the file and print counter value (should be 12)
		try {
        	FileInputStream fis = new FileInputStream(counterName);
            System.out.println("Counter value after local increments is " + fis.read() + " (12)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		// Restart IT
		it.startIT();
		
		// Execute fourth remote increment (c -> 13)
		SimpleImpl.increment(counterName);
		
		// Execute fifth remote increment (c -> 14)
		SimpleImpl.increment(counterName);
		
		// Stop IT definitely
        it.stopIT(true);
        
        // Open the file and print final counter value (should be 14)
		try {
        	FileInputStream fis = new FileInputStream(counterName);
            System.out.println("Final counter value is " + fis.read() + " (14)");
			fis.close();
        }
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
}


package sequential.mergesort;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;


public class SequenceGenerator {

	public static void main(String[] args) {
		if ( args.length != 3 ) {
			System.out.println("Usage: java SequenceGenerator file_name sequence_length seed\n");
			return;
		}
		
		new SequenceGenerator().generate(args[0],
										 Integer.parseInt(args[1]),
										 Long.parseLong(args[2]));
	}
	
	public SequenceGenerator() { }
	
	public void generate(String fileName, int length, long seed) {
		Random r = new Random(seed);
		
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			String s = length + "\n";
			fos.write(s.getBytes());
			
			for (int i = 0; i < length; i++) {
				s = r.nextInt(length) + "\n";
				fos.write(s.getBytes());
			}
			
			fos.close();
		}
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			System.exit(1);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
}

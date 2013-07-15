
package sequential.streams;

import java.io.*;

import worker.simple.SimpleImpl;


public class Streams {

	public static void main(String[] args) {
		try {
			String seq1 = "sequence1.txt";
			String seq2 = "sequence2.txt";
			int initialValue = 1;
			
			// Initialize sequences (-> 1)
			FileOutputStream inifos1 = new FileOutputStream(seq1, false);
			FileOutputStream inifos2 = new FileOutputStream(seq2, false);
			inifos1.write(initialValue);
			inifos1.flush();
			inifos2.write(initialValue);
			FileInputStream inifis1 = new FileInputStream(seq1);
			System.out.println("(1) Initial sequences value is " + inifis1.read()); // 1
			inifos1.close();
			inifos2.close();
			
			
			// Execute remote increment (-> 2)
			System.out.println("\n################ TASK CREATION ################");
			SimpleImpl.increment(seq1);
			
			
			// Try FileInputStream and FileOutputStream
			System.out.println("\n################ FileInputStream and FileOutputStream ################");
			FileInputStream fis1 = new FileInputStream(seq1);
	        System.out.println("(2) Sequence1 1st value is " + fis1.read()); // 2
	           
	        FileOutputStream fos1 = new FileOutputStream(new File(seq1), true);
	        fos1.write(7); // seq1 = 2 7
	        fos1.flush();
	        System.out.println("(7) Sequence1 2nd value is " + fis1.read()); // 7
	         
	        FileOutputStream fos2 = new FileOutputStream(new File(seq2));
	        fos2.write(0); // seq2 = 0
	        fos2.write(1); // seq2 = 0 1
	        fos2.flush();
	        
	        FileInputStream fis2 = new FileInputStream(new File(seq2));
	        System.out.println("(0) Sequence2 1st value is " + fis2.read()); // 0
	        FileInputStream fis2b = new FileInputStream(fis2.getFD());
	        System.out.println("(1) Sequence2 2nd value is " + fis2b.read()); // 1
	        
	        FileOutputStream fos1a = new FileOutputStream(new File(seq1));
	        FileOutputStream fos1b = new FileOutputStream(fos1.getFD());
	        fos1b.write(0); // seq1 = 0
	        
	        fos1.close();
	        fos2.close();
			fis2.close();
			fis2b.close();
			fis1.close();
			fos1a.close();
			fos1b.close();
			
			
			// Execute remote increment (-> 1)
			System.out.println("\n################ TASK CREATION ################");
			SimpleImpl.increment(seq1);
			
			
			// Try FileReader, InputStreamReader, BufferedReader and FileWriter, OutputStreamWriter, BufferedWriter
			System.out.println("\n################ InputStreamReader, BufferedReader and OutputStreamWriter, BufferedWriter ################");
			InputStreamReader isr1 = new InputStreamReader(new FileInputStream(seq1));
	        System.out.println("(1) Sequence1 1st value is " + isr1.read()); // 1
	            
	        OutputStreamWriter osw1 = new OutputStreamWriter(new FileOutputStream(new File(seq1), true));
	        osw1.write(7); // seq1 = 1 7
	        osw1.flush();
	        System.out.println("(7) Sequence1 2nd value is " + isr1.read()); // 7
	           
	        BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(seq2), false)));
	        bw2.write(0); // seq2 = 0
	        bw2.flush();
	            
	        BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream(seq2)));
	        System.out.println("(0) Sequence2 1st value is " + br2.read()); // 0
	           
	        BufferedReader br1 = new BufferedReader(new FileReader(seq1));
	        System.out.println("(1) Sequence1 1st value is " + br1.read()); // 1
	            
	        BufferedWriter bw1 = new BufferedWriter(new FileWriter(seq1));
	        bw1.write(0); // seq1 = 0
	            
	        br1.close();
	        bw1.close();
	        bw2.close();
	        isr1.close();
			br2.close();
			osw1.close();
	        
			
			// Execute remote increment (-> 1)
			System.out.println("\n################ TASK CREATION ################");
			SimpleImpl.increment(seq1);
			SimpleImpl.increment(seq2);
			
			
			// Try BufferedInputStream, DataInputStream and BufferedOutputStream, DataOutputStream
			System.out.println("\n################ BufferedInputStream, DataInputStream and BufferedOutputStream, DataOutputStream ################");
			DataOutputStream dos1 = new DataOutputStream(new FileOutputStream(seq1));
			BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(seq1));
			dos1.write(2); // seq1 = 2
			System.out.println("(2) Sequence1 1st value is " + bis1.read()); // 2
			
			BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(seq2, true));
			bos2.write(2); // seq2 = 1 2
			bos2.flush();
			DataInputStream dis2 = new DataInputStream(new FileInputStream(seq2));
			System.out.println("(1) Sequence2 1st value is " + dis2.read()); // 1
			System.out.println("(2) Sequence2 2nd value is " + dis2.read()); // 2
			
			bos2.close();
			dis2.close();
			dos1.close();
			bis1.close();
	        
			
			// Execute remote increment (-> 3)
			System.out.println("\n################ TASK CREATION ################");
			SimpleImpl.increment(seq1);
			
			
			// Try PrintStream, PrintWriter
			System.out.println("\n################ PrintStream, PrintWriter ################");
			PrintStream ps1a = new PrintStream(new FileOutputStream(seq1, true));
			ps1a.write(7);
			ps1a.flush();
			fis1 = new FileInputStream(seq1);
			System.out.println("(3) Sequence1 1st value is " + fis1.read()); // 3
			System.out.println("(7) Sequence1 2nd value is " + fis1.read()); // 7
			fis1.close();
			ps1a.close();
			
			PrintWriter pw1a = new PrintWriter(new FileOutputStream(seq1));
			pw1a.print('9');
			pw1a.flush();
			DataInputStream dis1 = new DataInputStream(new FileInputStream(seq1));
			System.out.println("(9) Sequence1 1st value is " + (char)((byte)dis1.read())); // 9
			dis1.close();
			pw1a.close();
			
			
			// Try RandomAccessFile
			System.out.println("\n################ RandomAccessFile ################");
			RandomAccessFile raf2a = new RandomAccessFile(new File(seq2), "rw");
			long length = raf2a.length();
			FileReader fr2 = new FileReader(raf2a.getFD());
			System.out.println("(1) Sequence2 1st value is " + fr2.read()); // 1
			System.out.println("(2) Length of sequence2 is " + length);
			raf2a.seek(length);
			raf2a.writeUTF("6");
			raf2a.close();
			
			RandomAccessFile raf2b = new RandomAccessFile(new File(seq2), "r");
			raf2b.seek(length);
			System.out.println("(6) Last number of sequence2 is " + raf2b.readUTF());
			
			fr2.close();
			raf2b.close();
			
			System.out.println("\n################ END OF STREAM TEST ################\n");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
}

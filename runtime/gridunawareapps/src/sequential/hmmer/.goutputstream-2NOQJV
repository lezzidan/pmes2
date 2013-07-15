package sequential.hmmer;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import worker.hmmer.HMMPfamImpl;


public class HMMPfamIter {

	private static final String SEQUENCE  = "seqF";
	private static final String DATABASE  = "dbF";
	
	private static final String TOTAL_NHMM = "--total_nhmm";
	private static final String START_NHMM = " --start_nhmm ";
	
	private static final String FRAGS_DIR = "hmmer_frags";
	
	// hmmpfam constants
	private static final int ALPHABET_SIZE 	 = 20;
	private static final int TRANS_PROB_SIZE = 7;
	private static final int PLAN7_DESC 	 = 1 << 1;
	private static final int PLAN7_RF 		 = 1 << 2;
	private static final int PLAN7_CS 		 = 1 << 3;
	private static final int PLAN7_STATS 	 = 1 << 7;
	private static final int PLAN7_MAP 		 = 1 << 8;
	private static final int PLAN7_ACC 		 = 1 << 9;
	private static final int PLAN7_GA 		 = 1 << 10;
	private static final int PLAN7_TC 		 = 1 << 11;
	private static final int PLAN7_NC 		 = 1 << 12;
	
	// Let's assume these are the right sizes for file reading (in bytes)
	private static final int CHAR_SIZE  = 1;
	private static final int INT_SIZE   = 4;
	private static final int FLOAT_SIZE = 4;
	private static final byte[] intBytes = new byte[INT_SIZE];
	
	private static final byte iniSeq       = 0x3e;
	private static final int  v20Magic     = 0xe8ededb5;
	
	private static int numProcs;
	private static long memPerProc; 	// B
	private static long minFragSizeSeq; // B
	private static long minFragSizeDB;  // B
	private static String fragsDirName;
	
	private static CommandLineArgs clArgs;
	private static int[] dbFragsNumModels = null;
    private static int totalNumModels = 0;
	
	private static boolean debug;
	
	
	public static void main(String args[]) throws Exception {
		
		/* Parameters:
		 * - 0: debug
		 * - 1: database file name
		 * - 2: query sequences file name
		 * - 3: output file name
		 *  
		 * - 4: number of processors
		 * - 5: memory per processor for database plus sequences (MB)
		 * - 6: minimum fragment size for database (KB)
		 * - 7: minimum fragment size for sequences (KB)
		 * - 8: directory to store the fragments temporarily
		 * 
		 * - 9: absolute path of the hmmpfam binary
		 * - 10: command line arguments for hmmpfam
		 */
		
		debug = Boolean.parseBoolean(args[0]);
		
		String dbName		= args[1],
			   seqsName		= args[2],
			   outputName	= args[3];
		
		numProcs = Integer.parseInt(args[4]);
		
		/* memPerProc >= minFragSizeSeq + minFragSizeDB 
		 * Total memory available per processor - memory that hmmpfam consumes
		 * Include an estimation of the memory consumed by the output? JVM?
		 */
		//memPerProc = Long.parseLong(args[5]) * (long)Math.pow(2, 20); // MB -> B  TODO!!!!!!!!!!!!!!!!!!
		memPerProc = Long.parseLong(args[5]) * (long)Math.pow(2, 10); // KB -> B
		
		/* Important when there are more processors available than necessary.
		 * Tell which is the minimum size for the fragments of sequences and database
		 * that keeps parallelism but not too much to be worth.
		 */
		minFragSizeDB  = Long.parseLong(args[6]) * (long)Math.pow(2, 10); // KB -> B
		//minFragSizeSeq = Long.parseLong(args[7]) * (long)Math.pow(2, 10); // KB -> B
		minFragSizeSeq = Long.parseLong(args[7]);				    // B: TODO!!!!!!!!!!!!!!!!!	

		/* Generated fragments will be located in the specified directory
		 * For MN, they should be located preferably in /gpfs/scratch
		 */
		File fragsDir = new File(args[8] + File.separator + FRAGS_DIR);
		if (!fragsDir.exists() && !fragsDir.mkdir())
			throw new Exception("Cannot create the fragments directory");
		fragsDir.deleteOnExit();
		fragsDirName = fragsDir.getCanonicalPath();
		
		// Get binary and parse command line arguments
		String hmmpfamBin = args[9];
		clArgs = new CommandLineArgs(args, 10);
		
		File fSeq = new File(seqsName);
		File fDB = new File(dbName);
		
		LinkedList<String> seqFrags = new LinkedList<String>();
		LinkedList<String> dbFrags  = new LinkedList<String>();
		
		if (debug) {
			System.out.println("\nParameters: ");
			System.out.println("- Database file: " + dbName);
			System.out.println("- Query sequences file: " + seqsName);
			System.out.println("- Output file: " + outputName);
			System.out.println("- Number of procs: " + numProcs);
			System.out.println("- Memory per processor (B): " + memPerProc);
			System.out.println("- Minimum frag size for db (B): " + minFragSizeDB);
			System.out.println("- Minimum frag size for seq (B): " + minFragSizeSeq);
			System.out.println("- Frags dir: " + fragsDirName);
			System.out.println("- hmmpfam binary: " + hmmpfamBin);
			System.out.println("- Command line args: " + clArgs.getArgs());
		}
		
		/* FIRST PHASE
		 * Segment the query sequences file, the database file or both
		 */
		
		split(fSeq, fDB, seqFrags, dbFrags);
		
		
		/* SECOND PHASE
		 * Launch hmmpfam for each pair of seq - db fragments
		 */
		int numSeqFrags = seqFrags.size();
		int numDBFrags = dbFrags.size();
		
		/* Pass the total number of models and the number of models per fragment
		 * if the database is segmented and Z is not defined
		 */
		boolean numModelsNeeded = numDBFrags > 1 && !clArgs.isZDefined();
		
		//if (numModelsNeeded) clArgs.addArg(TOTAL_NHMM, Integer.toString(totalNumModels));
		String commonArgs = clArgs.getArgs();
		ArrayList<String> outputs = new ArrayList<String>(numSeqFrags * numDBFrags);
		
		int i = 0, startHMM = 0, dbNum = 0;
		for (String dbFrag : dbFrags) {
			//String finalArgs = numModelsNeeded ? commonArgs + START_NHMM + startHMM : commonArgs;
			String finalArgs = commonArgs;
			if (dbFragsNumModels != null) startHMM += dbFragsNumModels[i++];
			int seqNum = 0;
			for (String seqFrag : seqFrags) {
				String output = fragsDirName + File.separator + SEQUENCE + seqNum + "_" + DATABASE + dbNum + ".out";
				outputs.add(output);
				new File(output).deleteOnExit();
				
				HMMPfamImpl.hmmpfam(hmmpfamBin,
									finalArgs,
									seqFrag,
									dbFrag,
									output);
				
				seqNum++;
			}
			dbNum++;
		}
		// TODO: PROVAR AMB EL OUTER LOOP SOBRE LES SEQUENCIES
		
		
		/* THIRD PHASE
		 * Merge all output in a single file
		 */
		ArrayList<String> outputsAux = new ArrayList<String>(numSeqFrags * numDBFrags); 
		ArrayList<String> outputsTemp;
		
		// Iterate until there's only one output file with all results merged
		while (outputs.size() > 1) {
			ListIterator<String> li = outputs.listIterator();
			while (li.hasNext()) {
				String firstOutput = li.next();
				String secondOutput = li.hasNext() ? li.next() : null;
				if (secondOutput == null) {
					outputsAux.add(firstOutput);
					break;
				}
				
				if (sameSeqFragment(firstOutput, secondOutput)) {
					// Merge output fragments of different db fragments (must take care when merging)
					HMMPfamImpl.mergeSameSeq(firstOutput, secondOutput, clArgs.getALimit());
					outputsAux.add(firstOutput);
				}
				else if (sameDBFragment(firstOutput, secondOutput)) {
					// Merge output fragments of different sequence fragments (basically appending one to another)
					HMMPfamImpl.mergeSameDB(firstOutput, secondOutput);
					outputsAux.add(firstOutput);
				}
				else {
					// Avoid merging two output fragments of different sequence and db fragments
					outputsAux.add(firstOutput);
					li.previous();
				}
			}
			
			outputsTemp = outputs;
			outputs = outputsAux;
			outputsAux = outputsTemp;
			outputsAux.clear();
		}
		
		try {
			prepareResultFile(outputs.get(0), outputName, seqsName, dbName);
		}
		catch (IOException e) {
			System.out.println("Error copying final result file");
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	
	// Split the query sequences file, the database file or both
	private static void split(File fSeq,
							  File fDB,
							  LinkedList<String> seqFrags,
							  LinkedList<String> dbFrags) {
		
		long fSeqSize = fSeq.length(), // B
			 fDBSize  = fDB.length();  // B
		
		long seqFragSize, dbFragSize;
		
		// Update the minimums if the respective file sizes are actually smaller
		if (minFragSizeSeq > fSeqSize) minFragSizeSeq = fSeqSize;
		if (minFragSizeDB  > fDBSize)  minFragSizeDB  = fDBSize;
		
		/* We will try to avoid segmenting the DB as less as possible, generating
		 * the smallest number of fragments to make them fit in one processor's memory,
		 * and respecting the minimum fragment size for the DB.
		 */
		long aux = fDBSize;
		int i = 2;
		while (aux + minFragSizeSeq > memPerProc && aux > minFragSizeDB)
			aux = fDBSize / i++;
		
		// We know that minFragSizeDB + minFragSizeSeq <= memPerProc
		dbFragSize = (aux == fDBSize || aux > minFragSizeDB) ? aux : minFragSizeDB;
		long availForSeq = memPerProc - dbFragSize;
		
		if (fSeqSize + fDBSize > memPerProc * numProcs) {
			/* We don't have enough processors to cover all the calculation in only one step.
			 * We have to segment the query sequences file according to the remaining memory
			 * per processor i.e. the memory not occupied by the (fragment of) DB.
			 */
			
			// We know that aux >= minFragSizeSeq
			aux = fSeqSize;
			i = 2;
			while (aux > availForSeq && aux > minFragSizeSeq)
				aux = fSeqSize / i++;
			
			seqFragSize = (aux == fSeqSize || aux > minFragSizeSeq) ? aux : minFragSizeSeq;
		}
		else {
			/* We have enough processors to cover all the calculation in only one step.
			 * We have to segment the query sequences file according to the number of processors.
			 * Still, there's no point in segmenting too much, even if we have plenty of
			 * processors to use. Let's segment according to the minimum memory to use
			 * per processor if necessary.
			 */
			
			aux = fSeqSize / (3 * numProcs);
			if (aux > availForSeq) aux = availForSeq;
			seqFragSize = (aux == fSeqSize || aux > minFragSizeSeq) ? aux: minFragSizeSeq;
		}
		
		if (debug) {
			System.out.println("\nDecided sizes for fragments:");
			System.out.println("- Sequences file size (B): " + fSeqSize);
			System.out.println("- Sequences fragment size (B): " + seqFragSize);
			System.out.println("- DB file size (B): " + fDBSize);
			System.out.println("- DB fragment size (B): " + dbFragSize);
		}
		
		// Now generate the fragments (if necessary)
		try {
			if (fSeqSize == seqFragSize) seqFrags.add(fSeq.getCanonicalPath());
			else						 generateSeqFragments(fSeq, seqFragSize, seqFrags);
			
			if (fDBSize == dbFragSize)	 dbFrags.add(fDB.getCanonicalPath());
			else						 generateDBFragments(fDB, dbFragSize, dbFrags);
		}
		catch (Exception e) {
			System.out.println("Error generating fragments");
			e.printStackTrace();
			System.exit(1);
		}
			
		// Mark the fragments to be deleted on JVM exit (if any)
		if (seqFrags.size() > 1)
			for (String seqFrag : seqFrags)
				new File(seqFrag).deleteOnExit();
		if (dbFrags.size() > 1)
			for (String dbFrag : dbFrags)
				new File(dbFrag).deleteOnExit();
	}
	
	
	/* Visit the query sequence file iteratively, skipping fragSize bytes and then
	 * searching for '>' to determine the end of the fragment at each iteration.
	 */
	
	private static void generateSeqFragments(File srcFile,
		  	 								 long fragSize,
		  	 								 LinkedList<String> frags) throws Exception {

		if (debug)
			System.out.println("\nGenerating sequence fragments");
		
		RandomAccessFile raf = new RandomAccessFile(srcFile, "r");
		FileChannel seqChannel = raf.getChannel();
		
		// Position of the first byte in the fragment
		long iniFragPosition = 0;
		int fragNum = 0;
		boolean eofReached = false;
		
		while (!eofReached) {
			seqChannel.position(iniFragPosition + fragSize);
			
			boolean fragRead = false;
			while (!fragRead) {
				try {
					fragRead = (raf.readByte() == iniSeq); 
				}
				catch (EOFException e) {
					eofReached = true;
					fragRead = true;
					continue;
				}
			}
		
			// Position right after the last byte of the fragment (position of token)
			long endFragPosition = eofReached ? seqChannel.size() : seqChannel.position() - 1;
			
			// Channel for the current fragment
			String fragPathName = fragsDirName + File.separator + SEQUENCE + fragNum;
			FileChannel fragChannel = new FileOutputStream(fragPathName).getChannel();
			frags.add(fragPathName);
			
			if (debug)
				System.out.println("\t- Fragment " + fragNum + ": transfer from " + srcFile.getCanonicalPath() + " to " + fragPathName
				  + ", " + (endFragPosition - iniFragPosition) + " bytes starting at byte " + iniFragPosition);
			
			seqChannel.transferTo(iniFragPosition, endFragPosition - iniFragPosition, fragChannel);
			iniFragPosition = endFragPosition;
			fragNum++;
		}
		
		raf.close();
	}
	
	
	// Visit the DB file, reading one model at each iteration.
	private static void generateDBFragments(File srcFile,
											long fragSize,
											LinkedList<String> frags) throws Exception {
		
		if (debug)
			System.out.println("\nGenerating database fragments");
		
		BufferedInputStream bisDB = new BufferedInputStream(new FileInputStream(srcFile));
		bisDB.mark(5);
		final boolean isSwap = (readInt(bisDB) == Integer.reverseBytes(v20Magic));
		bisDB.reset();
		
		FileChannel dbChannel = new FileInputStream(srcFile).getChannel();
		int numFrags = (int)(srcFile.length() / fragSize);
		int toAdd = (int)(srcFile.length() % fragSize);
		dbFragsNumModels = new int[numFrags];
		int fragNum = 0;
		int fragNumModels = 0;
		long fragBytes = 0;
		long iniFrag = 0;
		long modelBytes;
		
		while ((modelBytes = readHMM(bisDB, isSwap)) > 0) {
			fragNumModels++;
			fragBytes += modelBytes;
			if (fragBytes >= fragSize) {
				// Channel for the current fragment
				String fragPathName = fragsDirName + File.separator + DATABASE + fragNum;
				FileChannel fragChannel = new FileOutputStream(fragPathName).getChannel();
				frags.add(fragPathName);
				
				if (debug)
					System.out.println("\t- Fragment " + fragNum + ": transfer from " + srcFile.getCanonicalPath() + " to " + fragPathName
									   + ", " + fragBytes + " bytes starting at byte " + iniFrag);
					
				dbChannel.transferTo(iniFrag, fragBytes, fragChannel);
				
				iniFrag += fragBytes;
				dbFragsNumModels[fragNum] = fragNumModels;
                totalNumModels += fragNumModels;
				fragNumModels = 0;
				fragBytes = 0;
				fragNum++;
				
				// Make sure that last fragment has the remainder of bytes
				if (fragNum == numFrags - 1) fragSize += toAdd;
			}
		}
		// Treat last fragment
		if (fragBytes > 0) {
			String fragPathName = fragsDirName + File.separator + DATABASE + fragNum;
			FileChannel fragChannel = new FileOutputStream(fragPathName).getChannel();
			frags.add(fragPathName);
			
			System.out.println("\t- Fragment " + fragNum + ": transfer from " + srcFile.getCanonicalPath() + " to " + fragPathName
							   + ", " + fragBytes + " bytes starting at byte " + iniFrag);
			
			dbChannel.transferTo(iniFrag, fragBytes, fragChannel);
			
			dbFragsNumModels[fragNum] = fragNumModels;
            totalNumModels += fragNumModels;
		}
		else fragNum--;
		
		bisDB.close();
		dbChannel.close();
		
		if (debug) {
			System.out.println("Found a total of " + totalNumModels + " models in the database");
			System.out.println("Number of models per fragment:");
			for (int i = 0; i <= fragNum; i++)
				System.out.println("\t- Fragment " + i + ": " + dbFragsNumModels[i] + " models");
		}
	}
	
	// Read an HMM (binary, HMMER 2.0, not byteswap) and return its length in bytes
	private static long readHMM(BufferedInputStream bis, boolean swap) throws Exception {
		long modelBytes = 0;
		int toSkip;
		
		// Magic number
		try {
			int magic;
			if (swap) magic = Integer.reverseBytes(readInt(bis));
			else      magic = readInt(bis);
			// Check if it's either HMMER 2.0 in binary, or the swapped version
			if (magic != v20Magic)
				throw new Exception("Error: unsupported format for binary DB, must be HMMER 2.0");
		}
		catch (EOFException e) {
			return modelBytes;
		}
		modelBytes += INT_SIZE;
		// Flags
		int flags;
		if (swap) flags = Integer.reverseBytes(readInt(bis));
		else	  flags = readInt(bis);
		modelBytes += INT_SIZE;
		// Name
		if (swap) toSkip = Integer.reverseBytes(readInt(bis)) * CHAR_SIZE;
		else	  toSkip = readInt(bis) * CHAR_SIZE;
		skip(bis, toSkip);
		modelBytes += (toSkip + INT_SIZE);
		// Accession
		if ((flags & PLAN7_ACC) > 0)  {
			if (swap) toSkip = Integer.reverseBytes(readInt(bis)) * CHAR_SIZE;
			else 	  toSkip = readInt(bis) * CHAR_SIZE;
			skip(bis, toSkip);
			modelBytes += (toSkip + INT_SIZE);
		}
		// Description
		if ((flags & PLAN7_DESC) > 0) {
			if (swap) toSkip = Integer.reverseBytes(readInt(bis)) * CHAR_SIZE;
			else 	  toSkip = readInt(bis) * CHAR_SIZE;
			skip(bis, toSkip);
			modelBytes += (toSkip + INT_SIZE);
		}
		// Model length
		int modelLength;
		if (swap) modelLength = Integer.reverseBytes(readInt(bis));
		else 	  modelLength = readInt(bis);
		modelBytes += INT_SIZE;
		// Alphabet type
		readInt(bis);
		modelBytes += INT_SIZE;
		// RF alignment annotation
		if ((flags & PLAN7_RF) > 0) {
			toSkip = (modelLength + 1) * CHAR_SIZE; 
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// CS alignment annotation
		if ((flags & PLAN7_CS) > 0) {
			toSkip = (modelLength + 1) * CHAR_SIZE;
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// Alignment map annotation
		if ((flags & PLAN7_MAP) > 0) {
			toSkip = (modelLength + 1) * INT_SIZE;
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// Command line log
		if (swap) toSkip = Integer.reverseBytes(readInt(bis)) * CHAR_SIZE;
		else      toSkip = readInt(bis) * CHAR_SIZE;
		skip(bis, toSkip);
		modelBytes += (toSkip + INT_SIZE);
		// Nseq
		readInt(bis);
		modelBytes += INT_SIZE;
		// Creation time
		if (swap) toSkip = Integer.reverseBytes(readInt(bis)) * CHAR_SIZE;
		else      toSkip = readInt(bis) * CHAR_SIZE;
		skip(bis, toSkip);
		modelBytes += (toSkip + INT_SIZE);
		// Checksum
		readInt(bis);
		modelBytes += INT_SIZE;
		// Pfam gathering thresholds
		if ((flags & PLAN7_GA) > 0) {
			toSkip = 2 * FLOAT_SIZE;
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// Pfam trusted cutoffs
		if ((flags & PLAN7_TC) > 0) {
			toSkip = 2 * FLOAT_SIZE;
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// Pfam noise cutoffs
		if ((flags & PLAN7_NC) > 0) {
			toSkip = 2 * FLOAT_SIZE;
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// Specials
		toSkip = 4 * 2 * FLOAT_SIZE;
		skip(bis, toSkip);
		modelBytes += toSkip;
		// Null model
		toSkip = FLOAT_SIZE + ALPHABET_SIZE * FLOAT_SIZE;
		skip(bis, toSkip);
		modelBytes += toSkip;
		// EVD stats
		if ((flags & PLAN7_STATS) > 0) {
			toSkip = 2 * FLOAT_SIZE;
			skip(bis, toSkip);
			modelBytes += toSkip;
		}
		// Entry/exit probabilities
		toSkip = (2 * modelLength + 3) * FLOAT_SIZE;
		skip(bis, toSkip);
		modelBytes += toSkip;
		// Main model
		toSkip =    modelLength 	 * ALPHABET_SIZE   * FLOAT_SIZE
			  	 + (modelLength - 1) * ALPHABET_SIZE   * FLOAT_SIZE
			  	 + (modelLength - 1) * TRANS_PROB_SIZE * FLOAT_SIZE;
		modelBytes += toSkip;
		skip(bis, toSkip);
		
		return modelBytes;
	}
	
	private static int readInt(BufferedInputStream bis) throws Exception {
	    int toRead = INT_SIZE;
	    int read = 0, sumRead = 0;
	    while (toRead > 0) {
	    	read = bis.read(intBytes, sumRead, toRead);
	    	if (read > 0)  		{ toRead -= read; sumRead += read; }
	    	else if (read == 0) Thread.sleep(100);
	    	else		   		throw new EOFException("End of file");
	    }
	   
		return (  ((intBytes [0] & 0xff) << 24)
				| ((intBytes [1] & 0xff) << 16)
				| ((intBytes [2] & 0xff) << 8)
				|  (intBytes [3] & 0xff));
	}
	
	private static void skip(BufferedInputStream bis, int toSkip) throws Exception {
		int skipped;
		while (toSkip > 0) {
			skipped = (int)bis.skip(toSkip);
			if (skipped == 0) throw new EOFException("Error: unexpected end of file");
			toSkip -= skipped;
		}
	}
	
	
	// Returns true if both output fragments belong to the same fragment of the query sequences file
	private static boolean sameSeqFragment(String frag1, String frag2) {
		int i1 = frag1.lastIndexOf(SEQUENCE) + SEQUENCE.length();
		int i2 = frag2.lastIndexOf(SEQUENCE) + SEQUENCE.length();
		
		while (frag1.charAt(i1) != '_' && frag2.charAt(i2) != '_')
			if (frag1.charAt(i1++) != frag2.charAt(i2++)) return false;
		
		if (frag1.charAt(i1) == '_' && frag2.charAt(i2) == '_')
			return true;
		else
			return false;
	}
	
	
	// Returns true if both output fragments belong to the same fragment of the database file
	private static boolean sameDBFragment(String frag1, String frag2) {
		int i1 = frag1.lastIndexOf(DATABASE) + DATABASE.length();
		int i2 = frag2.lastIndexOf(DATABASE) + DATABASE.length();
		
		while (frag1.charAt(i1) != '.' && frag2.charAt(i2) != '.')
			if (frag1.charAt(i1++) != frag2.charAt(i2++)) return false;
		
		if (frag1.charAt(i1) == '.' && frag2.charAt(i2) == '.')
			return true;
		else
			return false;
	}
	
	
	private static void prepareResultFile(String source,
										  String dest,
										  String seqsName,
										  String dbName) throws IOException {
		
		String headerEnd = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -";
		
		String header =   "hmmpfam - search one or more sequences against HMM database\n"
						+ "HMMER 2.3.2 (Oct 2003)\n"
						+ "Copyright (C) 1992-2003 HHMI/Washington University School of Medicine\n"
						+ "Freely distributed under the GNU General Public License (GPL)\n"
						+ "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n"
						+ "HMM file:                 " + dbName   + "\n"
						+ "Sequence file:            " + seqsName + "\n"
						+ headerEnd + "\n";
		
		if (debug) {
			System.out.println("\nPreparing result file: ");
			System.out.println("\t- Source: " + source);
			System.out.println("\t- Destination: " + dest);
		}
		
		// Open the destination file and write the final header
		FileOutputStream dstFos = new FileOutputStream(dest);
		dstFos.write(header.getBytes());
		FileChannel dstChannel = dstFos.getChannel();
		
		// Open the source file and find the end of the current header, which must be replaced
		RandomAccessFile srcRaf = new RandomAccessFile(source, "r");
		FileChannel srcChannel = srcRaf.getChannel();
		String line = srcRaf.readLine();
		while (line != null && !line.equals(headerEnd)) line = srcRaf.readLine();
		long iniPos = srcChannel.position();
		
		// Copy the source file to the destination file, preserving the final header but not the old one
		srcChannel.transferTo(iniPos, srcChannel.size(), dstChannel);
		
		srcRaf.close();
		dstFos.close();
		srcChannel.close();
		dstChannel.close();
	}
	
	
	// Class to parse and store the command line arguments of hmmpfam provided by the user
	private static class CommandLineArgs {
		
		private StringBuilder commandLineArgs;
		private int aLimit;
		private int threshZ;
		
		public CommandLineArgs(String[] args, int startingPos) {
			commandLineArgs = new StringBuilder("--cpu 0");
			aLimit = -1;
			threshZ = -1;
			
			for (int i = startingPos; i < args.length; i++) {
				     if (args[i].equals("-n"))			System.out.println("-n option not supported, ignoring");
				else if (args[i].equals("-A")) 			{
														commandLineArgs.append(" ").append(args[i]).append(" ").append(args[++i]);
				     									aLimit = Integer.parseInt(args[i]);
				     									}
				else if (args[i].equals("-E"))			commandLineArgs.append(" ").append(args[i]).append(" ").append(args[++i]);
				else if (args[i].equals("-T"))			commandLineArgs.append(" ").append(args[i]).append(" ").append(args[++i]);
				else if (args[i].equals("-Z"))			{
														commandLineArgs.append(" ").append(args[i]).append(" ").append(args[++i]);
														threshZ = Integer.parseInt(args[i]);
														}
				else if (args[i].equals("--acc"))		commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--compat"))	System.out.println("--compat option not supported, ignoring");
				else if (args[i].equals("--cpu"))		System.out.println("--cpu: ignoring provided value");
				else if (args[i].equals("--cut_ga"))	commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--cut_nc"))	commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--cut_tc"))	commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--domE"))		commandLineArgs.append(" ").append(args[i]).append(" ").append(args[++i]);
				else if (args[i].equals("--domT"))		commandLineArgs.append(" ").append(args[i]).append(" ").append(args[++i]);
				else if (args[i].equals("--forward"))	commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--null2"))		commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--pvm"))		System.out.println("--pvm option not supported, ignoring");
				else if (args[i].equals("--xnu"))		commandLineArgs.append(" ").append(args[i]);
				else if (args[i].equals("--informat"))	{
														System.out.println("--informat option not supported, ignoring");
														i++;
														}
				else 									{
														System.err.println("Error: invalid option " + args[i]);
														System.exit(1);
														}
			}
		}
		
		public void addArg(String argName, String argValue) {
			commandLineArgs.append(" ").append(argName).append(" ").append(argValue);
		}
		
		public String getArgs() {
			return commandLineArgs.toString();
		}
		
		public int getALimit() {
			return aLimit;
		}
		
		public int getThreshZ() {
			return threshZ;
		}
		
		public boolean isADefined() {
			return aLimit >= 0;
		}
		
		public boolean isZDefined() {
			return threshZ >= 0;
		}
		
	}
	
}

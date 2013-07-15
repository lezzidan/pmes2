
package api.mergesort;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import worker.mergesort.MergeSortImpl;
import worker.mergesort.MergeSortAppException;

import integratedtoolkit.api.IntegratedToolkit;
import integratedtoolkit.api.impl.IntegratedToolkitImpl;


public class MergeSort {
	
	public static void main(String[] args) {
		
		if (args.length != 1 && args.length != 3) {
			System.out.println("Usage: java MergeSort file_to_sort [sequence_length seed]\n");
			return;
		}
		
		String fileToSort = args[0];
		
		// Generate random sequence if requested
		if (args.length > 1)
			new SequenceGenerator().generate(fileToSort,
											 Integer.parseInt(args[1]),
											 Long.parseLong(args[2]));
		
		int length = 0;
		try {
			FileReader fr = new FileReader(fileToSort);
			BufferedReader br = new BufferedReader(fr);
			
			length = Integer.parseInt(br.readLine());
			
			System.out.print("Input sequence is: ");
			for (int i = 0; i < length; i++)
				System.out.print(br.readLine() + " ");
			System.out.println("");
			
			br.close();
			fr.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		IntegratedToolkit it = new IntegratedToolkitImpl();
		it.startIT();
		
		MergeSort ms = new MergeSort();
		ms.sort(fileToSort, length);
		
		it.stopIT(true);
		
		try {
			FileReader fr = new FileReader(fileToSort);
			BufferedReader br = new BufferedReader(fr);
			
			length = Integer.parseInt(br.readLine());
			
			System.out.print("Sorted sequence is: ");
			for (int i = 0; i < length; i++)
				System.out.print(br.readLine() + " ");
			System.out.println("");
			
			br.close();
			fr.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// Delete intermediate files
		ms.clean(fileToSort);
		
	}
	
	public MergeSort() { }
	
	public void sort(String file, int length) {
		if (length == 1)
			return;
		
		try {
			String leftHalf = file + "l";
			String rightHalf = file + "r";
			MergeSortImpl.split(file, leftHalf, rightHalf);
				
			int leftLength = length / 2;
			int rightLength = length - leftLength;
			sort(leftHalf, leftLength);
			sort(rightHalf, rightLength);
				
			MergeSortImpl.merge(leftHalf, rightHalf, file);
		}
		catch (MergeSortAppException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void clean(String fileName) {
		File dir = new File(System.getProperty("user.dir"));
		String pattern = fileName + ".+";
		for (File f : dir.listFiles())
			if (f.getName().matches(pattern))
					f.delete();
	}
}

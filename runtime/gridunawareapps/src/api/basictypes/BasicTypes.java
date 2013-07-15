
package api.basictypes;

import java.io.BufferedReader;
import java.io.FileReader;

import integratedtoolkit.api.IntegratedToolkit;
import integratedtoolkit.api.IntegratedToolkit.*;
import integratedtoolkit.api.impl.IntegratedToolkitImpl;

import worker.basictypes.BasicTypesImpl;


public class BasicTypes {

	public static void main(String[] args) {
		
		// Start IT
		IntegratedToolkit it = new IntegratedToolkitImpl();
		it.startIT();
		
		BasicTypesImpl.testBasicTypes("test_file",
									  true,
									  'E',
									  "Test",
									  (byte)7,
									  (short)77,
									  777,
									  (long)7777,
									  7.7f,
									  7.77777);
		
		String fileName = it.openFile("test_file", OpenMode.READ);
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// Stop IT
        it.stopIT(true);
	}
	
}

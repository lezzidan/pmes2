
package sequential.basictypes;

import worker.basictypes.BasicTypesImpl;


public class BasicTypes {

	public static void main(String[] args) {
		
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
	
	}
	
}
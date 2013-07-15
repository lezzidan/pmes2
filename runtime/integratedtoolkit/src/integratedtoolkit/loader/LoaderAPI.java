
package integratedtoolkit.loader;

import integratedtoolkit.api.IntegratedToolkit.OpenMode;


public interface LoaderAPI {

	// Returns the renaming of the last file version just transferred
	String getFile(String fileName, String destDir);
	
	// Returns the renaming of the file version opened
	String openFile(String fileName, OpenMode m);
	
	void loaderError(boolean mustClean);
	
}

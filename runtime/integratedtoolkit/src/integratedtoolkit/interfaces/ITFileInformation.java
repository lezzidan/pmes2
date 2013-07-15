
package integratedtoolkit.interfaces;

import integratedtoolkit.types.file.FileInstanceId;


public interface ITFileInformation {

	// Returns the name of the given file version (actually a renaming performed by FIP)
	String getName(FileInstanceId fId);
	
}


package integratedtoolkit.interfaces;

import integratedtoolkit.types.file.FileAccessId;
import integratedtoolkit.types.file.Location;


public interface ITFileTransfer {

	void transferFileForOpen(FileAccessId faId, Location targetLocation);

	void transferFileRaw(FileAccessId faId, Location targetLocation);
	
	void checkResultFilesTransferred();
	
	void deleteIntermediateFiles();
	
}

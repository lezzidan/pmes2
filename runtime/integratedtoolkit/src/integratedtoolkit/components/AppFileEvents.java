
package integratedtoolkit.components;


// Events coming from File Manager
public interface AppFileEvents {
	
	void fileForOpenTransferred();
	
	void rawFileTransferred();
	
	void resultFilesTransferred();
	
	void intermediateFilesDeleted();
	
}

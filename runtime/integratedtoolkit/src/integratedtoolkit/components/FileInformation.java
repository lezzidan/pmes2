
package integratedtoolkit.components;

import integratedtoolkit.interfaces.ITFileInformation;

import integratedtoolkit.types.file.FileInstanceId;
import integratedtoolkit.types.file.Location;
import integratedtoolkit.types.file.ResultFile;
import java.util.List;
import java.util.Set;


// To request or update information about a file
public interface FileInformation extends ITFileInformation {
	
	// Returns a set of locations (host, path) which have a copy of the ﬁle
	List<Location> getLocations(FileInstanceId fId);

	// Returns the result files (final version) of the files associated with fileIds
	Set<ResultFile> getResultFiles(List<Integer> fileIds);
	
	void addLocation(FileInstanceId fId, String newHost, String newPath);
	
	void removeLocation(FileInstanceId fId, String oldHost, String oldPath);

	String getOriginalName(FileInstanceId fId);
	
	List<String> getFileLocations(String fileName);

	void addSize(FileInstanceId fId, List<Long> sizeModDate);

	long getSize(FileInstanceId fId);
}

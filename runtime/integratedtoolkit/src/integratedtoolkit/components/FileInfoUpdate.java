
package integratedtoolkit.components;

import integratedtoolkit.types.file.FileInstanceId;
import integratedtoolkit.types.file.Location;
import java.util.List;


// To inform about new file versions
public interface FileInfoUpdate {
	
	void newFileVersion(FileInstanceId newFileId,
						String name,
						List<Location> locations);
	
}


package integratedtoolkit.components;

import java.util.List;

import integratedtoolkit.interfaces.ITFileAccess;
import integratedtoolkit.types.file.AccessParams;
import integratedtoolkit.types.file.FileAccessId;


/* To register a new file access, either from a task or from the main code of the application
 * To determine if a file has already been accessed
 */
public interface FileAccess extends ITFileAccess {
	
	public List<FileAccessId> registerFileAccesses(List<AccessParams> accesses);
	
}

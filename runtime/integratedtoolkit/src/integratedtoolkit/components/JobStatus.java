
package integratedtoolkit.components;

import java.util.Map;
import java.util.List;
import integratedtoolkit.types.file.*;

//To inform about the end of a job
public interface JobStatus {
	
	enum JobEndStatus {
		OK,
		TO_RESCHEDULE,
		TRANSFERS_FAILED,
		SUBMISSION_FAILED,
		EXECUTION_FAILED;
	}
	
	void notifyJobEnd(int jobId, JobEndStatus status, String message, Map<FileInstanceId,Location> locs, Map<FileInstanceId,List<Long>> sizeAndMod);
	
}

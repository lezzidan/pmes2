
package integratedtoolkit.components;

import org.gridlab.gat.URI;

import integratedtoolkit.types.file.FileInstanceId;
import integratedtoolkit.types.ExecutionParams;
import integratedtoolkit.types.Method;
import java.util.Map;
import java.util.List;

// To request the creation of a job
public interface JobCreation {
	
	int newJob(Method method, ExecutionParams execParams, List<FileInstanceId> filesToTransf);
	
	int newCleanJob(URI[] cleanScripts, String[] cleanParams);
	
	int jobRescheduled(Method method, ExecutionParams newExecParams, List<FileInstanceId> filesToTransf);

	void setSpeedMatrix(Map<String,Map<String,Float>> speedMatrix);
	
	Map<String,Map<String,Float>> getSpeedMatrix();
	
	Map<String,Integer> getHostToRetry();
	
	Map<String,Boolean> getHostToUsedInRun();

	void speedMatrixUpdate(String source, String target, float netSpeed);
}

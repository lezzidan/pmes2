package integratedtoolkit.components;

import integratedtoolkit.types.file.*;

//Updates the file information allocated in scheduler
public interface SchedulerUpdate {

	void setFileInformation(FileInfo f);

	void addNewVersion(FileInstanceId fId);
        
        void resetExecTimeHistorical();
}

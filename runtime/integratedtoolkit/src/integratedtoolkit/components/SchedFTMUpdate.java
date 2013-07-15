package integratedtoolkit.components;

import integratedtoolkit.types.file.FileInstanceId;

public interface SchedFTMUpdate {

 void addLocation(FileInstanceId fId, String newHost, String newPath);

}

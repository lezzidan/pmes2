
package integratedtoolkit.api.impl;

import integratedtoolkit.interfaces.ITEvents;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.api.APIEvents;
import integratedtoolkit.api.APIEvents.*;


public class ITEventHandler implements ITEvents, ITError {

	// Integrated Toolkit API to transmit events to
	private static APIEvents itAPI;
	
	public static void init(APIEvents api) {
		itAPI = api;
	}
	
	
	public ITEventHandler() { }
	
	
	// ITEvents interface implementation (event handling)
	
	public void lastWriterTaskFinished() {
		itAPI.newFunctionalEvent(EventType.LAST_WRITER_TASK_FINISHED);
	}
	
	public void allTasksFinished() {
		itAPI.newFunctionalEvent(EventType.ALL_TASKS_FINISHED);
	}
	
	public void fileForOpenTransferred() {
		itAPI.newFunctionalEvent(EventType.FILE_FOR_OPEN_TRANSFERRED);
	}
	
	public void rawFileTransferred() {
		itAPI.newFunctionalEvent(EventType.RAW_FILE_TRANSFERRED);
	}
	
    public void resultFilesTransferred() {
        itAPI.newFunctionalEvent(EventType.RESULT_FILES_TRANSFERRED);
    }
	
	public void intermediateFilesDeleted() {
		itAPI.newFunctionalEvent(EventType.INTERMEDIATE_FILES_DELETED);
	}
	
	
	// ITError interface implementation (error management)
	
	public void throwError(String componentName,
						   String errorMessage,
						   Exception exception) {
		
		itAPI.newErrorEvent(true);
		
		System.err.println("\n----------------- INTEGRATED TOOLKIT ERROR -----------------");
		System.err.println("Error received from " + componentName);
		if (errorMessage.length() > 0) System.err.println(errorMessage);
		if (exception != null) 		   exception.printStackTrace();
		System.exit(1);
	}
	
}

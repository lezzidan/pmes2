
package integratedtoolkit.api;


public interface APIEvents {

	public enum EventType {
		LAST_WRITER_TASK_FINISHED,
		ALL_TASKS_FINISHED,
		FILE_FOR_OPEN_TRANSFERRED,
		RAW_FILE_TRANSFERRED,
		RESULT_FILES_TRANSFERRED,
		INTERMEDIATE_FILES_DELETED;
	}
	
	void newFunctionalEvent(EventType eType);
	
	void waitForFunctionalEvent(EventType eType);
	
	void newErrorEvent(boolean mustClean);
	
}

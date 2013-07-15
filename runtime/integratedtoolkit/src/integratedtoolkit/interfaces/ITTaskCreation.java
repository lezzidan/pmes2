
package integratedtoolkit.interfaces;

import integratedtoolkit.types.Parameter;


public interface ITTaskCreation {

	void newTask(String methodClass, String methodName, Parameter[] parameters);
	
	void noMoreTasks();
	
	int synchronizeWithPreviousCreations();
	
}

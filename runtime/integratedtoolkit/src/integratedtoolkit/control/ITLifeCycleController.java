
package integratedtoolkit.control;

import org.objectweb.fractal.api.control.IllegalLifeCycleException;
import org.objectweb.proactive.core.component.controller.ProActiveLifeCycleController;


public interface ITLifeCycleController extends ProActiveLifeCycleController {
	
	public void stopFc(boolean cleanup) throws IllegalLifeCycleException;
	
}

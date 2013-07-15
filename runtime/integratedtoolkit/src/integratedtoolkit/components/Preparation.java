
package integratedtoolkit.components;

import org.objectweb.proactive.core.util.wrapper.StringWrapper;


public interface Preparation  {
	
	StringWrapper initialize();
	
	void cleanup() throws Exception;
	
}

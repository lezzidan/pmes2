
package integratedtoolkit.interfaces;

import org.objectweb.proactive.core.component.type.annotations.multicast.Reduce;
import org.objectweb.proactive.core.component.type.annotations.multicast.ReduceMode;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;


public interface ITPreparation {

	@Reduce(reductionMode = ReduceMode.CUSTOM, customReductionMode = InitReduction.class)
	StringWrapper initialize();
	
	void cleanup() throws Exception;
	
}


package integratedtoolkit.interfaces;

import integratedtoolkit.ITConstants;

import org.objectweb.proactive.core.component.exceptions.ReductionException;
import org.objectweb.proactive.core.component.type.annotations.multicast.ReduceBehavior;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;

import java.io.Serializable;
import java.util.List;

public class InitReduction implements ReduceBehavior, Serializable {

	public Object reduce(List<?> values) throws ReductionException {
		if (values.isEmpty()) {
            throw new ReductionException("No values to perform reduction on");
        }
        
        for (Object value : values) {
            if (!(value instanceof StringWrapper))
                throw new ReductionException("Wrong type: expected " + StringWrapper.class.getName() +
                							 " but received " + value.getClass().getName());
            
            String stringValue = ((StringWrapper)value).stringValue();
            if (!stringValue.equals(ITConstants.INIT_OK))
            	return value;
        }
        
        return new StringWrapper(ITConstants.INIT_OK);
    }
	
}

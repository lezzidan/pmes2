
package api.counter;

import integratedtoolkit.types.annotations.*;
import integratedtoolkit.types.annotations.ParamMetadata.*;


public interface CounterItf {

	@MethodConstraints(operatingSystemType = "WindowsXP")
	@ClassName("worker.simple.SimpleImpl")
	void increment(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file
	);
	
	@MethodConstraints(operatingSystemType = "WindowsXP")
	@ClassName("worker.counter.CounterImpl")
	@RealMethodName("increment")
	void increment2(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file
	);
	
}

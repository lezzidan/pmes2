
package sequential.streams;

import integratedtoolkit.types.annotations.ClassName;
//import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.Direction;
import integratedtoolkit.types.annotations.ParamMetadata.Type;

public interface StreamsItf {

	//@MethodConstraints(operatingSystemType = "Linux")
	@ClassName("worker.simple.SimpleImpl")
	void increment(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file
	);
	
}

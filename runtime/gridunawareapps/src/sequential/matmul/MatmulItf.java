
package sequential.matmul;

import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.*;


public interface MatmulItf {
	
	@MethodConstraints(hostQueue = "short", processorArchitecture = "Intel")
	@ClassName("worker.matmul.MatmulImpl")
	void multiplyAccumulative(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file1,
	
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file2,

		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file3
	);

}
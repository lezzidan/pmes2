
package sequential.cholesky;

import integratedtoolkit.types.annotations.*;
import integratedtoolkit.types.annotations.ParamMetadata.*;


public interface CholeskyItf {
	
	@MethodConstraints(operatingSystemType = "Linux")
	@ClassName("worker.cholesky.CholeskyImpl")
	void initialize(
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String file
	);
	
	@MethodConstraints(processorSpeed = 1.8f, processorCPUCount = 4)
	@ClassName("worker.cholesky.CholeskyImpl")
	void multiplyAccumulative(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file3,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file2,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file1
	);
	
	@MethodConstraints(memoryPhysicalSize = 0.5f, storageElemSize = 40f, hostQueue = "par")
	@ClassName("worker.cholesky.CholeskyImpl")
	void substract(
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String file3,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file2,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file1
	);
	
	@MethodConstraints(memoryVirtualSize = 8, appSoftware = "Xalan,Xerces")
	@ClassName("worker.cholesky.CholeskyImpl")
	void cholesky(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file
	);
	
	@MethodConstraints(processorArchitecture = "Intel", appSoftware="Xalan")
	@ClassName("worker.cholesky.CholeskyImpl")
	void choleskyDivision(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file2,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file1
	);
	
}


package api.meanvalue;

import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.*;


public interface MeanValueItf {

	@MethodConstraints(processorSpeed = 1.8f, processorCPUCount = 2, operatingSystemType = "Linux")
	@ClassName("worker.meanvalue.MeanValueImpl")
	void genRandom(
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String file1
	);

	@MethodConstraints(memoryPhysicalSize = 0.5f, storageElemSize = 40f, appSoftware = "Xalan,Xerces,ProActive")
	@ClassName("worker.meanvalue.MeanValueImpl")
	void mean(
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file1,

		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String file2
	);

}

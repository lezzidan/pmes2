
package sequential.mergesort;

import integratedtoolkit.types.annotations.ClassName;
//import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.Direction;
import integratedtoolkit.types.annotations.ParamMetadata.Type;


public interface MergeSortItf {
	
	@ClassName("worker.mergesort.MergeSortImpl")
	void split(
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String file,
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String fileL,
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String fileR
	);
	
	@ClassName("worker.mergesort.MergeSortImpl")
	void merge(
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String fileL,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String fileR,
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String file
	);
	
}

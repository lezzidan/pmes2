
package sequential.hmmer;

import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.Direction;
import integratedtoolkit.types.annotations.ParamMetadata.Type;


public interface HMMPfamItf {

	//@MethodConstraints(storageElemSize = 1.5f)
	@ClassName("worker.hmmer.HMMPfamImpl")
	void hmmpfam(
		@ParamMetadata(type = Type.STRING, direction = Direction.IN)
		String hmmpfamBin,
		@ParamMetadata(type = Type.STRING, direction = Direction.IN)
		String commandLineArgs,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String seqFile,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String dbFile,
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resultFile
	);

	//@MethodConstraints(storageElemSize = 1.5f)	
	@ClassName("worker.hmmer.HMMPfamImpl")
	void mergeSameSeq(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String resultFile1,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String resultFile2,
		@ParamMetadata(type = Type.INT, direction = Direction.IN)
		int aLimit
	);

	//@MethodConstraints(storageElemSize = 1.5f)	
	@ClassName("worker.hmmer.HMMPfamImpl")
	void mergeSameDB(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String resultFile1,
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String resultFile2
	);
	
}


package sequential.sim;

import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.Direction;
import integratedtoolkit.types.annotations.ParamMetadata.Type;


public interface SimItf {

	@ClassName("worker.sim.SimImpl")
	void simBin(
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String progModel,

                @ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String typesModel,

                @ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String funcModel,

                @ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String resultName,

                @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resSpec,

                @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resE,

                 @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resC,

                @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resV,

                @ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String simBinary,
		
		@ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String running_mode,

		@ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String commandArgs
	);

	@ClassName("worker.sim.SimImpl")
        void simBin(
		@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String progModel,

                @ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String typesModel,

                 @ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String resultName,

                @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resSpec,

                @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resE,

                 @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resC,

                @ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String resV,

                @ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String simBinary,

		@ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String running_mode,

                @ParamMetadata(type = Type.STRING, direction = Direction.IN)
                String commandArgs

	);
}

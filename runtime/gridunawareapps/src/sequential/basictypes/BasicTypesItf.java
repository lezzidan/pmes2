
package sequential.basictypes;

import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.Direction;
import integratedtoolkit.types.annotations.ParamMetadata.Type;


public interface BasicTypesItf {

	@ClassName("worker.basictypes.BasicTypesImpl")
	void testBasicTypes(
		@ParamMetadata(type = Type.FILE, direction = Direction.OUT)
		String file,
		@ParamMetadata(type = Type.BOOLEAN)
		boolean b,
		@ParamMetadata(type = Type.CHAR)
		char c,
		@ParamMetadata(type = Type.STRING)
		String s,
		@ParamMetadata(type = Type.BYTE)
		byte by,
		@ParamMetadata(type = Type.SHORT)
		short sh,
		@ParamMetadata(type = Type.INT)
		int i,
		@ParamMetadata(type = Type.LONG, direction = Direction.IN) // Direction not mandatory for basic types, default=IN
		long l,
		@ParamMetadata(type = Type.FLOAT)
		float f,
		@ParamMetadata(type = Type.DOUBLE)
		double d
	);

}

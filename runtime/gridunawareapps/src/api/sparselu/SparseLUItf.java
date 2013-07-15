
package api.sparselu;

//import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.ParamMetadata;
import integratedtoolkit.types.annotations.ParamMetadata.Direction;
import integratedtoolkit.types.annotations.ParamMetadata.Type;


public interface SparseLUItf {

	@ClassName("worker.sparselu.SparseLUImpl")
	void lu0(
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String diag
	);
	
	@ClassName("worker.sparselu.SparseLUImpl")
    void bdiv(
    	@ParamMetadata(type = Type.FILE, direction = Direction.IN)
    	String diag,
    	@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
    	String row
    );
    
	@ClassName("worker.sparselu.SparseLUImpl")
    void bmod(
    	@ParamMetadata(type = Type.FILE, direction = Direction.IN)
    	String row,
    	@ParamMetadata(type = Type.FILE, direction = Direction.IN)
    	String col,
    	@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
    	String inner
    );
	
	@ClassName("worker.sparselu.SparseLUImpl")
    void fwd(
    	@ParamMetadata(type = Type.FILE, direction = Direction.IN)
		String a,
		@ParamMetadata(type = Type.FILE, direction = Direction.INOUT)
		String b
	); 
	
}

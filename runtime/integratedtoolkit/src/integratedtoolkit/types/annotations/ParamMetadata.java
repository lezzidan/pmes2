
package integratedtoolkit.types.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ParamMetadata {

	public static enum Type {
		FILE,
		BOOLEAN,
		CHAR,
		STRING,
		BYTE,
		SHORT,
		INT,
		LONG,
		FLOAT,
		DOUBLE;
	}
	
	public static enum Direction {
		IN,
		OUT,
		INOUT;
	}
	
	Type type();
	// Set default direction=IN for basic types
	Direction direction() default Direction.IN;
	
}

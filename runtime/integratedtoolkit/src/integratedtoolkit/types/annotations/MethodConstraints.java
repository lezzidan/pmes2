
package integratedtoolkit.types.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodConstraints {

	String 	processorArchitecture() default "[unassigned]";
	int 	processorCPUCount()		default 0;
	float 	processorSpeed()		default 0;					// in GHz
	
	float 	memoryPhysicalSize()	default 0; 					// in GB
	float 	memoryVirtualSize()		default 0; 					// in GB
	float	memoryAccessTime()		default 0;					// in ns
	float 	memorySTR()				default 0;	 				// in GB/s
	
	float 	storageElemSize() 		default 0; 					// in GB
	float	storageElemAccessTime()	default 0; 					// in ms
	float 	storageElemSTR()		default 0;					// in MB/s
	
	String 	operatingSystemType()	default "[unassigned]";
	
	String 	hostQueue() 			default "[unassigned]";
	
	String 	appSoftware() 			default "[unassigned]";
}

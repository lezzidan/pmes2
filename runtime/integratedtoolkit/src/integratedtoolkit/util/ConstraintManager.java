
package integratedtoolkit.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import gnu.trove.map.hash.*;

import org.apache.log4j.Logger;

import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.Parameter;
import integratedtoolkit.types.Task;
import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.MethodConstraints;
import integratedtoolkit.types.annotations.RealMethodName;


public class ConstraintManager {
	
	private Class annotItfClass;
	private THashMap<Integer,String> constraints;
	private THashMap<Integer,String> constraintsPresched;
	private THashMap<Integer,String> queues;
	
	private static final String AVAIL_CONSTR_SHORT		    = "Capabilities/Host/TaskCount[text()<@MaxValue]";
	private static final String AVAIL_CONSTR_SHORT_PRESCHED = "Capabilities/Host/TaskCount[text()<@MaxValue]";
	private static final String AVAIL_CONSTR_LONG		    = "/ResourceList/Resource[" + AVAIL_CONSTR_SHORT + "]";
	private static final String AVAIL_CONSTR_LONG_PRESCHED	= "/ResourceList/Resource[" + AVAIL_CONSTR_SHORT_PRESCHED + "]";
	public static final  String NO_CONSTR 					= "/ResourceList/Resource";

	// Component logger - No need to configure, ProActive does
	private static final Logger logger = Logger.getLogger(Loggers.TS_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	
	// Java constructor
	public ConstraintManager(Class annotItfClass) throws ClassNotFoundException {
		this.annotItfClass = annotItfClass;
		int hashSize = ((annotItfClass.getDeclaredMethods().length * 3) / 2) + 1;
		this.constraints = new THashMap<Integer,String>(hashSize);
		this.constraintsPresched = new THashMap<Integer,String>(hashSize);
		this.queues = new THashMap<Integer,String>(hashSize);
	}
	
	// C constructor
	public ConstraintManager(String constraintsFile) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(constraintsFile));
		
		logger.debug("CONST_FILE" + constraintsFile);
		
		this.constraints = new THashMap<Integer,String>();
		this.constraintsPresched = new THashMap<Integer,String>();
		this.queues = new THashMap<Integer,String>();
		
		
		String methodConstr;
		int methodId = 0;
		while ((methodConstr = br.readLine()) != null) {

			ArrayList<String> requirements = new ArrayList<String>();

			if (methodConstr.compareTo("") == 0) {// No constraints defined
				constraints.put(methodId, AVAIL_CONSTR_LONG);
				continue;
			}
			String[] constrPairs = methodConstr.split(",");
			for (String pair : constrPairs) {
				int start = 0, end, equal = 0;
				
				while (pair.charAt(equal) !='=') equal ++;
				String cap = pair.substring(0, equal).trim();
				
				String aux = pair.substring(equal+1).trim();
				String value = aux.substring(1, aux.length()-1);
				buildXPathConstraint(requirements, cap, value, methodId);
			}
			
			// Build XPath for normal scheduling
			requirements.add(AVAIL_CONSTR_SHORT);
			StringBuilder constrXPath = new StringBuilder().append("/ResourceList/Resource[");
			for (Iterator<String> i = requirements.iterator(); i.hasNext(); ) {
				constrXPath.append(i.next());
				if (i.hasNext()) constrXPath.append(" and "); 
			}
			constrXPath.append("]");
			constraints.put(methodId, constrXPath.toString());
			
			// Build XPath for presched
			requirements.remove(requirements.size() - 1); // Remove AVAIL_CONSTR_SHORT
			requirements.add(AVAIL_CONSTR_SHORT_PRESCHED);
			constrXPath = new StringBuilder().append("/ResourceList/Resource[");
			for (Iterator<String> i = requirements.iterator(); i.hasNext(); ) {
				constrXPath.append(i.next());
				if (i.hasNext()) constrXPath.append(" and "); 
			}
			constraintsPresched.put(methodId, constrXPath.toString());
			logger.debug("METHOD: "+ methodId + "CONSTRAINT: "+ constrXPath.toString());
			methodId++;
		}
	}
	
	public String getConstraints(Task task) throws NoSuchMethodException {
		int methodId = task.getMethod().getId();
		String methodConstr = constraints.get(methodId);
		if (methodConstr != null)
			return methodConstr;
		
		// Get the corresponding method from annotated interface
		Method m = findMethodInItf(task);
		
		// Load constraints for the method if available
		Annotation constrAnnot = m.getAnnotation(MethodConstraints.class);
		if (constrAnnot == null) {
			methodConstr = AVAIL_CONSTR_LONG;
			constraints.put(methodId, methodConstr);
			return methodConstr;
		}
		
		// Build XPath expression
		List<String> requirements = buildXPathConstraints(constrAnnot, methodId);
		
		// If task count reaches its maximum, the resource must be unavailable
		requirements.add(AVAIL_CONSTR_SHORT);
		
		StringBuilder constrXPath = new StringBuilder().append("/ResourceList/Resource[");
		for (Iterator<String> i = requirements.iterator(); i.hasNext(); ) {
			constrXPath.append(i.next());
			if (i.hasNext()) constrXPath.append(" and "); 
		}
		
		constrXPath.append("]");
		
		methodConstr = constrXPath.toString();
		constraints.put(methodId, methodConstr);
		return methodConstr;
	}
	
	public String getConstraintsPresched(Task task) throws NoSuchMethodException {
		int methodId = task.getMethod().getId();
		String methodConstr = constraintsPresched.get(methodId);
		if (methodConstr != null)
			return methodConstr;
		
		// Get the corresponding method from annotated interface
		Method m = findMethodInItf(task);
		
		// Load constraints for the method if available
		Annotation constrAnnot = m.getAnnotation(MethodConstraints.class);
		if (constrAnnot == null) {
			methodConstr = AVAIL_CONSTR_LONG_PRESCHED;
			constraintsPresched.put(methodId, methodConstr);
			return methodConstr;
		}
		
		// Build XPath expression
		List<String> requirements = buildXPathConstraints(constrAnnot, methodId);
		
		// If task count reaches the double of the maximum, the resource must be unavailable
		requirements.add(AVAIL_CONSTR_SHORT_PRESCHED);
		
		StringBuilder constrXPath = new StringBuilder().append("/ResourceList/Resource[");
		for (Iterator<String> i = requirements.iterator(); i.hasNext(); ) {
			constrXPath.append(i.next());
			if (i.hasNext()) constrXPath.append(" and "); 
		}
		
		constrXPath.append("]");
		
		methodConstr = constrXPath.toString();
		constraintsPresched.put(methodId, methodConstr);
		return methodConstr;
	}
	
	public String getQueue(Task task) {
		return queues.get(task.getMethod().getId());
	}
	
	
	private Method findMethodInItf(Task task) throws NoSuchMethodException {
		Parameter[] taskParams = task.getMethod().getParameters();
		int numParams = taskParams.length;
		
		Class[] parClasses = new Class[numParams];
		for (int i = 0; i < numParams; i++) {
			switch (taskParams[i].getType()) {
				case FILE_T:
					parClasses[i] = String.class;
					break;
				case BOOLEAN_T:
					parClasses[i] = boolean.class;
					break;
				case CHAR_T:
					parClasses[i] = char.class;
					break;
				case STRING_T:
					parClasses[i] = String.class;
					break;
				case BYTE_T:
					parClasses[i] = byte.class;
					break;
				case SHORT_T:
					parClasses[i] = short.class;
					break;
				case INT_T:
					parClasses[i] = int.class;
					break;
				case LONG_T:
					parClasses[i] = long.class;
					break;
				case FLOAT_T:
					parClasses[i] = float.class;
					break;
				case DOUBLE_T:
					parClasses[i] = double.class;
					break;
			}
		}
		
		Method method = annotItfClass.getDeclaredMethod(task.getMethod().getName(), parClasses);
		
		// Check if the class of the found method matches the class of the task method
		if (method.getAnnotation(ClassName.class).value().equals(task.getMethod().getDeclaringClass()))
			return method;
		else {
			// There are two identical methods from different classes
			mainLoop: for (Method m : annotItfClass.getDeclaredMethods()) {
				Annotation nameAnnot = m.getAnnotation(RealMethodName.class);
				if (nameAnnot != null
					&& ((RealMethodName)nameAnnot).value().equals(task.getMethod().getName())
					&& m.getAnnotation(ClassName.class).value().equals(task.getMethod().getDeclaringClass())
					&& m.getParameterTypes().length == numParams) {
						Class[] parTypes = m.getParameterTypes();
						for (int i = 0; i < numParams; i++)
							if (!parTypes[i].getName().equals(parClasses[i].getName()))
								continue mainLoop;
						return m;
				}
			}
		}
		throw new NoSuchMethodException("Cannot find the method " + task.getMethod().getName());
	}
	
	// Java
	private List<String> buildXPathConstraints(Annotation constrAnnot, int methodId) {
		ArrayList<String> requirements = new ArrayList<String>();
		
		String procArch    		= ((MethodConstraints)constrAnnot).processorArchitecture();
		float  cpuSpeed	   		= ((MethodConstraints)constrAnnot).processorSpeed();
		int    cpuCount	   		= ((MethodConstraints)constrAnnot).processorCPUCount();
		String osType	   		= ((MethodConstraints)constrAnnot).operatingSystemType();
		float  physicalMemSize  = ((MethodConstraints)constrAnnot).memoryPhysicalSize();
		float  virtualMemSize  	= ((MethodConstraints)constrAnnot).memoryVirtualSize();
		float  memoryAT    		= ((MethodConstraints)constrAnnot).memoryAccessTime();
		float  memorySTR   		= ((MethodConstraints)constrAnnot).memorySTR();
		float  diskSize    		= ((MethodConstraints)constrAnnot).storageElemSize();
		float  diskAT      		= ((MethodConstraints)constrAnnot).storageElemAccessTime();
		float  diskSTR     		= ((MethodConstraints)constrAnnot).storageElemSTR();
		String queue	   		= ((MethodConstraints)constrAnnot).hostQueue();
		String appSoftware 		= ((MethodConstraints)constrAnnot).appSoftware();
		
		// Translation of constraints : Java Annotations -> XPath expression
		if (!procArch.equals("[unassigned]"))
			requirements.add("Capabilities/Processor/Architecture[text()='" + procArch + "']");
		if (cpuSpeed > 0)
			requirements.add("Capabilities/Processor/Speed[text()>=" + cpuSpeed + "]");
		if (cpuCount > 0)
			requirements.add("Capabilities/Processor/CPUCount[text()>=" + cpuCount + "]");
		if (!osType.equals("[unassigned]"))
			requirements.add("Capabilities/OS/OSType[text()='" + osType + "']");
		if (physicalMemSize > 0)
			requirements.add("Capabilities/Memory/PhysicalSize[text()>=" + physicalMemSize + "]");
		if (virtualMemSize > 0)
			requirements.add("Capabilities/Memory/VirtualSize[text()>=" + virtualMemSize + "]");
		if (memoryAT > 0)
			requirements.add("Capabilities/Memory/AccessTime[text()<=" + memoryAT + "]");
		if (memorySTR > 0)
			requirements.add("Capabilities/Memory/STR[text()>=" + memorySTR + "]");
		if (diskSize > 0)
			requirements.add("Capabilities/StorageElement/Size[text()>=" + diskSize + "]");
		if (diskAT > 0)
			requirements.add("Capabilities/StorageElement/AccessTime[text()<=" + diskAT + "]");
		if (diskSTR > 0)
			requirements.add("Capabilities/StorageElement/STR[text()>=" + diskSTR + "]");
		if (!queue.equals("[unassigned]")) {
			queues.put(methodId, queue);
			if (queue.equals("any"))
				requirements.add("Capabilities/Host[Queue]");
			else
				requirements.add("Capabilities/Host/Queue[text()='" + queue + "']");
		}
		if (!appSoftware.equals("[unassigned]")) {
			String[] software = appSoftware.split(",");
			for (String s : software)
				requirements.add("Capabilities/ApplicationSoftware/Software[text()='" + s + "']");
		}
		
		return requirements;
	}
	
	
	// C
	private void buildXPathConstraint(List<String> requirements, String cap, String value, int methodId) {
		if 		(cap.equals("processorArchitecture"))
			requirements.add("Capabilities/Processor/Architecture[text()='" + value + "']");
		else if (cap.equals("processorSpeed"))
			requirements.add("Capabilities/Processor/Speed[text()>=" + value + "]");
		else if (cap.equals("processorCPUCount"))
			requirements.add("Capabilities/Processor/CPUCount[text()>=" + value + "]");
		else if (cap.equals("operatingSystemType"))
			requirements.add("Capabilities/OS/OSType[text()='" + value + "']");
		else if (cap.equals("memoryPhysicalSize"))
			requirements.add("Capabilities/Memory/PhysicalSize[text()>=" + value + "]");
		else if (cap.equals("memoryVirtualSize"))
			requirements.add("Capabilities/Memory/VirtualSize[text()>=" + value + "]");
		else if (cap.equals("memoryAccessTime"))
			requirements.add("Capabilities/Memory/AccessTime[text()<=" + value + "]");
		else if (cap.equals("memorySTR"))
			requirements.add("Capabilities/Memory/STR[text()>=" + value + "]");
		else if (cap.equals("storageElemSize"))
			requirements.add("Capabilities/StorageElement/Size[text()>=" + value + "]");
		else if (cap.equals("storageElemAccessTime"))
			requirements.add("Capabilities/StorageElement/AccessTime[text()<=" + value + "]");
		else if (cap.equals("storageElemSTR"))
			requirements.add("Capabilities/StorageElement/STR[text()>=" + value + "]");
		else if (cap.equals("hostQueue")) {
			queues.put(methodId, value);
			if (value.equals("any"))
				requirements.add("Capabilities/Host[Queue]");
			else
				requirements.add("Capabilities/Host/Queue[text()='" + value + "']");
		}
		else if (cap.equals("appSoftware")) {
			String[] software = value.split(",");
			for (String s : software)
				requirements.add("Capabilities/ApplicationSoftware/Software[text()='" + s + "']");
		}
	}
	
}

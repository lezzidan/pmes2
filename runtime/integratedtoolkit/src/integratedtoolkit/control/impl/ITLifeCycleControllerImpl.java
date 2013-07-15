
package integratedtoolkit.control.impl;

import integratedtoolkit.ITConstants;
import integratedtoolkit.components.Preparation;
import integratedtoolkit.control.ITLifeCycleController;

import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.objectweb.proactive.core.component.Constants;
import org.objectweb.proactive.core.component.Fractive;
import org.objectweb.proactive.core.component.controller.MembraneController;
import org.objectweb.proactive.core.component.controller.ProActiveLifeCycleControllerImpl;
import org.objectweb.proactive.core.component.controller.PriorityController;
import org.objectweb.proactive.core.component.controller.PriorityController.RequestPriority;
import org.objectweb.proactive.core.component.type.ProActiveTypeFactoryImpl;

import org.objectweb.fractal.api.Component;
import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.api.control.LifeCycleController;
import org.objectweb.fractal.api.control.IllegalLifeCycleException;
import org.objectweb.fractal.api.control.NameController;
import org.objectweb.fractal.api.factory.InstantiationException;
import org.objectweb.fractal.api.type.TypeFactory;
import org.objectweb.fractal.util.Fractal;


public class ITLifeCycleControllerImpl extends ProActiveLifeCycleControllerImpl
									   implements ITLifeCycleController {
	
	// Safe stopping order
	private static final String[] stoppingOrderIT = {ITConstants.TS, ITConstants.TA, ITConstants.JM, ITConstants.FM};
	private static final String[] stoppingOrderFM = {ITConstants.FTM, ITConstants.FIP};
	
	
	public ITLifeCycleControllerImpl(Component owner) {
		super(owner);
	}
	
	
	protected void setControllerItfType() {
        try {
            setItfType(ProActiveTypeFactoryImpl.instance()
                       .createFcItfType(Constants.LIFECYCLE_CONTROLLER,
                    		   			ITLifeCycleController.class.getName(),
                    		   			TypeFactory.SERVER, TypeFactory.MANDATORY,
                    		   			TypeFactory.SINGLE));
        }
        catch (InstantiationException e) {
            throw new ProActiveRuntimeException("cannot create controller " +
                this.getClass().getName());
        }
    }
	
	
	public void stopFc(boolean cleanup) {
		try {
			String hierarchicalType = owner.getComponentParameters().getHierarchicalType();
            if (hierarchicalType.equals(Constants.COMPOSITE)) {
            	String name = ((NameController)getFcItfOwner()
					  	  	  .getFcInterface(Constants.NAME_CONTROLLER))
					  	  	  .getFcName();
            	
                Component[] innerComponents = Fractal.getContentController(getFcItfOwner())
                							  .getFcSubComponents();
                
                if (innerComponents != null) {
                	// Sort the inner components according to the right stopping order
                	sortComponents(innerComponents, name);
                	// Stop the inner components
                    for (int i = 0; i < innerComponents.length; i++) {
                        try {
                            if (Fractive.getMembraneController(innerComponents[i]).getMembraneState()
                                .equals(MembraneController.MEMBRANE_STOPPED)) {
                                throw new IllegalLifeCycleException(
                                          "Before stopping all subcomponents, make sure that the membrane of all them is started");
                            }
                        }
                        catch (NoSuchInterfaceException e) {
                            // The subcomponent doesn't have a MembraneController, no need to check what's in the previous try block
                        }
                        
                        // Increase the priority of the stopFc method
                        PriorityController pc = (PriorityController)innerComponents[i]
                                                .getFcInterface(Constants.REQUEST_PRIORITY_CONTROLLER);
                        pc.setPriority(Constants.LIFECYCLE_CONTROLLER,
                        			   "stopFc",
                        			   new Class[] { boolean.class },
                        			   RequestPriority.NF2);
                        
                        controllerLogger.info("Trying to stop subcomponent "
                        					  + ((NameController)innerComponents[i].getFcInterface(Constants.NAME_CONTROLLER)).getFcName()
                        					  + " with cleanup " + cleanup);
                        
                        // Stop the inner component
                        ((ITLifeCycleController)innerComponents[i].getFcInterface(Constants.LIFECYCLE_CONTROLLER)).stopFc(cleanup);
                    }
                }
            }
            else if (cleanup) { // Primitive
            	controllerLogger.info("Component "
            						  + ((NameController)getFcItfOwner().getFcInterface(Constants.NAME_CONTROLLER)).getFcName()
            						  + " performing cleanup");
            	Preparation prepItf = ((Preparation)getFcItfOwner().getFcInterface("Preparation"));
            	prepItf.cleanup();
            }
            
            fcState = LifeCycleController.STOPPED;
            
            controllerLogger.info("Component "
            					  + ((NameController)getFcItfOwner().getFcInterface(Constants.NAME_CONTROLLER)).getFcName()
            					  + " stopped");
        }
		catch (NoSuchInterfaceException nsie) {
            controllerLogger.error("interface not found : " + nsie.getMessage());
        }
		catch (IllegalLifeCycleException ilce) {
            controllerLogger.error("illegal life cycle operation : " + ilce.getMessage());
        }
		catch (Exception e) {
			// Will never be thrown
		}
	}
	
	
	private void sortComponents(Component[] components, String superComponent) throws NoSuchInterfaceException {
		String[] sortingOrder;
		if (superComponent.equals(ITConstants.IT)) // IT supercomponent
			sortingOrder = stoppingOrderIT;
		else									   // FM supercomponent
			sortingOrder = stoppingOrderFM;
		
		for (int i = 0; i < components.length; i++) {
			String name = sortingOrder[i];
			for (int j = i; j < components.length; j++) {
				String name2 = ((NameController)components[j]
			  	  	      	   .getFcInterface(Constants.NAME_CONTROLLER))
			  	  	      	   .getFcName();
			  	
				if (name2.equals(name)) {
					if (i != j) {
						Component aux = components[j];
						components[j] = components[i];
						components[i] = aux;
					}
					break;
				}
			}
		}
	}
		
}

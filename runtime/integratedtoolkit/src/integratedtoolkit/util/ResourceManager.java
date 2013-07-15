
package integratedtoolkit.util;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import integratedtoolkit.ITConstants;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.xpath.domapi.XPathEvaluatorImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.xpath.XPathEvaluator;
//import org.w3c.dom.xpath.XPathNSResolver;
import org.w3c.dom.xpath.XPathResult;

import org.apache.log4j.Logger;
import integratedtoolkit.log.Loggers;

public class ResourceManager {

	// Component logger - No need to configure, ProActive does
        private static final Logger logger = Logger.getLogger(Loggers.TS_COMP);
        private static final boolean debug = logger.isDebugEnabled();

	private Document resourcesDoc;
	private XPathEvaluator evaluator;
	//private XPathNSResolver resolver;
	private List<String> resourceList;
	private ProjectManager projManager = new ProjectManager(); //Project Manager Instance

	public ResourceManager() throws Exception {
		String resourceFile = System.getProperty(ITConstants.IT_RES_FILE);
		
		// Parse the XML document which contains resource information
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    docFactory.setNamespaceAware(true);
	    resourcesDoc = docFactory.newDocumentBuilder().parse(resourceFile);
	    
	    // Validate the document against an XML Schema
	    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    Source schemaFile = new StreamSource(System.getenv("IT_HOME") + ITConstants.IT_RES_SCHEMA);
	    Schema schema = schemaFactory.newSchema(schemaFile);
	    Validator validator = schema.newValidator();
	    validator.validate(new DOMSource(resourcesDoc));
	    	    
	    // Create an XPath evaluator to solve queries
	    evaluator = new XPathEvaluatorImpl(resourcesDoc);
	    // resolver = evaluator.createNSResolver(resourcesDoc);
	    
	    NodeList nl = resourcesDoc.getChildNodes().item(0).getChildNodes();
	    int numRes = 0; 
	    for (int i = 0; i < nl.getLength(); i++) {
	    	Node n = nl.item(i);
	    	if (n.getNodeName().equals("Resource")) numRes++;
	    }
	    resourceList = new ArrayList<String>(numRes);
	}
	
	
	@SuppressWarnings("unchecked")
	public List<String> findResources(String constraints) {
		// Find the resources that match the constraints
	    XPathResult matchingRes = (XPathResult)evaluator.evaluate(constraints,
	    														  resourcesDoc,
	    														  /*resolver*/null,
	    														  XPathResult.UNORDERED_NODE_ITERATOR_TYPE,
	    														  null);
	    Node n;
	    Element e;
	    String resourceName;
	    resourceList.clear();
	    while ((n = matchingRes.iterateNext()) != null) {
	    	e = (Element)n;
	    	
	    	// Get current resource and add it to the list
	    	resourceName = e.getAttribute("Name");
	    	resourceList.add(resourceName);
	    }

		return resourceList;
	}


	 public String getNetworkSpeed(String resource) {

                String xPathToProp ="/ResourceList/Resource[@Name='" + resource + "']/Capabilities/NetworkAdaptor/NetworkSpeed";

                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                                  resourcesDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,
                                                                                                                  null);
                Node n = res.getSingleNodeValue();
                if (n == null)
                        return null;
                else
                        return n.getTextContent();
        }



	 public String getCPUSpeed(String resource) {

                String xPathToProp ="/ResourceList/Resource[@Name='" + resource + "']/Capabilities/Processor/Speed";

                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                                  resourcesDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,
                                                                                                                  null);
                Node n = res.getSingleNodeValue();
                if (n == null)
                        return null;
                else
                        return n.getTextContent();
        }

	public String getResCPUCount(String resource) {
              
		String xPathToProp ="/ResourceList/Resource[@Name='" + resource + "']/Capabilities/Processor/CPUCount";
                
		XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                         	  resourcesDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,
		                                                                                                  null);
                Node n = res.getSingleNodeValue();
                if (n == null)
                        return null;
                else
                        return n.getTextContent();
        }
	
	
	/* Alternative version of findResources to sort the resources by task count */
	@SuppressWarnings("unchecked")
	public List<String> findResourcesSorted(String constraints) {
        java.util.TreeMap<Integer,List<String>> map = new java.util.TreeMap<Integer,List<String>>();
        List<String> resourceList = new LinkedList<String>();

        // Find the resources that match the constraints
    	XPathResult matchingRes = (XPathResult)evaluator.evaluate(constraints,
                                                                  resourcesDoc,
                                                                  null,
                                                                  XPathResult.UNORDERED_NODE_ITERATOR_TYPE,                                                                                                                null);
	    Node n;
	    Element e;
	    String resourceName;
	    while ((n = matchingRes.iterateNext()) != null) {
	        e = (Element)n;
	        // Get current resource and add it to the list
	        resourceName = e.getAttribute("Name");
	
	        Element eTask = (Element)e.getElementsByTagName("TaskCount").item(0);
	        
	        Integer taskCount = Integer.parseInt(eTask.getTextContent());
	        List<String> l;
	        if ((l = map.get(taskCount)) == null) {
	                map.put(taskCount, l = new LinkedList<String>());
	        }
	        l.add(resourceName);
	    }
	    for (List l : map.values()) {
	        resourceList.addAll(l);
	    }
	    
	    return resourceList;
	}

		
	@SuppressWarnings("unchecked")
	public boolean matches(String resourceName, String constraints) {	
		List<String> resourceList = findResources(constraints);
		
		for (String s : resourceList)
			if (s.equals(resourceName))
				return true;
				
		return false;
	}
	
	
	public void setMaxTaskCount(String resourceName, String taskCount) {
		String xPath = "/ResourceList/Resource[@Name='" + resourceName + "']/Capabilities/Host/TaskCount";
		XPathResult taskCountRes = (XPathResult)evaluator.evaluate(xPath,
				  				  							 	   resourcesDoc,
				  				  							 	   null,
				  												   XPathResult.FIRST_ORDERED_NODE_TYPE,
				  												   null);
		Node taskCountNode = taskCountRes.getSingleNodeValue();
		Element taskCountElem = (Element)taskCountNode;
		taskCountElem.setAttribute("MaxValue", taskCount);
	}
	
	
	public void reserveResource(String resourceName) {

	       if(projManager.getProperty(resourceName, ITConstants.LIMIT_OF_TASKS) != null){
		modifyTaskCount(resourceName, 1);
	       }	
	}
	
	
	public void freeResource(String resourceName) {
	      if(projManager.getProperty(resourceName, ITConstants.LIMIT_OF_TASKS) != null){
		modifyTaskCount(resourceName, -1);
	      }
	}	
	
	
	public void freeAllResources() {
		String xPath = "/ResourceList/Resource/Capabilities/Host/TaskCount";
		XPathResult taskCountRes = (XPathResult)evaluator.evaluate(xPath,
				  				  								   resourcesDoc,
				  												   null,
				  												   XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
				  												   null);
		Node taskCountNode;
		// Use snapshot in order to modify the document
		for (int i = 0; i < taskCountRes.getSnapshotLength(); i++) {
			taskCountNode = taskCountRes.snapshotItem(i);
			taskCountNode.setTextContent(0 + "");
		}
	}
	
	
	private void modifyTaskCount(String resourceName, int addition) {
		String xPath = "/ResourceList/Resource[@Name='" + resourceName + "']/Capabilities/Host/TaskCount";
		XPathResult taskCountRes = (XPathResult)evaluator.evaluate(xPath,
				  				  								   resourcesDoc,
				  				  								   null,
				  				  								   XPathResult.FIRST_ORDERED_NODE_TYPE,
				  				  								   null);
		Node taskCountNode = taskCountRes.getSingleNodeValue();
		int taskCount = Integer.parseInt(taskCountNode.getTextContent());
		taskCount += addition;
		taskCountNode.setTextContent(taskCount + "");
		
		if(debug)
		{	
		 logger.debug("Resource "+resourceName+ " is now having "+taskCount);
		}

	}
	
}

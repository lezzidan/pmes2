package integratedtoolkit.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import integratedtoolkit.ITConstants;
import integratedtoolkit.log.Loggers;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.apache.xpath.domapi.XPathEvaluatorImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathResult;

public class ProjectManager {

	private Document projectDoc;
	private XPathEvaluator evaluator;
	
	//private XPathNSResolver resolver;
	
	private static final Logger logger = Logger.getLogger(Loggers.ALL_COMP);
	private static final boolean debug = logger.isDebugEnabled();

	public ProjectManager() throws Exception {
		String projectFile = System.getProperty(ITConstants.IT_PROJ_FILE);
	    // Parse the XML document which contains resource information
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    docFactory.setNamespaceAware(true);
	    projectDoc = docFactory.newDocumentBuilder().parse(projectFile);
	    
	    // Validate the document against an XML Schema
	    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    Source schemaFile = new StreamSource(System.getenv("IT_HOME") + ITConstants.IT_PROJ_SCHEMA);
	    Schema schema = schemaFactory.newSchema(schemaFile);
	    Validator validator = schema.newValidator();
	    validator.validate(new DOMSource(projectDoc));
	    
	    // Create an XPath evaluator to solve queries
	    evaluator = new XPathEvaluatorImpl(projectDoc);
	    //resolver = evaluator.createNSResolver(resourceDoc);
	}

	public String getProperty(String workerName, String property) {
		String xPathToProp = "/Project/Worker[@Name='" + workerName + "']/" + property;
		XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
					      								  projectDoc,
					 									  null,
					 									  XPathResult.FIRST_ORDERED_NODE_TYPE,
					 									  null);
		Node n = res.getSingleNodeValue();
		if (n == null)
			return null;
		else
			return n.getTextContent();
	}


	 public String getDataNodeProperty(String dataNodeName,  String property) {
                String xPathToProp = "/Project/DataNode[@Name='" + dataNodeName + "']/" + property;
                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          projectDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,
                                                                                                                  null);
                Node n = res.getSingleNodeValue();
                if (n == null)
                        return null;
                else
                        return n.getTextContent();
        }

	public String getMaxRetries(){

	        String xPathToProp = "/Project/MaxRetries";
                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          projectDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,
                                                                                                                  null);
                Node n = res.getSingleNodeValue();
                if (n == null)
                        return null;
                else
                        return n.getTextContent();
        }

	
	 public List<String> getFileLocations(String filename) {

                /*<Locations>
                  <File Name="ta_A13_CM2_sresb1_r1_200101-210012.nc">
                   <Path>file://bscgrid05.bsc.es/home/rrafanel/jra4_v2/worker/files/</Path>
                   <Path>file://bscgrid02.bsc.es/home/rrafanel/jra4_v2/worker/files/</Path>
                  </File>
                </Locations>*/
		
		List<String> locations = new LinkedList<String>();
	
                String xPathToProp =  "/Project/Locations/File[@Name='" + filename + "']/Path";
                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          projectDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.UNORDERED_NODE_ITERATOR_TYPE,                                                                                                                                               null);
		Node n;
           	while ((n = res.iterateNext()) != null) {
                  locations.add(n.getTextContent());
                }

                if (locations.isEmpty())
                        return null;
                else
                        return locations;
        }

	public long getFileLocationsSize(String filename){

	  Node n = getFileNode(filename);
	
          Element e;
          String size;
	  long l;
	  if(n != null){
           e = (Element)n;
           // Get size file attribute
           size = e.getAttribute("Size");
           l = Long.parseLong(size);     
           return l;
	  }
	  else{
	   return 0;
	  }
	}

	public long getFileLocationsLastMod(String filename){

          Node n = getFileNode(filename);

          Element e;
          String lastMod;
          long l;
          if(n != null){
           e = (Element)n;
           // Get size file attribute
           lastMod = e.getAttribute("LastModDate");
           l = Long.parseLong(lastMod);
           return l;
          }
          else{
           return 0;
          }
        }


	public Node getFileNode(String filename){

                String xPathToProp =  "/Project/Locations/File[@Name='" + filename + "']";
                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          projectDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,                                                                                                                  null);

	      Node n = res.getSingleNodeValue();
	      
	      if(n != null)
               return n;
	      else
	       return null;
        }
          

	public void setFileLocations(Map<String,List<String>> store, Map<String,Long> fileLastModDate){

           Node locations = null;
           Node oldLocations = null;
           Node oldFileNode = null;
           List<String> oldFileLocations = null;
           Iterator<String> io = null;
	   boolean fileNotDefined = false;

           String projectFile = System.getProperty(ITConstants.IT_PROJ_FILE);

           Node project = projectDoc.getElementsByTagName("Project").item(0);

           if(hasLocations()){
             locations = projectDoc.getElementsByTagName("Locations").item(0);
            }
            else{
             locations = projectDoc.createElement("Locations");
            }

              //There are Locations tag defined on project file
              for (Map.Entry<String,List<String>> e : store.entrySet()) {

               if(hasLocations()){
                oldFileNode = getFileNode(e.getKey());
		if(oldFileNode == null){
		 fileNotDefined = true;  
		}
               }

                //Creating child file
                Node f = projectDoc.createElement("File");
                Element file = (Element) f;
                file.setAttribute("Name",e.getKey());
		file.setAttribute("LastModDate", Long.toString(fileLastModDate.get(e.getKey())));
                f = (Node) file;

	        //Taking store uri's
                List<String> l = e.getValue();
                Iterator<String> il = l.iterator();

		if(!fileNotDefined){
                 if(hasLocations()){
                   oldFileLocations = getFileLocations(e.getKey());
                   io = oldFileLocations.iterator();

                   while(io.hasNext())
                    {
                     String nextElem = io.next();

                      //Appending paths
                      Node path = projectDoc.createElement("Path");
                      path.setTextContent(nextElem);
                      f.appendChild(path);
                    }
		  }
		}//End fileNotDefined

		while(il.hasNext())
                  {
                    String nextElem = il.next();
                    if(hasLocations()){
                      if(!oldFileLocations.contains(nextElem)){
                        //Appending paths
                        Node path = projectDoc.createElement("Path");
                        path.setTextContent(nextElem);
                        f.appendChild(path);
                      }
                    }
                    else{
                      //Appending paths
                      Node path = projectDoc.createElement("Path");
                      path.setTextContent(nextElem);
                      f.appendChild(path);
                    }
                  }

                  if(hasLocations() && (!fileNotDefined)){
                   locations.replaceChild(f,oldFileNode);
                  }
                  else{
                   locations.appendChild(f);
                  }
	
		//Resetting fileNotDefined flag
		fileNotDefined = false;

              } //End For

             if(!hasLocations()){
              project.appendChild(locations);
             }

           try{
           // Writting the changes to project file
           Transformer transformer = TransformerFactory.newInstance().newTransformer();
           transformer.setOutputProperty(OutputKeys.INDENT, "yes");
           transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(2));

           //Initializing StreamResult with File object to save
           StreamResult result = new StreamResult(projectFile);
           DOMSource source = new DOMSource(projectDoc);
           transformer.transform(source, result);

           } catch (TransformerConfigurationException e) {
           } catch (TransformerException e) {
          }
        }


     
	public boolean hasLocations(){

	  if(projectDoc.getElementsByTagName("Locations").item(0) != null){
                //There are Locations tag defined on project file
                return true;
          }
	  else{
	       return false;
	  }
	}


	
	public List<String> getWorkers() {
		List<String> workerList = new LinkedList<String>();
		
		// Find all the workers defined in the project file
		String xPathToWorkers = "/Project/Worker";
	    XPathResult workerRes = (XPathResult)evaluator.evaluate(xPathToWorkers,
	    														projectDoc,
	    														/*resolver*/null,
	    														XPathResult.UNORDERED_NODE_ITERATOR_TYPE,
	    														null);
	    Node n;
	    Element e;
	    String workerName;
	    while ((n = workerRes.iterateNext()) != null) {
	    	e = (Element)n;
	    	
	    	// Get current resource and add it to the list
	    	workerName = e.getAttribute("Name");
	    	workerList.add(workerName);
	    }
	    
	    return workerList;
	}

}

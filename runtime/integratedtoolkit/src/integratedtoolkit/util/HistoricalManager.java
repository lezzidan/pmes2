package integratedtoolkit.util;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.*;

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

public class HistoricalManager {

	private static final String PROJ_LOAD_ERR       = "Error loading project information";

	private Document histDoc;
	private XPathEvaluator evaluator;
	
	// Object that stores the information about the current project
        private ProjectManager projManager;
	
	//private XPathNSResolver resolver;
	
	private static final Logger logger = Logger.getLogger(Loggers.ALL_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	
	// Language
        private static final boolean isJava = System.getProperty(ITConstants.IT_LANG) != null
                                                                          && System.getProperty(ITConstants.IT_LANG).equals("java")
                                                                          ? true : false;

	public HistoricalManager() throws Exception {
  
	    if (projManager == null) {
               try {
                      projManager = new ProjectManager();
                   }
               catch (Exception e) {
                     logger.error(PROJ_LOAD_ERR, e);
                    }
            }

            String histFile = System.getProperty(ITConstants.IT_HIST_FILE);
	   
	    //Checking if the historical file is previously created
	    File f = new File(histFile);
	
	    //If file exist avoid to create it
	    if(!f.exists()){
	      
               if(debug) logger.debug("Creating Historical File.");

	      //Creating it if it doesn't exist.        
              try {
               f.createNewFile();

	       FileWriter fstream = new FileWriter(f);
               BufferedWriter out = new BufferedWriter(fstream);
               out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><RunStatistics>");
	       out.newLine();
	       out.write("</RunStatistics>");

               //Close the output stream
               out.close();

              }catch (IOException ioe) {
                logger.debug("Unable to Create/Open the Historical File.");
              }
	    }	   

	    // Parse the XML document which contains resource information
	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    docFactory.setNamespaceAware(true);
	    histDoc = docFactory.newDocumentBuilder().parse(histFile);
	    
	    // Validate the document against an XML Schema
	    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    Source schemaFile = new StreamSource(System.getenv("IT_HOME") + ITConstants.IT_HIST_SCHEMA);
	    Schema schema = schemaFactory.newSchema(schemaFile);
	    Validator validator = schema.newValidator();
	    validator.validate(new DOMSource(histDoc));
	    
	    // Create an XPath evaluator to solve queries
	    evaluator = new XPathEvaluatorImpl(histDoc);
	}


	public Map<String,Map<String,Float>> getSpeedMatrix() {

		int numLinks = 0;
		float sourceSpeed = 0;
                float targetSpeed = 0;
		
		boolean found = false;
		List<String> resources = new LinkedList<String>();
		List<String> workers = projManager.getWorkers();
		ResourceManager resManager = null;
                ConstraintManager constrManager = null;

		try {
                       resManager = new ResourceManager();
                    }catch (Exception e) {
                        logger.error("Error loading resource information", e);
                    }

		if (constrManager == null) {
                        if (isJava) {
                                try {
                                        String appName = System.getProperty(ITConstants.IT_APP_NAME);
                                        constrManager = new ConstraintManager(Class.forName(appName + "Itf"));
                                }catch (ClassNotFoundException e) {
                                        logger.error("Error loading application constraints", e);
                                }
                        }
                        else { // C binding
                                try {
                                        constrManager = new ConstraintManager(System.getProperty(ITConstants.IT_CONSTR_FILE));
                                        logger.error("Error loading application constraints");
                                }catch (Exception e) {
                                        logger.error("Error loading application constraints", e);
                                }
                        }
                }
                // Find all the nodes defined in speed matrix
                String xPathToLinks = "/RunStatistics/NetSpeed/Link";
                XPathResult linksRes = (XPathResult)evaluator.evaluate(xPathToLinks,
                                                                                                                        histDoc,
                                                                                                                        /*resolver*/null,
                                                                                                                        XPathResult.UNORDERED_NODE_ITERATOR_TYPE,
                                                                                                               null);
            Node n;
            Element e;	    
		
            while ((n = linksRes.iterateNext()) != null) {
                e = (Element)n;

                // Get current resource and add it to the list
                String resourceName = e.getAttribute("Src");
                 resources.add(resourceName);
		}
		
	    //If there isn't previous historical
             if(resources.isEmpty()){
               resources = resManager.findResources(ConstraintManager.NO_CONSTR);
             }
	     //If speed matrix exists
	     else{
	       List<String> definedResources = resManager.findResources(ConstraintManager.NO_CONSTR);

	       //Adding the new possible workers that aren't in speed matrix
               for(int i=0;i < workers.size();i++){
                 if(!resources.contains(workers.get(i))){
                     resources.add(workers.get(i));
                 }
               }
		
	       //Removing from matrix the non defined workers, excepting the master
	       for(int i=0;i < resources.size();i++){
                 if(!definedResources.contains(resources.get(i))){
                     resources.remove(resources.get(i));
                 }
               }
	    } 	

		Map<String,Map<String,Float>> hostToSpeedMatrix = new THashMap<String,Map<String,Float>>(resources.size());

		for(int i=0; i < resources.size();i++){
		  Map<String,Float> hostToLinks = new THashMap<String,Float>();
		  String existLink =  "/RunStatistics/NetSpeed/Link[@Src='" + resources.get(i) + "']";
		  XPathResult link = (XPathResult)evaluator.evaluate(existLink,
                                                                                                          histDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,                                                                                                                                                    null);          
		  Node t = link.getSingleNodeValue();
		  if(t != null){
  
		     for(int j=0; j < resources.size();j++){
		    String xPathToProp =  "/RunStatistics/NetSpeed/Link[@Src='" + resources.get(i) + "']/Speed[@Dst='" + resources.get(j) + "']";
                    XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          histDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,                                                                                                                                                    null);         	
		       
		        n = res.getSingleNodeValue();
                       if(n != null){
                        hostToLinks.put(resources.get(j),Float.parseFloat(n.getTextContent()));
                       }
		       else{
	 
			 if(resManager.getNetworkSpeed(resources.get(i)) != null){
			   sourceSpeed = Float.parseFloat(resManager.getNetworkSpeed(resources.get(i)));
			 }

			 if(resManager.getNetworkSpeed(resources.get(j)) != null){
                           targetSpeed = Float.parseFloat(resManager.getNetworkSpeed(resources.get(j)));
		 	 }

			 if(sourceSpeed == 0 || targetSpeed == 0){
                           hostToLinks.put(resources.get(j),Float.parseFloat(ITConstants.DEFAULT_NET_SPEED));
			 }
			 else{
			   if(sourceSpeed < targetSpeed){
                             hostToLinks.put(resources.get(j),sourceSpeed);
			   }
			   else{
			     hostToLinks.put(resources.get(j),targetSpeed);
			   }
			 }
		       }
                     }//For Workers
                    hostToSpeedMatrix.put(resources.get(i),hostToLinks);
		  }
		  else{
                     for(int j=0; j < resources.size();j++){

		         if(resManager.getNetworkSpeed(resources.get(i)) != null){
                           sourceSpeed = Float.parseFloat(resManager.getNetworkSpeed(resources.get(i)));
                         }
                         if(resManager.getNetworkSpeed(resources.get(j)) != null){
                           targetSpeed = Float.parseFloat(resManager.getNetworkSpeed(resources.get(j)));
                         }

                         if(sourceSpeed == 0 || targetSpeed == 0){
                           hostToLinks.put(resources.get(j),Float.parseFloat(ITConstants.DEFAULT_NET_SPEED));
                         }
                         else{
                           if(sourceSpeed < targetSpeed){
                             hostToLinks.put(resources.get(j),sourceSpeed);
                           }
                           else{
                             hostToLinks.put(resources.get(j),targetSpeed);
                           }
                         }
		     }
		     hostToSpeedMatrix.put(resources.get(i),hostToLinks);
		  }
		  
     	     }//For Id's

	    return hostToSpeedMatrix;
	}



	        public Map<Integer,Map<String,Float>> getMeanTimeStructure(String name) {

                int taskNum = 0;
                List<String> workers = projManager.getWorkers();
                Map<Integer,Map<String,Float>> taskTohostsMeanWTQueue = new THashMap<Integer,Map<String,Float>>();

                String xPathToProp =  "/RunStatistics/"+name+"/TaskType";
                XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          histDoc,
                                                                                                          null,                                                                                                                                                        XPathResult.UNORDERED_NODE_ITERATOR_TYPE,                                                                                                                    null);
                while (( res.iterateNext()) != null) {
                      taskNum++;
                }


                for(int i=0; i < taskNum;i++){

                  Map<String,Float> hostsToMeanWTQueue = new THashMap<String,Float>(workers.size());
                  String existTask =  "/RunStatistics/"+name+"/TaskType[@Id='" + i + "']";
                  XPathResult task = (XPathResult)evaluator.evaluate(existTask,
                                                                                                          histDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,                                                                                                                                                    null);
                  Node t = task.getSingleNodeValue();

                  if(t != null){
                     for(int j=0; j < workers.size();j++){
                      xPathToProp =  "/RunStatistics/"+name+"/TaskType[@Id='" + i + "']/Worker[@Name='" + workers.get(j) + "']";
                      res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          histDoc,
                                                                                                                  null,
                                                                                                                  XPathResult.FIRST_ORDERED_NODE_TYPE,                                                                                                                                                    null);
                       Node n = res.getSingleNodeValue();
                       if(n != null){
                        hostsToMeanWTQueue.put(workers.get(j),Float.parseFloat(n.getTextContent()));
                       }
                     }//For Workers
                    taskTohostsMeanWTQueue.put(i,hostsToMeanWTQueue);
                  }
             }//For Id's

	     return taskTohostsMeanWTQueue;
        }


	public Map<String,Float> getAvailability() {

	  List<String> workers = projManager.getWorkers();
	  
          Map<String,Float> hostsToAvailability = new THashMap<String,Float>(workers.size());

          for(int i=0; i < workers.size();i++){

	     String xPathToProp =  "/RunStatistics/Availability/Worker[@Name='" + workers.get(i) + "']";
             XPathResult res = (XPathResult)evaluator.evaluate(xPathToProp,
                                                                                                          histDoc,
                                                                                                          null,                                                                                                                                                        XPathResult.FIRST_ORDERED_NODE_TYPE,                                                                                                                    null);
             Node n = res.getSingleNodeValue();

             if(n != null){
                 hostsToAvailability.put(workers.get(i),Float.parseFloat(n.getTextContent()));
             }
	     else{
		hostsToAvailability.put(workers.get(i),(float) 1);
	     }
	 }
	  
	  return hostsToAvailability;
	}


	public void setAvailability(Map<String,Float> hostsToMeanAvail) {

	    /*<Availability>
                <Worker Name="bscgrid02.bsc.es">1</Worker>
                <Worker Name="bscgrid03.bsc.es">1</Worker>
            </Availability>*/

	   Node newAvail = null;
           Node oldAvail = null;

           String histFile = System.getProperty(ITConstants.IT_HIST_FILE);

           Node runstatistics = histDoc.getElementsByTagName("RunStatistics").item(0);

           if(histDoc.getElementsByTagName("Availability").item(0) != null){

              //There are defined availability tag on historical file
               oldAvail = histDoc.getElementsByTagName("Availability").item(0);
            }

           newAvail = histDoc.createElement("Availability");

           for (Map.Entry<String,Float> e : hostsToMeanAvail.entrySet()) {

              String host = e.getKey();
              Float value = e.getValue();

              //Creating child Worker
              Node w = histDoc.createElement("Worker");
              Element worker = (Element) w;
              worker.setAttribute("Name",e.getKey());
              w = (Node) worker;
              w.setTextContent(Float.toString(e.getValue()));

              newAvail.appendChild(w);
           }

           if(histDoc.getElementsByTagName("Availability").item(0) != null){
              //Replacing oldexecutionTime by the new one
              runstatistics.replaceChild(newAvail,oldAvail);
           }
           else{
                 //Look up if exists the tag Mean, if it's we remove it in order to insert it in order
                 if(histDoc.getElementsByTagName("MeanTimeInQueue").item(0) != null){
                    runstatistics.insertBefore(newAvail,histDoc.getElementsByTagName("MeanTimeInQueue").item(0)); 
		 }
	         else{
                    runstatistics.appendChild(newAvail);
                 }
           }

	   try{
           // Writting the changes to historical file
           Transformer transformer = TransformerFactory.newInstance().newTransformer();
           transformer.setOutputProperty(OutputKeys.INDENT, "yes");
           transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(2));
          
	   //Initializing StreamResult with object to save in the file
           StreamResult result = new StreamResult(histFile);
           DOMSource source = new DOMSource(histDoc);
           transformer.transform(source, result);
	
           } catch (TransformerConfigurationException e) {
           } catch (TransformerException e) {
          }
        }


	 public void setMeanExecTime(Map<Integer,Map<String,Float>> taskTohostsExecTime){

            /*<MeanExecTime>
              <TaskType Id="0">
                <Worker Name="bscgrid02.bsc.es">20.420502</Worker>
              </TaskType>

              <TaskType Id="1">
                <Worker Name="bscgrid02.bsc.es">21.595749</Worker>
              </TaskType>
            </MeanExecTime>*/

           Node executionTime= null;
           Node oldexecutionTime = null;

           String histFile = System.getProperty(ITConstants.IT_HIST_FILE);
           Node runstatistics = histDoc.getElementsByTagName("RunStatistics").item(0);

           if(histDoc.getElementsByTagName("MeanExecTime").item(0) != null){

              //There are defined MeanExecTime tag in historical file
               oldexecutionTime = histDoc.getElementsByTagName("MeanExecTime").item(0);
            }

          executionTime= histDoc.createElement("MeanExecTime");

           for (Map.Entry<Integer,Map<String,Float>> e : taskTohostsExecTime.entrySet()) {

              Integer taskId = e.getKey();
              Map<String,Float> hostsToMeanWTQueue = e.getValue();

              //Creating child TaskType
              Node tId = histDoc.createElement("TaskType");
              Element t = (Element) tId;
              t.setAttribute("Id",Integer.toString(e.getKey()));
              tId = (Node) t;

              for (Map.Entry<String,Float> o :  hostsToMeanWTQueue.entrySet()) {

                 //Creating child Worker
                 Node w = histDoc.createElement("Worker");
                 Element worker = (Element) w;
                 worker.setAttribute("Name",o.getKey());
                 w = (Node) worker;
                 w.setTextContent(Float.toString(o.getValue()));
                 tId.appendChild(w);
              }

               executionTime.appendChild(tId);
           }

           if(histDoc.getElementsByTagName("MeanExecTime").item(0) != null){
              //Replacing old MeanExecTime by the new one
              runstatistics.replaceChild(executionTime, oldexecutionTime);
           }
           else{
              Node availability = histDoc.getElementsByTagName("NetSpeed").item(0);
              if(availability != null){
                 runstatistics.insertBefore( executionTime,availability);
              }
              else{
                runstatistics.appendChild( executionTime);
              }
           }

           try{
           // Writting the changes to historical file
           Transformer transformer = TransformerFactory.newInstance().newTransformer();
           transformer.setOutputProperty(OutputKeys.INDENT, "yes");
           transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(2));

           //Initializing StreamResult with File object to save to file
           StreamResult result = new StreamResult(histFile);
           DOMSource source = new DOMSource(histDoc);
           transformer.transform(source, result);

           } catch (TransformerConfigurationException e) {
           } catch (TransformerException e) {
          }
        }     


        public void setSpeedMatrix(Map<String,Map<String,Float>> hostToSpeedMatrix){

        /*<NetSpeed>
         <Link Src="bscgrid02.bsc.es">
          <Speed Dst="bscgrid03.bsc.es">89</Speed>  
          <Speed Dst="bscgrid05.bsc.es">89</Speed>  
          <Speed Dst="bscgrid06.bsc.es">89</Speed>  
         </Link>
	</NetSpeed>*/

           Node netspeed = null;
           Node oldNetSpeed = null;

           String histFile = System.getProperty(ITConstants.IT_HIST_FILE);   

           Node runstatistics = histDoc.getElementsByTagName("RunStatistics").item(0);

           if(histDoc.getElementsByTagName("NetSpeed").item(0) != null){

              //There are defined NetSpeed tag in historical file
               oldNetSpeed = histDoc.getElementsByTagName("NetSpeed").item(0);
            }

            netspeed = histDoc.createElement("NetSpeed");


           for (Map.Entry<String,Map<String,Float>> e : hostToSpeedMatrix.entrySet()) {

              String link = e.getKey();
              Map<String,Float> hostToSpeed = e.getValue();

              //Creating child link
              Node li = histDoc.createElement("Link");
              Element l = (Element) li;
              l.setAttribute("Src",e.getKey());
              li = (Node) l;

              for (Map.Entry<String,Float> o :  hostToSpeed.entrySet()) {

                 //Creating child speed
                 Node s = histDoc.createElement("Speed");
                 Element speed = (Element) s;
                 speed.setAttribute("Dst",o.getKey());
                 s = (Node) speed;
                 s.setTextContent(Float.toString(o.getValue()));
                 li.appendChild(s);
              }

              netspeed.appendChild(li);
           }

           if(histDoc.getElementsByTagName("NetSpeed").item(0) != null){
              //Replacing old NetSpeed by the new one
              runstatistics.replaceChild(netspeed,oldNetSpeed);
           }
           else{
              runstatistics.appendChild(netspeed);
           }

           try{
           // Writting the changes to historical file
           Transformer transformer = TransformerFactory.newInstance().newTransformer();
           transformer.setOutputProperty(OutputKeys.INDENT, "yes");
           transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(2));

           //Initializing StreamResult with object to save in the file
           StreamResult result = new StreamResult(histFile);
           DOMSource source = new DOMSource(histDoc);
           transformer.transform(source, result);

           } catch (TransformerConfigurationException e) {
           } catch (TransformerException e) {
          }
        }

}

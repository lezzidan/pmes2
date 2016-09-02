package es.bsc.pmes.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by scorella on 8/5/16.
 */
public class Job {
    private String id;
    private JobStatus status;
    private ArrayList<Resource> resources;
    private User user;
    private ArrayList<String> dataIn;
    private ArrayList<String> dataOut;
    private String cmd;
    private JobDefinition jobDef;

    private static final Logger logger = LogManager.getLogger(Job.class.getName());

    public Job() {
        this.id = UUID.randomUUID().toString();
        this.status = JobStatus.PENDING;
        this.resources = new ArrayList<Resource>();
        this.user = null;
        this.dataIn = null;
        this.dataOut = null;
        this.cmd = "";
        this.jobDef = null;
    }

    public void run(){
        // TODO
    }

    public void stop(){
        // TODO
    }

    public void generateProjects(){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);
            Element rootElement = doc.createElement("Project");
            doc.appendChild(rootElement);

            Element masterNode = doc.createElement("MasterNode");
            rootElement.appendChild(masterNode);

            Element cloud = doc.createElement("Cloud");
            rootElement.appendChild(cloud);

            Element initialVMs = doc.createElement("InitialVMs");
            initialVMs.appendChild(doc.createTextNode(this.getJobDef().getInitialVMs().toString()));
            cloud.appendChild(initialVMs);

            Element minimumVMs = doc.createElement("MinimumVMs");
            minimumVMs.appendChild(doc.createTextNode(this.getJobDef().getMinimumVMs().toString()));
            cloud.appendChild(minimumVMs);

            Element maximumVMs = doc.createElement("MaximumVMs");
            maximumVMs.appendChild(doc.createTextNode(this.getJobDef().getMaximumVMs().toString()));
            cloud.appendChild(maximumVMs);

            Element cloudProvider = doc.createElement("CloudProvider");
            cloud.appendChild(cloudProvider);
            cloudProvider.setAttribute("Name", "one"); //TODO Parametrizar

            Element limitOfVMs = doc.createElement("LimitOfVMs");
            limitOfVMs.appendChild(doc.createTextNode(this.getJobDef().getLimitVMs().toString()));
            cloudProvider.appendChild(limitOfVMs);

            Element properties = doc.createElement("Properties");
            cloudProvider.appendChild(properties);

            String[] propertyNames = {"auth", "ca-auth", "user-cred", "password", "owner", "jobname", "max-vm-creation-time", "max-connection-errors", "vm_user", "context"}; // TODO revisar si puedo añadir property context
            String[] propertyValue = {"x509", "/etc/grid-security/certificates", this.getUser().getCredentials().get("pem"), this.getUser().getCredentials().get("key"), this.getJobDef().getUser().getUsername(), this.getJobDef().getJobName(), "10", "15", this.getUser().getUsername(), "user_data=\"file://$PWD/tmpfedcloud.login\""}; //TODO parametrizar
            for (int i= 0; i < propertyNames.length; i++) {
                Element property = doc.createElement("Property");
                properties.appendChild(property);

                Element name = doc.createElement("Name");
                name.appendChild(doc.createTextNode(propertyNames[i]));
                property.appendChild(name);

                Element value = doc.createElement("Value");
                value.appendChild(doc.createTextNode(propertyValue[i]));
                property.appendChild(value);
            }

            Element images = doc.createElement("Images");
            cloudProvider.appendChild(images);

            Element image = doc.createElement("Image");
            images.appendChild(image);
            image.setAttribute("Name", this.getJobDef().getImg().getImageName());

            Element installDir = doc.createElement("InstallDir");
            installDir.appendChild(doc.createTextNode("/opt/COMPSs/"));
            image.appendChild(installDir);

            Element workingDir = doc.createElement("WorkingDir");
            workingDir.appendChild(doc.createTextNode("/tmp/Worker/"));
            image.appendChild(workingDir);

            Element user = doc.createElement("User");
            user.appendChild(doc.createTextNode(this.getUser().getUsername()));
            image.appendChild(user);

            Element packages = doc.createElement("Package");
            image.appendChild(packages);

            Element sources = doc.createElement("Source");
            sources.appendChild(doc.createTextNode(this.getJobDef().getApp().getSource()));
            packages.appendChild(sources);

            Element target = doc.createElement("Target");
            target.appendChild(doc.createTextNode("/home/user/apps/"));
            packages.appendChild(target);

            Element instanceTypes = doc.createElement("InstanceTypes");
            cloudProvider.appendChild(instanceTypes);

            String[] instanceTypesNames = {"small", "medium", "large", "extra_large"};
            for (String type:instanceTypesNames) {
                Element instanceType = doc.createElement("InstanceType");
                instanceTypes.appendChild(instanceType);
                instanceType.setAttribute("Name",type);
            }

            //Write to xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("/home/bscuser/subversion/projects/pmes2/trunk/src/main/resources/projects.xml")); //TODO complete path

            //DEBUG
            //StreamResult result = new StreamResult(System.out);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);

            logger.trace("projects.xml generated and saved");

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public void generateResources(){
        //TODO
        try {
            //Create Document
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);
            Element rootElement = doc.createElement("ResourcesList");
            doc.appendChild(rootElement);

            Element cloudProvider = doc.createElement("CloudProvider");
            rootElement.appendChild(cloudProvider);
            cloudProvider.setAttribute("Name", "ProviderName"); //TODO parametrizar

            Element endpoint = doc.createElement("Endpoint");
            cloudProvider.appendChild(endpoint);

            Element server = doc.createElement("Server");
            server.appendChild(doc.createTextNode("https://bscgrid20.bsc.es:11443/")); //TODO parametrizar
            endpoint.appendChild(server);

            Element connector = doc.createElement("Connector");
            connector.appendChild(doc.createTextNode("integratedtoolkit.connectors.rocci.ROCCI")); //TODO parametrizar
            endpoint.appendChild(connector);

            Element images = doc.createElement("Images");
            cloudProvider.appendChild(images);

            Element image = doc.createElement("Image");
            image.setAttribute("Name", this.getJobDef().getImg().getImageName());
            images.appendChild(image);

            Element creationTime = doc.createElement("CreationTime");
            creationTime.appendChild(doc.createTextNode("60"));
            image.appendChild(creationTime);

            Element adaptors = doc.createElement("Adaptors");
            image.appendChild(adaptors);

            Element adaptor = doc.createElement("Adaptor");
            adaptor.setAttribute("Name", "integratedtoolkit.nio.master.NIOAdaptor");
            adaptors.appendChild(adaptor);

            Element submissionSystem = doc.createElement("SubmissionSystem");
            adaptor.appendChild(submissionSystem);

            Element interactive = doc.createElement("Interactive");
            submissionSystem.appendChild(interactive);

            Element ports = doc.createElement("Ports");
            adaptor.appendChild(ports);

            Element minport = doc.createElement("MinPort");
            minport.appendChild(doc.createTextNode("43100")); //TODO parametrizar
            ports.appendChild(minport);

            Element maxport = doc.createElement("MaxPort");
            maxport.appendChild(doc.createTextNode("43110")); //TODO parametrizar
            ports.appendChild(maxport);

            Element instanceTypes = doc.createElement("InstanceTypes");
            cloudProvider.appendChild(instanceTypes);

            String[] instanceTypesNames = {"small", "medium", "large", "extra_large"};
            String[] processorName = {"Processor1", "Processor1", "Processor1", "Processor1"}; //TODO parametrizar
            String[] computingUnitsValue = {"1", "4", "8", "16"}; //TODO parametrizar
            String[] priceValue = {"0.085", "0.212", "0.34", "0.68"}; //TODO parametrizar
            for (int i = 0; i < instanceTypesNames.length; i++) {
                Element instanceType = doc.createElement("InstanceType");
                instanceTypes.appendChild(instanceType);
                instanceType.setAttribute("Name",instanceTypesNames[i]);

                Element processor = doc.createElement("Processor");
                instanceType.appendChild(processor);
                processor.setAttribute("Name", processorName[i]);

                Element computingUnits = doc.createElement("ComputingUnits");
                processor.appendChild(computingUnits);
                computingUnits.appendChild(doc.createTextNode(computingUnitsValue[i]));

                Element price = doc.createElement("Price");
                instanceType.appendChild(price);

                Element timeUnit = doc.createElement("TimeUnit");
                price.appendChild(timeUnit);
                timeUnit.appendChild(doc.createTextNode("1"));

                Element pricePerUnit = doc.createElement("PricePerUnit");
                price.appendChild(pricePerUnit);
                pricePerUnit.appendChild(doc.createTextNode(priceValue[i]));
            }

            //Write to xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("/home/bscuser/subversion/projects/pmes2/trunk/src/main/resources/resources.xml")); //TODO complete path

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);

            logger.trace("resources.xml generated and saved");

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /** GETTERS AND SETTERS*/
    public String getId() {
        return id;
    }

    public String getStatus() {
        return status.toString();
    }

    public void setStatus(String status) {
        this.status = JobStatus.valueOf(status);
    }

    public Resource getResource(Integer idx) {
        return resources.get(idx);
    }

    public void addResource(Resource resource) {
        this.resources.add(resource);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ArrayList<String> getDataIn() {
        return dataIn;
    }

    public void setDataIn(ArrayList<String> dataIn) {
        this.dataIn = dataIn;
    }

    public ArrayList<String> getDataOut() {
        return dataOut;
    }

    public void setDataOut(ArrayList<String> dataOut) {
        this.dataOut = dataOut;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setJobDef(JobDefinition jobDef) {
        this.jobDef = jobDef;
    }

    public JobDefinition getJobDef() {
        return jobDef;
    }
}

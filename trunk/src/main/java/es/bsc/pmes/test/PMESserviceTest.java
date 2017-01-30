package es.bsc.pmes.test;

import es.bsc.pmes.api.PMESclient;
import es.bsc.pmes.service.PMESservice;
import es.bsc.pmes.types.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Endpoint;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by scorella on 8/22/16.
 */
public class PMESserviceTest {
    /*public static PMESclient client;

    public static void testCreateActivity(){
        // java -classpath /home/user/compss-connectors-commons.jar:/home/user/compss-connectors-rocci.jar:/home/user/pmes.jar es.bsc.pmes.test.PMESserviceTest
        ArrayList<JobDefinition> jobDefinitions = new ArrayList<>();

        App app = new App("sleep");
        //Image img = new Image("uuid_pmestestingocci_68", "small");
        Image img = new Image("uuid_testcompss14_82", "small");
        User usr = new User("scorella");
        HashMap<String, String> credentials = new HashMap<>();
        credentials.put("key", "~/certs/test/scorella_test.key");
        credentials.put("pem", "~/certs/test/scorella_test.pem");
        usr.setCredentials(credentials);

        JobDefinition job = new JobDefinition();
        job.setApp(app);
        job.setJobName("test");
        job.setImg(img);
        job.setUser(usr);
        job.setInputPath("");
        job.setOutputPath("");
        job.setWallTime(10);
        job.setNumNodes(1);
        job.setCores(16);
        job.setMemory(new Float(1.0));
        job.setCompss_flags(null);

        jobDefinitions.add(job);
        ArrayList<String> jobids = client.createActivity(jobDefinitions);
        for (String id:jobids
             ) {
            System.out.println("Job submitted "+id);
        }
    }

    public static void testGenerateProjects() {
        COMPSsJob jobTest = new COMPSsJob();
        jobTest.generateProjects();
    }

    public static void  testGenerateResources() {
        COMPSsJob jobTest = new COMPSsJob();
        jobTest.generateResources();
    }*/

    public static void main(String[] args){
        System.out.println("Testing app");

        /*client = new PMESclient("http://localhost:9998/");
        SystemStatus systemStatus = client.getSystemStatus();
        ArrayList<Host> cluster = systemStatus.getCluster();
        System.out.println(cluster.size());
        System.out.println(cluster.get(0).getName());

        testCreateActivity();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testCreateActivity();*/

        File fXML = new File("/home/bscuser/subversion/projects/pmes2/trunk/src/main/resources/config.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = null;
        try {
            doc = dBuilder.parse(fXML);
        } catch (SAXException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        doc.getDocumentElement().normalize();
        System.out.println("Root element :"+ doc.getDocumentElement().getNodeName());
        System.out.println(doc.getDocumentElement().getChildNodes().getLength());
        for (int i = 0; i < doc.getDocumentElement().getChildNodes().getLength(); i++){
            Node c = doc.getDocumentElement().getChildNodes().item(i);
            System.out.println(c.getNodeName());
            System.out.println(c.getTextContent());

        }
        System.out.println(doc.getDocumentElement().getElementsByTagName("logLevel").item(0).getTextContent());
        System.out.println(doc.getDocumentElement().getElementsByTagName("logPath").item(0).getTextContent());
        System.out.println(doc.getDocumentElement().getElementsByTagName("ca-path").item(0).getTextContent());
        System.out.println(doc.getDocumentElement().getElementsByTagName("auth").item(0).getTextContent());
        System.out.println(doc.getDocumentElement().getElementsByTagName("endPointROCCI").item(0).getTextContent());
        System.out.println(doc.getDocumentElement().getElementsByTagName("providerName").item(0).getAttributes().getNamedItem("Name").getTextContent());

        ArrayList<String> auth_keys = new ArrayList<>();
        NodeList keys = doc.getElementsByTagName("key");
        for (int i = 0; i < keys.getLength(); i++){
            Node node = keys.item(i);
            String key = node.getTextContent();
            System.out.println(key);

        }
    }

}

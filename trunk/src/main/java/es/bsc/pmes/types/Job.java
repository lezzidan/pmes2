package es.bsc.pmes.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

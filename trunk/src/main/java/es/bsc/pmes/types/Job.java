package es.bsc.pmes.types;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by scorella on 8/5/16.
 */
public class Job {
    private String id;
    private JobStatus status;
    private Resource resource;
    private User user;
    private ArrayList<String> dataIn;
    private ArrayList<String> dataOut;

    public Job() {
        this.id = UUID.randomUUID().toString();
        this.status = JobStatus.PENDING;
        this.resource = null;
        this.user = null;
        this.dataIn = null;
        this.dataOut = null;
    }

    public void run(){
        // TODO
    }

    public void stop(){
        // TODO
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

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
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
}

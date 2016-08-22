package es.bsc.pmes.managers;

import es.bsc.pmes.types.Job;

import java.util.HashMap;
import java.util.Queue;

/**
 * Created by scorella on 8/5/16.
 */
public class JobManager {
    private HashMap<String, Job> jobs;
    private Queue<Job> pendingJobs;
    private InfraestructureManager im;
    private DataManager dm;

    public JobManager(InfraestructureManager im){
        this.im = im;
    }

    public void enqueueJob(){
        // TODO
    }

    public void nextJob(){
        // TODO
    }

    public void executeJob(){
        // TODO
    }

    public void deleteJob(){
        // TODO
    }

    /** loop */
    public void startScheduler(){
        // TODO
    }

    public void endScheduler(){
        // TODO
    }

    /** GETTERS AND SETTERS*/
    public HashMap<String, Job> getJobs() {
        return jobs;
    }

    public void setJobs(HashMap<String, Job> jobs) {
        this.jobs = jobs;
    }

    public Queue<Job> getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(Queue<Job> pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public InfraestructureManager getIm() {
        return im;
    }

    public void setIm(InfraestructureManager im) {
        this.im = im;
    }

    public DataManager getDm() {
        return dm;
    }

    public void setDm(DataManager dm) {
        this.dm = dm;
    }
}

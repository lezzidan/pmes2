package es.bsc.pmes.managers;

import es.bsc.pmes.types.Job;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by scorella on 8/5/16.
 */
public class JobManager{
    private HashMap<String, Job> jobs;
    private InfraestructureManager im;
    private DataManager dm;
    private SchedulerThread scheduler;

    public JobManager(InfraestructureManager im){
        this.jobs = new HashMap<>();
        this.im = im;
        this.dm = new DataManager();
        this.scheduler = new SchedulerThread();

    }

    public void enqueueJob(Job newJob){
        this.jobs.put(newJob.getId(), newJob);
        this.scheduler.addJob(newJob);
    }

    public void deleteJob(Job job){
        this.scheduler.deleteJob(job);
        Job deletedJob = this.jobs.get(job.getId());
        deletedJob.setStatus("CANCELLED");
    }


    public void startScheduler(){
        this.scheduler.start();
    }


    public void endScheduler(){
        this.scheduler.setStop(Boolean.TRUE);
        // TODO: poner el resto de jobs pending a cancelled
    }

    /** GETTERS AND SETTERS*/
    public HashMap<String, Job> getJobs() {
        return jobs;
    }

    public void setJobs(HashMap<String, Job> jobs) {
        this.jobs = jobs;
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

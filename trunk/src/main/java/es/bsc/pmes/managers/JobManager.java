package es.bsc.pmes.managers;

import es.bsc.pmes.types.Job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * Created by scorella on 8/5/16.
 * Singleton class
 */
public class JobManager{
    private static JobManager jobManager = new JobManager(InfrastructureManager.getInfrastructureManager());
    private HashMap<String, Job> jobs;
    private InfrastructureManager im;
    private DataManager dm;
    private SchedulerThread scheduler;

    private static final Logger logger = LogManager.getLogger(InfrastructureManager.class.getName());


    private JobManager(InfrastructureManager im){
        this.jobs = new HashMap<>();
        this.im = im;
        this.dm = DataManager.getDataManager();
        this.scheduler = SchedulerThread.getScheduler();
        startScheduler();
    }

    public static JobManager getJobManager(){
        return jobManager;
    }

    public void enqueueJob(Job newJob){
        this.jobs.put(newJob.getId(), newJob);
        newJob.setStatus("PENDING");
        this.scheduler.addJob(newJob);
    }

    public void deleteJob(Job job){
        if (job.getStatus().equals("PENDING")){
            this.scheduler.deleteJob(job);
            Job deletedJob = this.jobs.get(job.getId());
            deletedJob.setStatus("CANCELLED");
        }
        else {
            logger.trace("Job not in pending jobs");
        }
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

    public InfrastructureManager getIm() {
        return im;
    }

    public void setIm(InfrastructureManager im) {
        this.im = im;
    }

    public DataManager getDm() {
        return dm;
    }

    public void setDm(DataManager dm) {
        this.dm = dm;
    }
}

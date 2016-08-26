package es.bsc.pmes.managers;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.execution.ExecutionThread;
import es.bsc.pmes.types.Job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by scorella on 8/23/16.
 * Singleton class
 * Always alive Thread
 */

public class SchedulerThread extends Thread{
    private static SchedulerThread scheduler = new SchedulerThread();
    private LinkedList<Job> pendingJobs;
    private Boolean stop = Boolean.FALSE;
    private InfrastructureManager im = InfrastructureManager.getInfrastructureManager();
    private static final Logger logger = LogManager.getLogger(SchedulerThread.class);

    private SchedulerThread(){
        this.pendingJobs = new LinkedList<>();
    }

    public static SchedulerThread getScheduler(){
        return scheduler;
    }

    public void run(){
        while (!this.stop) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(pendingJobs.size());
            if (!pendingJobs.isEmpty()){
                System.out.println(pendingJobs.size());
                Job nextJob = this.nextJob();
                this.executeJob(nextJob);
            }
        }
    }

    public void addJob(Job job){
        this.pendingJobs.add(job);
    }

    public Job nextJob(){
        return this.pendingJobs.poll();
    }

    public void executeJob(Job job){
        // Create Resource
        // ** configure Resource Petition
        logger.trace("Configuring Job " + job.getId());
        // Configuring Hardware
        HardwareDescription hd = new HardwareDescription();
        hd.setMemorySize(job.getJobDef().getMemory());
        hd.setTotalComputingUnits(job.getJobDef().getCores()*job.getJobDef().getNumNodes());

        // Configure software
        SoftwareDescription sd = new SoftwareDescription();
        sd.setImageType(job.getJobDef().getImg().getImageType());
        sd.setImageName(job.getJobDef().getImg().getImageName());

        // Configure properties
        HashMap<String, String> prop = this.im.configureResource(job.getJobDef());

        //** create resource
        logger.trace("Creating new Resource");
        String Id = this.im.createResource(hd, sd, prop);
        logger.trace("Resource Id " + Id);
        job.setResource(this.im.getActiveResources().get(Id));

        //StageIn
        logger.trace("Staging in");
        //TODO: stageIN

        //Run job
        ExecutionThread executor = new ExecutionThread(job);
        executor.start();
        System.out.println("waiting");
        try {
            executor.join();
            System.out.println("Execution Finished");
        } catch (Exception e){
            job.setStatus("CANCELLED");
            System.out.println("Interrupted execution");
        }

        //StageOut
        logger.trace("Staging out");
        //TODO: stageOut

        //Destroy Resource
        logger.trace("Deleting Resource");
        job.setStatus("FINISHED");
        //this.im.destroyResource(Id);
    }

    public void deleteJob(Job job){
        this.pendingJobs.remove(job);
    }

    public LinkedList<Job> getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(LinkedList<Job> pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public Boolean getStop() {
        return stop;
    }

    public void setStop(Boolean stop) {
        this.stop = stop;
    }
}

package es.bsc.pmes.managers;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.execution.COMPSsExecutionThread;
import es.bsc.pmes.managers.execution.ExecutionThread;
import es.bsc.pmes.managers.execution.SingleExecutionThread;
import es.bsc.pmes.types.COMPSsJob;
import es.bsc.pmes.types.Job;
import es.bsc.pmes.types.SingleJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
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

    private HashMap<String, ExecutionThread> tasks = new HashMap<>();

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
            //logger.trace("Pending jobs: "+pendingJobs.size());
            if (!pendingJobs.isEmpty()){
                Job nextJob = this.nextJob();
                if (nextJob.getTerminate()){
                    logger.trace("Job cancelled: Job Stop before execution");
                    nextJob.setStatus("CANCELLED");
                } else {
                    nextJob.setStatus("RUNNING");
                    this.executeJob(nextJob);
                }
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
        //Create execution dir
        String path = "/home/pmes/pmes/jobs/"+job.getJobDef().getJobName();
        File dir = new File(path);
        if (!dir.exists()) {
            boolean result = dir.mkdir();
            if (result){
                logger.trace("Job execution directory created: "+path);
            }
        }

        //Run job

        //TODO: Test single or COMPSs Job
        ExecutionThread executor = null;
        if (job instanceof COMPSsJob){
            executor = new COMPSsExecutionThread((COMPSsJob) job);
        } else {
            executor = new SingleExecutionThread((SingleJob) job);
        }
        this.tasks.put(job.getId(), executor);
        executor.start();

        //OLD
        /*COMPSsExecutionThread executor = new COMPSsExecutionThread(job);
        executor.start();
        try {
            logger.trace("Waiting for execution to finish.");
            executor.join();
            if (!job.getStatus().equals("CANCELLED")){
                job.setStatus("FINISHED");
            }
            logger.trace("Execution Finished");
        } catch (Exception e){
            job.setStatus("FAILED");
            logger.trace("Interrupted execution");
        }*/
    }

    public void stopJob(Job job){
        ExecutionThread executionThread = this.tasks.get(job.getId());
        if (executionThread != null) {
            try {
                executionThread.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void deleteJob(Job job){
        Boolean removed = this.pendingJobs.remove(job);
        if (removed) {
            logger.trace("Job cancelled: Job removed from the scheduler");
        } else {
            this.stopJob(job);
        }
    }

    /** GETTERS AND SETTERS*/
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

package es.bsc.pmes.managers;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.execution.COMPSsExecutionThread;
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
        //Run job
        COMPSsExecutionThread executor = new COMPSsExecutionThread(job);
        executor.start();
        System.out.println("waiting");
        try {
            executor.join();
            System.out.println("Execution Finished");
        } catch (Exception e){
            job.setStatus("CANCELLED");
            System.out.println("Interrupted execution");
        }
    }

    public void deleteJob(Job job){
        this.pendingJobs.remove(job);
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

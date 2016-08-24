package es.bsc.pmes.managers;

import es.bsc.pmes.managers.execution.ExecutionThread;
import es.bsc.pmes.types.Job;

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
                //TODO: stageIN
                this.executeJob(nextJob);
                //TODO: stageOut
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
        ExecutionThread executor = new ExecutionThread(job);
        executor.start();
        System.out.println("waiting");
        try {
            executor.join();
            job.setStatus("FINISHED");
            System.out.println("job Finished");
        } catch (Exception e){
            System.out.println("Interrupted execution");
        }
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

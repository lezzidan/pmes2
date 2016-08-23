package es.bsc.pmes.managers;

import es.bsc.pmes.types.ExecutionThread;
import es.bsc.pmes.types.Job;

import java.util.LinkedList;

/**
 * Created by scorella on 8/23/16.
 */
public class SchedulerThread extends Thread{
    private LinkedList<Job> pendingJobs;
    private Boolean stop = Boolean.FALSE;

    public SchedulerThread(){
        this.pendingJobs = new LinkedList<>();
    }

    public void run(){
        while (!this.stop) {
            if (!pendingJobs.isEmpty()){
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
        ExecutionThread executor = new ExecutionThread(job);
        executor.start();
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

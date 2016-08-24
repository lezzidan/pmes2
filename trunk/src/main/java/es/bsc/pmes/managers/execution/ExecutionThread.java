package es.bsc.pmes.managers.execution;

import es.bsc.conn.exceptions.ConnectorException;
import es.bsc.pmes.types.Job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by scorella on 8/23/16.
 */
public class ExecutionThread extends Thread{
    private Job job;

    public ExecutionThread(Job job){
        this.job = job;
    }

    public void run() {
        // TODO
        job.setStatus("RUNNING");
        System.out.println("Executing...");
    }

    public void sshCmdExecution(){
        // TODO
    }
}

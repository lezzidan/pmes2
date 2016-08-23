package es.bsc.pmes.types;

import es.bsc.conn.exceptions.ConnectorException;

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
        System.out.println("Executing..."+this.job.getCmd());
    }

    public void sshCmdExecution(){
        // TODO
    }
}

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

        String resourceAddress = job.getResource().getIp();
        System.out.println(resourceAddress);
        String user = job.getUser().getUsername();
        String address = user+"@"+resourceAddress;
        //String address = "user@"+resourceAddress;
        //String cmd = "pmes-testing@bscgrid20.bsc.es " + "ls -lart";
        String cmd = address + " " + job.getCmd();
        System.out.println(cmd);
        //Falla pq la maquina esta en prolog
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Integer exitValue = executeCommand(cmd);
        System.out.println("Process exit value "+ exitValue);
    }

    public Integer executeCommand(String cmd){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec("ssh "+cmd);
            Integer exitValue = process.waitFor();

            // Output
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()) );
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();

            //Error
            BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()) );
            while ((line = err.readLine()) != null) {
                System.out.println(line);
            }
            err.close();

            return exitValue;
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }
}

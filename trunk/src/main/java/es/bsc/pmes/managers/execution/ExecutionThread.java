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
        job.setStatus("RUNNING");
        System.out.println("Executing...");

        String resourceAddress = job.getResource(0).getIp(); //get master IP
        System.out.println(resourceAddress);

        String user = job.getUser().getUsername();
        String address = user+"@"+resourceAddress;

        String cmd = address + " " + job.getCmd();
        System.out.println(cmd);

        //Wait until vm is ready at login stage
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Execute command
        Integer exitValue = executeCommand(cmd);
        System.out.println("Process exit value "+ exitValue);
    }

    public Integer executeCommand(String cmd){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec("ssh "+cmd);
            Integer exitValue = process.waitFor();

            // Output log
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()) );
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();

            //Error log
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

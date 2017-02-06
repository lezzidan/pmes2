package es.bsc.pmes.managers.execution;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.types.Job;
import es.bsc.pmes.types.SingleJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by scorella on 9/19/16.
 */
public class SingleExecutionThread extends Thread implements ExecutionThread{
    private SingleJob job;
    private InfrastructureManager im = InfrastructureManager.getInfrastructureManager();
    private static final Logger logger = LogManager.getLogger(SingleExecutionThread.class);
    private Process process = null;

    public SingleExecutionThread(SingleJob job){
        this.job = job;
    }

    public void run() {
        executeJob();
    }

    public void cancel() throws Exception {
        if (this.process != null){
            this.process.destroy();
            logger.trace("Job cancelled: Execution stopped");
            this.job.getReport().setJobOutputMessage("Job cancelled: Execution stopped");
            this.job.setStatus("CANCELLED");
        }
        //reset interrupted flag
        //Thread.currentThread().interrupt();
    }

    public void executeJob(){
        // Create Resource
        if (job.getTerminate()){
            job.setStatus("CANCELLED");
            return;
        }
        String Id = createResource();
        logger.trace("Resource created with Id: "+ Id);


        //Configure execution
        String resourceAddress = job.getResource(0).getIp(); //get master IP
        String user = job.getUser().getUsername();
        String address = user+"@"+resourceAddress;
        String source = job.getJobDef().getApp().getSource();
        String target = job.getJobDef().getApp().getTarget();
        HashMap<String, String> args = job.getJobDef().getApp().getArgs();

        // Create command to execute
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("ssh");
        cmd.add(address);
        cmd.add("bash");
        cmd.add("-ic");
        cmd.add(target+"/./"+source);
        for (Object o : args.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            cmd.add((String) pair.getValue());
        }
        String[] command = new String[cmd.size()];
        job.setCmd(cmd.toArray(command));
        logger.trace(Arrays.toString(command));

        //Wait until vm is ready at login stage
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //StageIn
        if (job.getTerminate()){
            //Destroy VM if the user cancel the job.
            logger.trace("Job cancelled: Destroying resource with Id: "+Id);
            destroyResource(Id);
            job.setStatus("CANCELLED");
            return;
        }
        logger.trace("Stage in");
        stageIn();

        //Run job
        if (job.getTerminate()){
            //Destroy VM if the user cancel the job.
            logger.trace("Job cancelled: Destroying resource with Id: "+Id);
            destroyResource(Id);
            job.setStatus("CANCELLED");
            return;
        }

        logger.trace("runnning");
        long startTime = System.currentTimeMillis();
        Integer exitValue = executeCommand(command);
        long endTime = System.currentTimeMillis()-startTime;
        job.getReport().setElapsedTime(String.valueOf(endTime));

        logger.trace("exit code"+ exitValue);
        if (exitValue > 0){
            if (exitValue == 143){
                job.setStatus("CANCELLED");
                // TODO: Si cancelan un job cuando ya se ha ejecutado, traemos datos? salida de compss
            } else {
                job.setStatus("FAILED");
            }
        } else {
            //StageOut
            logger.trace("Stage out");
            stageOut();
            job.setStatus("FINISHED");
        }

        //Destroy Resource
        logger.trace("Destroy resource");
        destroyResource(Id);

        //Create Report
        this.job.createReport();
    }

    public String createResource(){
        // Create Resource
        // ** configure Resource Petition
        logger.trace("Configuring Job " + job.getId());
        // Configuring Hardware
        HardwareDescription hd = new HardwareDescription();
        hd.setMemorySize(job.getJobDef().getMemory());
        hd.setTotalComputingUnits(job.getJobDef().getCores()*job.getJobDef().getNumNodes());
        hd.setImageType(job.getJobDef().getImg().getImageType());
        hd.setImageName(job.getJobDef().getImg().getImageName());

        // Configure software
        SoftwareDescription sd = new SoftwareDescription();


        // Configure properties
        HashMap<String, String> prop = this.im.configureResource(job.getJobDef());

        //** create resource
        logger.trace("Creating new Resource");
        String Id = this.im.createResource(hd, sd, prop);
        logger.trace("Resource Id " + Id);
        job.addResource(this.im.getActiveResources().get(Id));
        return Id;
    }

    public void stageIn(){
        logger.trace("Staging in");
        Integer failedTransfers = this.job.stage(0);
        logger.trace("Failed Transfers: "+failedTransfers.toString());
    }

    public void stageOut(){
        logger.trace("Staging out");
        Integer failedTransfers = this.job.stage(1);
        logger.trace("Failed Transfers: "+failedTransfers.toString());
    }

    public void destroyResource(String Id){
        logger.trace("Deleting Resource");
        this.im.destroyResource(Id);
    }

    public Integer executeCommand(String[] cmd){
        Runtime runtime = Runtime.getRuntime();
        Integer times = 3;
        Integer exitValue = 255;
        Integer i = 0;
        // Adding retry: avoiding fail execution when it takes time to wake up the VM
        while (i < times && exitValue == 255) {
            logger.trace("Round " + String.valueOf(i));

            if (i > 0) {
                //Wait until vm will be ready
                try {
                    Thread.sleep(i*60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                this.process = runtime.exec(cmd);

                BufferedReader in = new BufferedReader(new
                        InputStreamReader(this.process.getInputStream()));

                BufferedReader err = new BufferedReader(new
                        InputStreamReader(this.process.getErrorStream()));

                // Output log
                String outStr = "";
                String line = null;
                while ((line = in.readLine()) != null) {
                    outStr += line;
                }
                in.close();
                logger.trace("out: " + outStr);
                job.getReport().setJobOutputMessage(outStr);

                //Error log
                line = null;
                String errStr = "";
                while ((line = err.readLine()) != null) {
                    errStr += line;
                }
                err.close();
                logger.trace("err: " + errStr);
                job.getReport().setJobErrorMessage(errStr);

                this.process.waitFor();
                exitValue = this.process.exitValue();
                logger.trace("Exit Value "+String.valueOf(exitValue));
                this.job.getReport().setExitValue(exitValue);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i += 1;
        }
        return exitValue;
    }
}


package es.bsc.pmes.managers.execution;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.types.Job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Arrays;

public abstract class AbstractExecutionThread extends Thread implements ExecutionThread{
    private InfrastructureManager im = InfrastructureManager.getInfrastructureManager();
    private Process process = null;
    private static final Logger logger = LogManager.getLogger(AbstractExecutionThread.class);

    public void run() {
        executeJob();
    }

    protected abstract Job getJob();

    public abstract void executeJob();

    @Override
    public void cancel() throws Exception {
        if (this.process != null){
            this.process.destroy();
            logger.trace("Job cancelled: Execution stopped");
            getJob().setStatus("CANCELLED");
        }
    }

    public void stageIn(){
        logger.trace("Staging in");
        Integer failedTransfers = getJob().stage(0);
        logger.trace("Failed Transfers: "+failedTransfers.toString());
    }

    public void stageOut(){
        logger.trace("Staging out");
        Integer failedTransfers = getJob().stage(1);
        logger.trace("Failed Transfers: "+failedTransfers.toString());
    }

    public String createResource(){
        // Create Resource
        // ** configure Resource Petition
        logger.trace("Configuring Job " + getJob().getId());
        // Configuring Hardware
        HardwareDescription hd = new HardwareDescription();
        hd.setMemorySize(getJob().getJobDef().getMemory());
        hd.setTotalComputingUnits(getJob().getJobDef().getCores()*getJob().getJobDef().getNumNodes());
        hd.setImageType(getJob().getJobDef().getImg().getImageType());
        hd.setImageName(getJob().getJobDef().getImg().getImageName());

        // Configure software
        SoftwareDescription sd = new SoftwareDescription();


        // Configure properties
        HashMap<String, String> prop = this.im.configureResource(getJob().getJobDef());

        //** create resource
        logger.trace("Creating new Resource");
        String Id = this.im.createResource(hd, sd, prop);
        logger.trace("Resource Id " + Id);
        getJob().addResource(this.im.getActiveResources().get(Id));
        return Id;
    }

    public void destroyResource(String Id){
        logger.trace("Deleting Resource");
        this.im.destroyResource(Id);
    }

    public Integer executeCommand(String[] cmd){
        // Runtime runtime = Runtime.getRuntime();
        Integer times = 3;
        Integer exitValue = 1;
        Integer i = 0;
        while (i < times && exitValue != 0) {
            logger.trace("Round "+String.valueOf(i));
            logger.trace("Command: " + Arrays.toString(cmd));

            if (i > 0){
                //Wait until vm will be ready
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                this.process = pb.start();
                //this.process = runtime.exec(cmd);

                BufferedReader in = new BufferedReader(new
                        InputStreamReader(this.process.getInputStream()));

                BufferedReader err = new BufferedReader(new
                        InputStreamReader(this.process.getErrorStream()));

                // Output log
                String outStr = "";
                String line = null;
                while ((line = in.readLine()) != null) {
                    outStr += line;
                    outStr += "\n";
                }
                in.close();
                logger.trace("out: " + outStr);
                getJob().getReport().setJobOutputMessage(outStr);

                //Error log
                line = null;
                String errStr = "";
                while ((line = err.readLine()) != null) {
                    errStr += line;
                    errStr += "\n";
                }
                err.close();
                logger.trace("err: " + errStr);
                getJob().getReport().setJobErrorMessage(errStr);

                this.process.waitFor();
                exitValue = this.process.exitValue();
                logger.trace("Exit Value "+String.valueOf(exitValue));
                getJob().getReport().setExitValue(exitValue);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i += 1;
        }
        return exitValue;
    }

    public Boolean stopExecution(String Id, Boolean destroyMachine){
        if (Id == null || Id.equals("-1")){
            getJob().setStatus("FAILED");
            getJob().getReport().setJobErrorMessage("OCCI has failed creating the resource");
            return Boolean.TRUE;
        }
        if ( getJob().getTerminate() ){
            //Destroy VM if the user cancel the job.
            if (destroyMachine) {
                logger.trace("Job cancelled: Destroying resource with Id: " + Id);
                destroyResource(Id);
            }
            getJob().setStatus("CANCELLED");
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}

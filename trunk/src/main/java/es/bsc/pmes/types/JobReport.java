package es.bsc.pmes.types;

/**
 * Created by scorella on 8/5/16.
 */
public class JobReport {
    private JobDefinition jobDefinition;
    private String jobOutputMessage;
    private String jobErrorMessage;
    private JobStatus jobStatus;
    private String elapsedTime;
    private String exitValue;

    public JobReport() {
        this.jobDefinition = null;
        this.jobOutputMessage = "";
        this.jobErrorMessage ="";
        this.jobStatus = JobStatus.valueOf("UNKNOWN");
        this.elapsedTime = "-1";
        this.exitValue = "-1";
    }

    /** GETTERS AND SETTERS*/
    public JobDefinition getJobDefinition() {
        return jobDefinition;
    }

    public void setJobDefinition(JobDefinition jobDefinition) {
        this.jobDefinition = jobDefinition;
    }

    public String getJobOutputMessage() {
        return jobOutputMessage;
    }

    public void setJobOutputMessage(String jobOutputMessage) {
        this.jobOutputMessage = jobOutputMessage;
    }

    public String getJobErrorMessage() {
        return jobErrorMessage;
    }

    public void setJobErrorMessage(String jobErrorMessage) {
        this.jobErrorMessage = jobErrorMessage;
    }

    public String getJobStatus() {
        return jobStatus.toString();
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(String elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public void setExitValue(Integer exitValue) {
        this.exitValue = exitValue.toString();
    }

    public String getExitValue() { return exitValue; }
}

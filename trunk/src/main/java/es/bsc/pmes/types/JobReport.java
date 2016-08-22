package es.bsc.pmes.types;

/**
 * Created by scorella on 8/5/16.
 */
public class JobReport {
    private JobDefinition jobDefinition;
    private String jobOutputMessage;
    private String jobErrorMessage;
    private String jobStatus;
    private String elapsedTime;

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
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(String elapsedTime) {
        this.elapsedTime = elapsedTime;
    }
}

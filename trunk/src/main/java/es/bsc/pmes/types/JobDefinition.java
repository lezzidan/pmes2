package es.bsc.pmes.types;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by scorella on 8/5/16.
 */
public class JobDefinition {
    private String id;
    private String jobName;
    private App app;
    private Image img;
    private User user;
    private String inputPath;
    private String outputPath;
    private Integer wallTime;
    private Integer numNodes;
    private Integer cores;
    private Integer memory;
    private HashMap<String, String> compss_flags;

    public JobDefinition() {
        this.id = UUID.randomUUID().toString();
        this.jobName = "";
        this.app = null;
        this.img = null;
        this.user = null;
        this.inputPath = "";
        this.outputPath = "";
        this.wallTime = -1;
        this.numNodes = -1;
        this.cores = -1;
        this.memory = -1;
        this.compss_flags = null;
    }

    /** GETTERS AND SETTERS*/
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public Image getImg() {
        return img;
    }

    public void setImg(Image img) {
        this.img = img;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public Integer getWallTime() {
        return wallTime;
    }

    public void setWallTime(Integer wallTime) {
        this.wallTime = wallTime;
    }

    public Integer getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(Integer numNodes) {
        this.numNodes = numNodes;
    }

    public Integer getCores() {
        return cores;
    }

    public void setCores(Integer cores) {
        this.cores = cores;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public HashMap<String, String> getCompss_flags() {
        return compss_flags;
    }

    public void setCompss_flags(HashMap<String, String> compss_flags) {
        this.compss_flags = compss_flags;
    }
}

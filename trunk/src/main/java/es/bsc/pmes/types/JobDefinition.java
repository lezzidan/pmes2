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
    private Float memory;
    private Float disk;
    private HashMap<String, String> compss_flags;
    private Integer initialVMs;
    private Integer minimumVMs;
    private Integer maximumVMs;
    private Integer limitVMs;


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
        this.memory = new Float(-1.0);
        this.disk = new Float(-1.0);
        this.compss_flags = null;
        this.initialVMs = 0;
        this.minimumVMs = 0;
        this.maximumVMs = 1;
        this.limitVMs = 1;
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

    public Float getMemory() {
        return memory;
    }

    public void setMemory(Float memory) {
        this.memory = memory;
    }

    public HashMap<String, String> getCompss_flags() {
        return compss_flags;
    }

    public void setCompss_flags(HashMap<String, String> compss_flags) {
        this.compss_flags = compss_flags;
    }

    public Integer getInitialVMs() {
        return initialVMs;
    }

    public void setInitialVMs(Integer initialVMs) {
        this.initialVMs = initialVMs;
    }

    public Integer getMinimumVMs() {
        return minimumVMs;
    }

    public void setMinimumVMs(Integer minimumVMs) {
        this.minimumVMs = minimumVMs;
    }

    public Integer getMaximumVMs() {
        return maximumVMs;
    }

    public void setMaximumVMs(Integer maximumVMs) {
        this.maximumVMs = maximumVMs;
    }


    public Integer getLimitVMs() {
        return limitVMs;
    }

    public void setLimitVMs(Integer limitVMs) {
        this.limitVMs = limitVMs;
    }

    public Float getDisk() {
        return disk;
    }

    public void setDisk(Float disk) {
        this.disk = disk;
    }
}

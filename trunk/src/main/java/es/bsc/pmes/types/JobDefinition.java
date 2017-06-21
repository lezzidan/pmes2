package es.bsc.pmes.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by scorella on 8/5/16.
 */
public class JobDefinition {
    private String id;
    private String jobName;
    private Integer wallTime;
    private Integer minimumVMs;
    private Integer maximumVMs;
    private Integer limitVMs;
    private Integer initialVMs;
    private Float memory;
    private Integer cores;
    private Float disk;
    private ArrayList<String> inputPaths;
    private ArrayList<String> outputPaths;
    private String infrastructure;
    private ArrayList<MountPoint> mountPoints;
    private Integer numNodes;
    private User user;
    private Image img;
    private App app;
    private HashMap<String, String> compss_flags;
    
    
    public JobDefinition() {
        this.id = UUID.randomUUID().toString();
        this.jobName = "";
        this.app = null;
        this.img = null;
        this.user = null;
        this.inputPaths = new ArrayList<String>();
        this.outputPaths = new ArrayList<String>();
        this.wallTime = -1;
        this.numNodes = -1;
        this.cores = -1;
        this.memory = (float) -1.0;
        this.disk = (float) -1.0;
        this.compss_flags = new HashMap<String, String>();
        this.initialVMs = 0;
        this.minimumVMs = 0;
        this.maximumVMs = 1;
        this.limitVMs = 1;
        this.infrastructure ="";
        this.mountPoints = new ArrayList<MountPoint>();
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


    public ArrayList<String> getInputPaths() {
        return inputPaths;
    }

    public void setInputPaths(ArrayList<String> inputPaths) {
        this.inputPaths = inputPaths;
    }

    public ArrayList<String> getOutputPaths() {
        return outputPaths;
    }

    public void setOutputPaths(ArrayList<String> outputPaths) {
        this.outputPaths = outputPaths;
    }

    public String getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(String infrastructure) {
        this.infrastructure = infrastructure;
    }

    public ArrayList<MountPoint> getMountPoints() {
        return mountPoints;
    }

    public void setMountPoints(ArrayList<MountPoint> mountPoints) {
        this.mountPoints = mountPoints;
    }
}

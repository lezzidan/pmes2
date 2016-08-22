package es.bsc.pmes.types;

/**
 * Created by scorella on 8/5/16.
 */
public class SystemStatus {
    private Integer usedCores;
    private Integer totalCores;
    private Float usedMemory;
    private Float totalMemory;

    /** GETTERS AND SETTERS*/
    public Integer getUsedCores() {
        return usedCores;
    }

    public void setUsedCores(Integer usedCores) {
        this.usedCores = usedCores;
    }

    public Integer getTotalCores() {
        return totalCores;
    }

    public void setTotalCores(Integer totalCores) {
        this.totalCores = totalCores;
    }

    public Float getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(Float usedMemory) {
        this.usedMemory = usedMemory;
    }

    public Float getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(Float totalMemory) {
        this.totalMemory = totalMemory;
    }
}

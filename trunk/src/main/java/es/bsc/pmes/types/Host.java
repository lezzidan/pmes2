package es.bsc.pmes.types;

/**
 * Created by scorella on 8/29/16.
 */
public class Host {
    private String name;
    private Integer usedCores;
    private Integer totalCores;
    private Float usedMemory;
    private Float totalMemory;

    public Host(String name, Integer totalCores, Float totalMemory) {
        this.name = name;
        this.usedCores = 0;
        this.totalCores = totalCores;
        this.usedMemory = new Float(0);
        this.totalMemory = totalMemory;
    }

    /** GETTERS AND SETTERS*/
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

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

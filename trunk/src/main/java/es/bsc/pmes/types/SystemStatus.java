package es.bsc.pmes.types;

import java.util.ArrayList;

/**
 * Created by scorella on 8/5/16.
 */
public class SystemStatus {
    private ArrayList<Host> cluster;

    public SystemStatus(){
        this.cluster = new ArrayList<Host>();
    }
    public void addHost(Host host){
        cluster.add(host);
    }

    public void deleteHost(Host host){
        cluster.remove(host);
    }

    public void update(Host host, Integer used_cpu, Float used_mem){
        int i = cluster.indexOf(host);
        Integer valueCpu = cluster.get(i).getUsedCores();
        Float valueMem = cluster.get(i).getUsedMemory();
        cluster.get(i).setUsedCores(valueCpu+used_cpu);
        cluster.get(i).setUsedMemory(valueMem+used_mem);
    }

    public String print(){
        String s = new String();
        s += "___________________________________________";
        s += "Cluster: \n";
        for (int i=0; i < this.cluster.size(); i++){
            s += "Host "+i+" Cpu: "+cluster.get(i).getUsedCores()+" Memory: "+cluster.get(i).getUsedMemory();
            s += "\n";
        }
        s += "___________________________________________";
        return s;
    }

    /** GETTERS AND SETTERS*/
    public ArrayList<Host> getCluster() {
        return cluster;
    }

    public void setCluster(ArrayList<Host> cluster) {
        this.cluster = cluster;
    }
}

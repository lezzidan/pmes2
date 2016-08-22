package es.bsc.pmes.types;

import es.bsc.conn.types.VirtualResource;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by scorella on 8/5/16.
 * Podemos quitar esta clase y usar la clase virtualResource de los connectores
 */
public class Resource {
    private String ip;
    private HashMap<String, String> context;
    private VirtualResource vr;

    public Resource() {
        // TODO
    }

    public Resource(String ip,HashMap<String, String> context, VirtualResource vr) {
        this.ip = ip;
        this.context = context;
        this.vr = vr;
    }

    public void configure(){
        // TODO
    }

    public void start(){
        // TODO
    }

    public void stop(){
        // TODO
    }

    /** GETTERS AND SETTERS*/
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public HashMap<String, String> getContext() {
        return context;
    }

    public void setContext(HashMap<String, String> context) {
        this.context = context;
    }

    public VirtualResource getVr() {
        return vr;
    }

    public void setVr(VirtualResource vr) {
        this.vr = vr;
    }
}

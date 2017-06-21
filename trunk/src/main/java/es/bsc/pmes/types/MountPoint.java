package es.bsc.pmes.types;

import java.util.HashMap;

/**
 *
 * @author fconejer
 */
public class MountPoint {
    
    private String target;
    private String device;
    private String permissions;

    public MountPoint() {
        this.target = "";
        this.device = "";
        this.permissions = "";
    }
    
    public MountPoint(String target, String device, String permissions) {
        this.target = target;
        this.device = device;
        this.permissions = permissions;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}

package integratedtoolkit.util.ur;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.gridlab.gat.io.File;

public final class FileTransferUsageRecord {

    //String name;
    String globalJobId;
    String localJobId;
    String fileName;
    boolean primaryHost;
    String status;
    String source;
    String destination;
    String localUserName;
    String hostName;
    long size = 0;
    long taskStartTime;
    long taskEndTime;
    int processId;
    List<Integer> groupIds;

    public FileTransferUsageRecord(String appName, String slaId, String toFile, String host, File file, String fileSource, String fileDestination) {

        globalJobId = slaId;
        fileName = toFile;
        primaryHost = Boolean.getBoolean(host);
        status = "Started";

        // File information
        size = file.length();
        //name = file.getName();
        
        source = fileSource;
        destination = fileDestination;

        getSystemProperties();

        // Build localJobId
        StringBuilder sb = new StringBuilder(Integer.toString(processId));
        localJobId = sb.append(".").append(appName).append(".").append(hostName).toString();

        // As close as we can get to the start time of the users task
        taskStartTime = System.currentTimeMillis();
    }

    public final void stop(List<Integer> groupIds) {
        taskEndTime = System.currentTimeMillis();
        this.groupIds = groupIds;
        status = "Completed";
        toDisk(toRecord());
    }

    private String toRecord() {

        StringBuilder ur = new StringBuilder();
        ur.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        ur.append("<UsageRecord xmlns=\"http://schema.ogf.org/urf/2003/09/urf\"");
        ur.append(" xmlns:urwg=\"http://schema.ogf.org/urf/2003/09/urf\"");
        ur.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        ur.append(" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"");
        ur.append(" xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg/urwg-schema.09.02.xsd\">");

        ur.append("<RecordIdentity recordId=\"\" createDate=\"" + inIso8601(taskEndTime) + "\"/>");
        ur.append("<JobIdentity>");
        ur.append("<LocalJobId>" + localJobId + "</LocalJobId>");
        ur.append("</JobIdentity>");

        ur.append("<UserIdentity><LocalUserId>" + localUserName + "</LocalUserId></UserIdentity>");

        ur.append("<Network metric=\"total\" units=\"bytes\">" + size + "</Network>");
        ur.append("<WallDuration>" + inDuration(taskEndTime - taskStartTime) + "</WallDuration>");
        ur.append("<StartTime>" + inIso8601(taskStartTime) + "</StartTime>");
        ur.append("<EndTime>" + inIso8601(taskEndTime) + "</EndTime>");
        ur.append("<Status>" + status + "</Status>");
        //ur.append("<Resource description=\"fileName\">" + name + "</Resource>");
        ur.append("<Resource description=\"logicalFileSource\">" + source + "</Resource>");
        ur.append("<Resource description=\"fileDestination\">" + destination + "</Resource>");
        ur.append("<Resource description=\"groupIds\">");
        for (Integer groupId : groupIds) {
        	ur.append(groupId + ",");
        }
        ur.deleteCharAt(ur.length() - 1);
        ur.append("</Resource>");

        ur.append("</UsageRecord>");
        return ur.toString();
    }

    /**
     * Dump the UR to the fileName given.
     *
     */
    private void toDisk(String record) {

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            out.write(record);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String inIso8601(long time) {

        Calendar cal = getCalendar(time);
        StringBuilder sb = new StringBuilder();
        sb.append(padNumber(cal.get(Calendar.YEAR), 4));
        sb.append("-");
        sb.append(padNumber(cal.get(Calendar.MONTH) + 1, 2));
        sb.append("-");
        sb.append(padNumber(cal.get(Calendar.DATE), 2));
        sb.append("T");
        sb.append(padNumber(cal.get(Calendar.HOUR_OF_DAY), 2));
        sb.append(":");
        sb.append(padNumber(cal.get(Calendar.MINUTE), 2));
        sb.append(":");
        sb.append(padNumber(cal.get(Calendar.SECOND), 2));
        sb.append(".");
        sb.append(padNumber(cal.get(Calendar.MILLISECOND), 3));
        sb.append("Z");
        return sb.toString();
    }

    private String padNumber(long value, int digits) {
        String s = Long.toString(value);
        for (int i = 0; i < (digits - s.length()); i++) {
            s = "0" + s;
        }
        return s;
    }

    private String inDuration(long duration) {

        Calendar cal = getCalendar(duration);

        int months = cal.get(Calendar.MONTH);
        int days = cal.get(Calendar.DAY_OF_MONTH) - 1;
        int hours = cal.get(Calendar.HOUR);
        int minutes = cal.get(Calendar.MINUTE);
        int seconds = cal.get(Calendar.SECOND);
        int mseconds = cal.get(Calendar.MILLISECOND);

        // P nMnDTnHnMnS
        StringBuilder sb = new StringBuilder("P");

        if (months > 0) {
            sb.append(months).append("M");
        }

        if (days > 0) {
            sb.append(days).append("D");
        }

        sb.append("T");

        if (hours > 0) {
            sb.append(hours).append("H");
        }

        if (minutes > 0) {
            sb.append(minutes).append("M");
        }

        sb.append(seconds).append(".").append(mseconds).append("S");
        return sb.toString();
    }

    private Calendar getCalendar(long time) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date(time));
        return cal;
    }

    private void getSystemProperties() {
        
        localUserName = System.getProperty("user.name");

        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostName = "";
        }

    }
}

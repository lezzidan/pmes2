package integratedtoolkit.util.ur;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class UsageRecord {

    /**
     *
     * Note that:
     *
     * 1) RecordId is not set by the UR class assume that the RUS service does this
     * 2) GlobalJobId is the SLA id (i.e. the id assigned by the metascheduler)
     *
     */
    static boolean primaryHost;
    static int processId;
    static int processors;  // Number of processors available
    static long jvmStartTime;  // When the JVM was started
    static long taskStartTime;
    static long taskEndTime;
    static long startCpu;
    static long endCpu;
    static long startUser;
    static long endUser;
    static String status;
    static String name;
    static String cwd;
    static String fileName;
    static String transferId;
    static String hostName;
    static String localJobId;
    static String localUserName;
    static String os;
    static String osVersion;
    static String procArch;
    static String globalJobId;
    static String jvmVersion;
    static String jvmVendor;
    final static Runtime runtime = Runtime.getRuntime();
    final static RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
    final static ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();

    public final static void start(String appName, String slaId, String toFile, String host, String tId) {

        name = appName;
        globalJobId = slaId;
        fileName = toFile;
        primaryHost = Boolean.getBoolean(host);
        transferId = tId;
        status = "Started";

        getSystemParameters();

        // Build localJobId
        StringBuilder sb = new StringBuilder(Integer.toString(processId));
        localJobId = sb.append(".").append(appName).append(".").append(hostName).toString();

        // Get when the JVM was started
        jvmStartTime = rmxb.getStartTime();

        // Start the counter on the CPU time
        if (tmxb.isCurrentThreadCpuTimeSupported()) {
        	// Measuring cpu time is supported by this JVM / OS
        	startCpu = tmxb.getCurrentThreadCpuTime();
        	startUser = tmxb.getCurrentThreadUserTime();
        }
        else {
        	startCpu = 0;
            startUser = 0;
        }

        // As close as we can get to the start time of the users task
        taskStartTime = System.currentTimeMillis();
    }

    public final static void end() {
        taskEndTime = System.currentTimeMillis();
        
        if (tmxb.isCurrentThreadCpuTimeSupported()) {
        	// Measuring cpu time is supported by this JVM / OS
        	endUser = tmxb.getCurrentThreadUserTime();
        	endCpu = tmxb.getCurrentThreadCpuTime();
        }
        else {
        	endCpu = 0;
            endUser = 0;
        }
        
        status = "Completed";
        toDisk(toRecord());
    }

    private static String toRecord() {

        long jvmUpTime = rmxb.getUptime();
        long jvmEndTime = jvmStartTime + jvmUpTime;

        StringBuilder ur = new StringBuilder();
        ur.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        ur.append("<UsageRecord xmlns=\"http://schema.ogf.org/urf/2003/09/urf\"");
        ur.append(" xmlns:urwg=\"http://schema.ogf.org/urf/2003/09/urf\"");
        ur.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        ur.append(" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"");
        ur.append(" xsi:schemaLocation=\"http://www.gridforum.org/2003/ur-wg/urwg-schema.09.02.xsd\">");

        ur.append("<RecordIdentity recordId=\"\" createDate=\"" + inIso8601(jvmEndTime) + "\"/>");
        ur.append("<JobIdentity>");
        ur.append("<GlobalJobId>" + globalJobId + "</GlobalJobId>");
        ur.append("<LocalJobId>" + localJobId + "</LocalJobId>");

        if (processId != 0) {
            ur.append("<ProcessId>" + processId + "</ProcessId>");
        }

        // TODO: This is not likely to appear in the schema
        ur.append("<TransferId>" + transferId + "</TransferId>");
        
        ur.append("</JobIdentity>");
        ur.append("<UserIdentity><LocalUserId>" + localUserName + "</LocalUserId></UserIdentity>");
        ur.append("<Host primary=\"" + primaryHost + "\">" + hostName + "</Host>");
        ur.append("<WallDuration>" + inDuration(jvmUpTime) + "</WallDuration>");
        ur.append("<CpuDuration usageType=\"user\">" + inDuration((endUser - startUser) / 1000000) + "</CpuDuration>");
        ur.append("<CpuDuration usageType=\"system\">" + inDuration((endCpu - startCpu) / 1000000) + "</CpuDuration>");
        ur.append("<StartTime>" + inIso8601(jvmStartTime) + "</StartTime>");
        ur.append("<EndTime>" + inIso8601(jvmEndTime) + "</EndTime>");
        ur.append("<Status>" + status + "</Status>");

        ur.append("<Processors description=\"available\">" + processors + "</Processors>");

        ur.append("<Memory description =\"jvmRuntime\" metric=\"maxMemory\" units=\"b\">" + runtime.maxMemory() + "</Memory>");

        for (MemoryPoolMXBean mmxb : ManagementFactory.getMemoryPoolMXBeans()) {
            String desc = "jvmMemoryPool (" + mmxb.getName() + ")";
            ur.append("<Memory description=\"" + desc + "\" units=\"b\" type=\"" + mmxb.getType().toString() + "\" metric=\"peak\">" + mmxb.getPeakUsage().getUsed() + "</Memory>");
        }

        ur.append("<TimeInstant description=\"jvmStartTime\">" + inIso8601(jvmStartTime) + "</TimeInstant>");
        ur.append("<TimeInstant description=\"taskStartTime\">" + inIso8601(taskStartTime) + "</TimeInstant>");
        ur.append("<TimeInstant description=\"jvmEndTime\">" + inIso8601(jvmEndTime) + "</TimeInstant>");
        ur.append("<TimeInstant description=\"taskEndTime\">" + inIso8601(taskEndTime) + "</TimeInstant>");

        ur.append("<TimeDuration description=\"taskDuration\">" + inDuration(taskEndTime - taskStartTime) + "</TimeDuration>");
        ur.append("<TimeDuration description=\"jvmDuration\">" + inDuration(jvmUpTime) + "</TimeDuration>");

        ur.append("<Resource description=\"executableName\">" + name + "</Resource>");
        ur.append("<Resource description=\"cwd\">" + cwd + "</Resource>");
        ur.append("<Resource description=\"slaId\">" + globalJobId + "</Resource>");
        ur.append("<Resource description=\"processorArchitecture\">" + procArch + "</Resource>");
        ur.append("<Resource description=\"os\">" + os + "</Resource>");
        ur.append("<Resource description=\"osVersion\">" + osVersion + "</Resource>");
        ur.append("<Resource description=\"jvmVersion\">" + jvmVersion + "</Resource>");
        ur.append("<Resource description=\"jvmVendor\">" + jvmVendor + "</Resource>");
        ur.append("</UsageRecord>");
        return ur.toString();
    }

    /**
     * Dump the UR to the fileName given.
     *
     */
    protected final static void toDisk(String record) {

        /*if (fileName == null || fileName.length() == 0) {
            fileName = name + ".ur.xml";
        }*/
    	
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            out.write(record);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected final static String inIso8601(long time) {

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

    private static String padNumber(long value, int digits) {
        String s = Long.toString(value);
        for (int i = 0; i < (digits - s.length()); i++) {
            s = "0" + s;
        }
        return s;
    }

    protected final static String inDuration(long duration) {

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

    private static Calendar getCalendar(long time) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date(time));
        return cal;
    }

    protected final static void getSystemParameters() {

        Properties pr = System.getProperties();

        cwd = pr.getProperty("user.dir");
        localUserName = pr.getProperty("user.name");
        os = pr.getProperty("os.name");
        osVersion = pr.getProperty("os.version");
        procArch = pr.getProperty("os.arch");
        processors = runtime.availableProcessors();
        jvmVersion = pr.getProperty("java.vm.version");
        jvmVendor = pr.getProperty("java.vm.vendor");

        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostName = "";
        }

        if ("Linux".equals(os)) {
            processId = getProcessIdUnix();
        } else {
            processId = 0;
        }
    }

    private static int getProcessIdUnix() {
        byte[] bo = new byte[100];
        String[] cmd = {"bash", "-c", "echo $PPID"};
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.getInputStream().read(bo);
        } catch (IOException e) {
            return 0;
        }

        return Integer.parseInt((new String(bo)).trim());
    }
}

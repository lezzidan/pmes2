package integratedtoolkit.util.ur;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public final class UsageRecordUtils {

    /**
     * Send this file to the RUS server.
     * TODO Spawn another thread if synchronously = true
     *
     * @param synchronously
     */
    public final static void toRus(String record, URL[] rusServers, boolean synchronously) {

        for (URL rus : rusServers) {
            try {
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(rus.toString());
                StringRequestEntity body = new StringRequestEntity(record, "application/ur+xml", null);
                method.setRequestEntity(body);
                int statusCode = client.executeMethod(method);
                if (statusCode != -1) {
                    String contents = method.getResponseBodyAsString();
                    method.releaseConnection();
                    System.out.println(contents);
                } else {
                    return;
                }
            } catch (MalformedURLException e) {
                System.out.println("MalformedURLException");
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                System.out.println("UnsupportedEncodingException");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("IOException");
                e.printStackTrace();
            }
        }

    // if we get here the us was never sent...
    // should raise an exception or something...
    }
}

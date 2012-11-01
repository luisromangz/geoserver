package org.geoserver.monitor.transport;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.geoserver.monitor.RequestData;

/**
 * Meant to just be a proof of concept for sending a post out
 *
 */
public class HttpMessageTransport implements MessageTransport {

    private final String url;

    private final String apiKey;

    public HttpMessageTransport(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }

    @Override
    public void transport(RequestData data) {
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        postMethod.addParameter("api", apiKey);
        // TODO serialize to json?
        String payload = serializeToJson(data);
        postMethod.addParameter("payload", payload);
        // TODO retry handler?
        try {
            int statusCode = client.executeMethod(postMethod);
            // TODO error handling
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("not ok: " + postMethod.getStatusLine());
            }
        } catch (HttpException e) {
            // TODO error handling
            e.printStackTrace();
        } catch (IOException e) {
            // TODO error handling
            e.printStackTrace();
        } finally {
            postMethod.releaseConnection();
        }
    }

    private String serializeToJson(RequestData data) {
        // TODO just a stub for now
        return "{ \"id\":" + data.internalid + "}";
    }

    @Override
    public void destroy() {
    }

}

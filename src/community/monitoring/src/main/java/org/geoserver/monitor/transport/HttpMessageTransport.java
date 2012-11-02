package org.geoserver.monitor.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.geoserver.monitor.RequestData;

import com.google.common.base.Throwables;

/**
 * Meant to just be a proof of concept for sending a post out
 * 
 */
public class HttpMessageTransport implements MessageTransport {

    private final String url;

    private final String apiKey;

    private static final Logger LOGGER = Logger.getLogger("org.geoserver.monitor.transport");

    public HttpMessageTransport(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }

    // send request data via http post
    // if sending fails, log failure and just drop message
    @Override
    public void transport(Collection<RequestData> data) {
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);

        // set payload of message
        JSONObject body = new JSONObject();
        JSONArray payload = serializeToJson(data);
        body.element("messages", payload);
        body.element("api", apiKey);
        StringRequestEntity requestEntity = null;
        try {
            requestEntity = new StringRequestEntity(body.toString(), "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Throwables.propagate(e);
        }
        postMethod.setRequestEntity(requestEntity);

        // send message
        try {
            int statusCode = client.executeMethod(postMethod);
            // if we receive a status code saying api key is invalid
            // we might want to signal back to the monitor filter to back off transporting messages
            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.warning("Did not receive ok response: " + statusCode + " from: " + url);
            }
        } catch (HttpException e) {
            logCommunicationError(e);
        } catch (IOException e) {
            logCommunicationError(e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    private void logCommunicationError(Exception e) {
        LOGGER.warning("Error comunicating with: " + url);
        if (LOGGER.isLoggable(Level.INFO)) {
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            e.printStackTrace(writer);
            LOGGER.info(out.toString());
        }
    }

    private JSONArray serializeToJson(Collection<RequestData> data) {
        JSONArray jsonArray = new JSONArray();
        for (RequestData requestData : data) {
            jsonArray.add(serializeToJson(requestData));
        }
        return jsonArray;
    }

    // consolidate request path and query string
    private String buildURL(RequestData requestData) {
        String path = requestData.getPath();
        String queryString = requestData.getQueryString();
        String url = path + (queryString == null ? "" : "?" + queryString);
        return url;
    }

    private JSONObject serializeToJson(RequestData requestData) {
        JSONObject json = new JSONObject();

        json.element("id", requestData.internalid);

        json.element("requestStatus", requestData.getStatus());
        json.element("url", buildURL(requestData));
        json.element("method", requestData.getHttpMethod());

        json.element("responseLength", requestData.getResponseLength());
        json.element("responseContentType", requestData.getResponseContentType());
        json.element("responseStatus", requestData.getResponseStatus());
        json.elementOpt("error", requestData.getErrorMessage());

        json.element("category", requestData.getCategory());
        json.elementOpt("operation", requestData.getOperation());
        json.elementOpt("suboperation", requestData.getSubOperation());
        json.elementOpt("service", requestData.getService());
        json.elementOpt("owsversion", requestData.getOwsVersion());

        json.element("startTimeMillis", requestData.getStartTime().getTime());
        json.element("endTimeMillis", requestData.getEndTime().getTime());

        json.element("remoteAddr", requestData.getRemoteAddr());
        json.elementOpt("remoteUserAgent", requestData.getRemoteUserAgent());
        json.elementOpt("remoteUser", requestData.getRemoteUser());
        json.elementOpt("referer", requestData.getHttpReferer());

        // TODO add remote geoip and address location once post processing is hooked up again
        // country/city and lat/lon

        json.element("serverHost", requestData.getHost());
        json.element("internalHost", requestData.getInternalHost());

        List<String> resources = requestData.getResources();
        if (resources != null && !resources.isEmpty()) {
            JSONArray jsonResources = new JSONArray();
            for (String resource : resources) {
                jsonResources.add(resource);
            }
            json.element("resources", resources);
        }

        return json;
    }

    @Override
    public void destroy() {
    }
}

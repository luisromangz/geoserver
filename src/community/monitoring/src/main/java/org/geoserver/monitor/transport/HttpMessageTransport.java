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
import org.geotools.util.logging.Logging;

import com.google.common.base.Throwables;

/**
 * Meant to just be a proof of concept for sending a post out
 * 
 */
public class HttpMessageTransport implements MessageTransport {

    private final String url;

    private final String apiKey;

    private static final Logger LOGGER = Logging.getLogger(HttpMessageTransport.class);

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

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(body.toString());
        }

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

        json.element("url", buildURL(requestData));
        json.elementOpt("http_referer", requestData.getHttpReferer());

        json.element("request_method", requestData.getHttpMethod());
        json.element("request_length", requestData.getBodyContentLength());
        json.elementOpt("request_content_type", requestData.getBodyContentType());

        json.element("response_status", requestData.getResponseStatus());
        json.element("response_length", requestData.getResponseLength());
        json.element("response_content_type", requestData.getResponseContentType());

        json.element("category", requestData.getCategory().toString());
        json.elementOpt("service", requestData.getService());

        json.elementOpt("operation", requestData.getOperation());
        json.elementOpt("sub_operation", requestData.getSubOperation());
        json.elementOpt("ows_version", requestData.getOwsVersion());

        // start_time is seconds since epoch
        // duration is in milliseconds
        long startMillis = requestData.getStartTime().getTime();
        long endMillis = requestData.getEndTime().getTime();
        json.element("start_time", startMillis / 1000);
        json.element("duration", endMillis - startMillis);

        json.elementOpt("server_host", requestData.getHost());
        json.element("internal_server_host", requestData.getInternalHost());

        json.element("remote_address", requestData.getRemoteAddr());
        json.elementOpt("remote_host", requestData.getRemoteHost());
        json.elementOpt("remote_user_agent", requestData.getRemoteUserAgent());
        json.elementOpt("remote_user", requestData.getRemoteUser());

        // country only gets set if the ip lookup succeeded
        if (requestData.getRemoteCountry() != null) {
            json.element("remote_latitude", requestData.getRemoteLat());
            json.element("remote_longitude", requestData.getRemoteLon());
        }

        json.elementOpt("error", requestData.getErrorMessage());

        List<String> resources = requestData.getResources();
        if (resources != null && !resources.isEmpty()) {
            JSONArray jsonResources = new JSONArray();
            jsonResources.addAll(resources);
            json.element("resources", resources);
        }

        return json;
    }

    @Override
    public void destroy() {
    }
}

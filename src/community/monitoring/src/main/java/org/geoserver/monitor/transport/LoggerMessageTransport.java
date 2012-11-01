package org.geoserver.monitor.transport;

import java.util.Collection;
import java.util.logging.Logger;

import org.geoserver.monitor.RequestData;
import org.geotools.util.logging.Logging;

public class LoggerMessageTransport implements MessageTransport {

    private static Logger LOGGER = Logging.getLogger("org.geoserver.monitor.transport");

    @Override
    public void transport(Collection<RequestData> data) {
        for (RequestData requestData : data) {
            LOGGER.info("Transporting data: " + requestData.internalid);
        }
    }

    @Override
    public void destroy() {
    }

}

package org.geoserver.monitor.transport;

import org.geoserver.monitor.RequestData;

public class NullMessageTransport implements MessageTransport {

    @Override
    public void transport(RequestData data) {
    }

    @Override
    public void destroy() {
    }
}

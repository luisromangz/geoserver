package org.geoserver.monitor.transport;

import java.util.Collection;

import org.geoserver.monitor.RequestData;

public class NullMessageTransport implements MessageTransport {

    @Override
    public void transport(Collection<RequestData> data) {
    }

    @Override
    public void destroy() {
    }
}

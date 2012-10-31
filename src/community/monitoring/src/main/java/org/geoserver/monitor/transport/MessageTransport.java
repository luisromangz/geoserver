package org.geoserver.monitor.transport;

import org.geoserver.monitor.RequestData;

/**
 * Takes request data from monitoring, and pushes it to where it needs to go
 *
 */
public interface MessageTransport {

    /**
     * 
     * @param data request data to transport
     */
    void transport(RequestData data);

    /**
     * Hook to shutdown any threads if started asynchronously
     */
    void destroy();
}

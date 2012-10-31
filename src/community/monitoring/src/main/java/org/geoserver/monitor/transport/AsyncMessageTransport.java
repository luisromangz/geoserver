package org.geoserver.monitor.transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geoserver.monitor.RequestData;

/**
 * Asynchronously delegate to another transport
 * 
 */
public class AsyncMessageTransport implements MessageTransport {

    private final MessageTransport transporter;

    private final ExecutorService executor;

    public AsyncMessageTransport(MessageTransport transport, int threadPoolSize) {
        this.transporter = transport;
        executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public void transport(RequestData data) {
        executor.execute(new AsyncMessageTask(transporter, data));
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }

    private static class AsyncMessageTask implements Runnable {

        private final MessageTransport transporter;

        private final RequestData data;

        private AsyncMessageTask(MessageTransport transport, RequestData data) {
            this.transporter = transport;
            this.data = data;
        }

        @Override
        public void run() {
            transporter.transport(data);
        }
    }
}

package org.geoserver.monitor.transport;

import java.util.Collection;
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

    public AsyncMessageTransport(MessageTransport transporter, int threadPoolSize) {
        this.transporter = transporter;
        executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public void transport(Collection<RequestData> data) {
        executor.execute(new AsyncMessageTask(transporter, data));
    }

    @Override
    public void destroy() {
        executor.shutdown();
        transporter.destroy();
    }

    private static class AsyncMessageTask implements Runnable {

        private final MessageTransport transporter;

        private final Collection<RequestData> data;

        private AsyncMessageTask(MessageTransport transport, Collection<RequestData> data) {
            this.transporter = transport;
            this.data = data;
        }

        @Override
        public void run() {
            transporter.transport(data);
        }
    }
}

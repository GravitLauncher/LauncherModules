package pro.gravit.launchermodules.mdnsclient;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.concurrent.CompletableFuture;

public class MDNSService implements AutoCloseable, ServiceListener {
    private String serviceType;
    private CompletableFuture<String> future = new CompletableFuture<>();

    private transient JmDNS jmDNS;

    public MDNSService(String serviceType) throws IOException {
        this.serviceType = serviceType;
        this.jmDNS = JmDNS.create();
    }

    public CompletableFuture<String> getFuture() {
        return future;
    }

    @Override
    public void close() throws Exception {
        jmDNS.unregisterAllServices();
        jmDNS.close();
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        if(event.getType().equals(serviceType)) {
            event.getDNS().requestServiceInfo(event.getType(), event.getName(), 700);
        }
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {

    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if(event.getType().equals(serviceType)) {
            Inet4Address address = event.getInfo().getInet4Addresses()[0];
            int port = event.getInfo().getPort();
            future.complete(String.format("ws://%s:%d/api", address.getHostAddress(), port));
        }
    }
}

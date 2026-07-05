package pro.gravit.launchermodules.mdnsserver;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;

public class MDNSService implements AutoCloseable {
    private String serviceType;
    private String serviceName;
    private int servicePort;

    private transient JmDNS jmDNS;

    public MDNSService(String serviceType, String serviceName, int servicePort, String address) throws IOException {
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.servicePort = servicePort;
        this.jmDNS = JmDNS.create();
        ServiceInfo serviceInfo = ServiceInfo.create(
                serviceType,
                serviceName,
                servicePort,
                address
        );
        jmDNS.registerService(serviceInfo);
    }

    @Override
    public void close() throws Exception {
        jmDNS.unregisterAllServices();
        jmDNS.close();
    }
}

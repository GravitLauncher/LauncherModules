package pro.gravit.launchermodules.swiftupdates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.storage.object.SwiftContainer;
import org.openstack4j.model.storage.object.options.CreateUpdateContainerOptions;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;
import org.openstack4j.openstack.OSFactory;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SwiftService {
    private static final Logger logger = LogManager.getLogger(SwiftService.class);
    private final OSClient.OSClientV3 openStackClient;
    private final Token sessionToken;
    private final String region;

    public SwiftService(String endpoint, String username, String password, String region, String domainName) {
        this.region = region;
        this.openStackClient = OSFactory.builderV3()
                .endpoint(endpoint)
                .credentials(username, password, Identifier.byName(domainName))
                .authenticate();
        this.sessionToken = openStackClient.getToken();
    }

    public void uploadDir(Path directory, String container, String prefix) throws IOException {
        final var objectStorage = OSFactory.clientFromToken(sessionToken).useRegion(region).objectStorage();
        final var containers = objectStorage.containers().list();
        if (containers.stream().map(SwiftContainer::getName).noneMatch(s -> s.equals(container))) {
            logger.info("[SwiftUpdates] No container with name {} found. Creating it now", container);
            objectStorage.containers().create(container, CreateUpdateContainerOptions.create().accessAnybodyRead());
        }
        List<CompletableFuture<?>> fileUploadsFuture = new ArrayList<>();
        final var startTime = System.currentTimeMillis();
        logger.info("[SwiftUpdates] Starting to upload updates directory contents to Object Storage container {} with prefix {}", container, prefix);
        IOHelper.walk(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                fileUploadsFuture.add(CompletableFuture.supplyAsync(() -> {
                    String eTag = OSFactory.clientFromToken(sessionToken).useRegion(region).objectStorage().objects().put(container,
                            prefix + directory.relativize(file),
                            Payloads.create(file.toFile()),
                            ObjectPutOptions.create().path(prefix));
                    logger.debug("[SwiftUpdates] Uploading {} ETag: {}", directory.relativize(file), eTag);
                    return null;
                }));
                return FileVisitResult.CONTINUE;
            }
        }, false);
        CompletableFuture.allOf(fileUploadsFuture.toArray(new CompletableFuture[0])).thenRun(() ->
                logger.info("[SwiftUpdates] Uploaded {} files with {} prefix Storage in {}", fileUploadsFuture.size(), prefix, SwiftUpdatesModule.readableTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime))));

    }

    public static class Config {
        public String openStackEndpoint = "";
        public String openStackUsername = "";
        public String openStackPassword = "";
        public String openStackDomain = "";
        public String openStackRegion = "";
        public String openStackContainer = "";
        public String prefix = "";

        public boolean isEmpty() {
            return openStackEndpoint.isEmpty() || openStackUsername.isEmpty() || openStackPassword.isEmpty() || openStackRegion.isEmpty() || openStackContainer.isEmpty();
        }
    }
}

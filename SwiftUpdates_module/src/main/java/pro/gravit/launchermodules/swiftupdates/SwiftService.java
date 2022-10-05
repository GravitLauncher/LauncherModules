package pro.gravit.launchermodules.swiftupdates;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.storage.object.SwiftContainer;
import org.openstack4j.model.storage.object.options.CreateUpdateContainerOptions;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectLocation;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;
import org.openstack4j.openstack.OSFactory;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SwiftService {
    private static final Logger logger = LogManager.getLogger(SwiftService.class);
    private final OSClient.OSClientV3 openStackClient;
    private final String region;

    public SwiftService(String endpoint, String username, String password, String region, String domainName) {
        this.region = region;
        this.openStackClient = OSFactory.builderV3()
                .endpoint(endpoint)
                .credentials(username, password, Identifier.byName(domainName))
                .authenticate();
    }

    public void uploadDir(Path directory, String container, String prefix, final boolean forceUpload) throws IOException {
        final var sessionToken = openStackClient.getToken();
        final var containers = getStorage(sessionToken, region).containers().list();

        // Make sure there is a container, if not we create one
        if (containers.stream().map(SwiftContainer::getName).noneMatch(s -> s.equals(container))) {
            logger.info("[SwiftUpdates] No container with name {} found. Creating it now", container);
            getStorage(sessionToken, region).containers().create(container, CreateUpdateContainerOptions.create().accessAnybodyRead());
        }

        // Go over updates folder recursively and queue all files for uploading
        List<CompletableFuture<?>> fileUploadsFuture = new ArrayList<>();
        final var startTime = System.currentTimeMillis();
        logger.info("[SwiftUpdates] Starting to upload updates directory contents to Object Storage container {} with prefix {}", container, prefix);

        IOHelper.walk(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                fileUploadsFuture.add(CompletableFuture.supplyAsync(() -> {
                    // Test local and remote ETags to make sure we don't consume unnecessary traffic
                    if (!forceUpload) {
                        try (InputStream input = IOHelper.newInput(file)) {
                            final String localETag = DigestUtils.md5Hex(input);
                            final var remoteObject = getStorage(sessionToken, region).objects()
                                    .get(container, prefix + directory.relativize(file));
                            if (remoteObject != null && localETag.equals(remoteObject.getETag())) {
                                logger.debug("[SwiftUpdates] SKIP: {} ETag: {}", directory.relativize(file), remoteObject.getETag());
                                return null;
                            }
                        } catch (IOException e) {
                            logger.error("[SwiftUpdates] Error while trying to fetch local ETag", e);
                        } catch (Exception e) {
                            logger.error("[SwiftUpdates] Other exception occurred", e);
                        }
                    }
                    // Put an object in the storage
                    String eTag = getStorage(sessionToken, region).objects()
                            .put(container, prefix + directory.relativize(file), Payloads.create(file.toFile()), ObjectPutOptions.create().path(prefix));
                    logger.debug("[SwiftUpdates] UPLOAD: {} ETag: {}", directory.relativize(file), eTag);
                    return null;
                }));
                return FileVisitResult.CONTINUE;
            }
        }, false);

        // Couple and run everything
        CompletableFuture.allOf(fileUploadsFuture.toArray(new CompletableFuture[0])).thenRun(() ->
                logger.info("[SwiftUpdates] Uploaded update files with {} prefix in {} to the storage",
                        prefix,
                        readableTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime))));
    }

    public void cleanupContainer(String container, String prefix) {
        final var sessionToken = openStackClient.getToken();
        logger.info("[SwiftUpdates] Cleaning up objects with prefix {} on the storage", prefix);
        final var startTime = System.currentTimeMillis();

        // Collect remote objects
        final var matchingRemoteData = getStorage(sessionToken, region).objects()
                .list(container, ObjectListOptions.create().startsWith(prefix)).stream().map(swiftObject ->
                        ObjectLocation.create(swiftObject.getContainerName(), swiftObject.getName()));

        // Map remote objects to futures that will delete objects
        final var filesCleanupFuture = matchingRemoteData.map(objectLocation -> CompletableFuture.supplyAsync(() -> {
            getStorage(sessionToken, region).objects().delete(objectLocation);
            logger.debug("[SwiftUpdates] Deleting {}", objectLocation.getObjectName());
            return null;
        })).toList();

        // Couple and run everything
        CompletableFuture.allOf(filesCleanupFuture.toArray(new CompletableFuture[0])).thenRun(() ->
                logger.info("[SwiftUpdates] Successfully cleaned up object storage with prefix {} in {}",
                        prefix,
                        readableTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime))));

    }

    private static ObjectStorageService getStorage(Token sessionToken, String region) {
        return OSFactory.clientFromToken(sessionToken).useRegion(region).objectStorage();
    }

    public static String readableTime(long seconds) {
        return String.format("%02dm%02ds", (seconds % 3600) / 60, (seconds % 60));
    }

    public static class Config {
        public String openStackEndpoint = "";
        public String openStackUsername = "";
        public String openStackPassword = "";
        public String openStackDomain = "";
        public String openStackRegion = "";
        public String openStackContainer = "";
        public SwiftSettings behavior = new SwiftSettings();

        public boolean isEmpty() {
            return openStackEndpoint.isEmpty() || openStackUsername.isEmpty() ||
                    openStackPassword.isEmpty() || openStackRegion.isEmpty() ||
                    openStackContainer.isEmpty();
        }
    }

    public static class SwiftSettings {
        public boolean forceUpload = false;
        public String prefix = "updates/";
    }
}

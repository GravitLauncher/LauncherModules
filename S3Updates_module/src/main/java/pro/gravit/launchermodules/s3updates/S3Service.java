package pro.gravit.launchermodules.s3updates;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.helper.IOHelper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class S3Service {
    private static final Logger logger = LogManager.getLogger(S3Service.class);
    private final S3AsyncClient s3AsyncClient;

    public S3Service(Config serviceConfig) {
        final var nettyClient = NettyNioAsyncHttpClient.builder()
                .maxConcurrency(serviceConfig.behavior.maxConcurrentConnections)
                .maxPendingConnectionAcquires(serviceConfig.behavior.maxPendingConnections)
                .connectionTimeout(Duration.ofSeconds(serviceConfig.behavior.connectionTimeout))
                .connectionAcquisitionTimeout(Duration.ofSeconds(serviceConfig.behavior.connectionAcquisitionTimeout))
                .build();
        this.s3AsyncClient = S3AsyncClient.builder()
                .endpointOverride(URI.create(serviceConfig.s3Endpoint))
                .httpClient(nettyClient)
                .region(Region.of(serviceConfig.s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(serviceConfig.s3AccessKey, serviceConfig.s3SecretKey)))
                .build();
    }

    public void uploadDir(Path directory, String bucket, String prefix, final boolean forceUpload) throws IOException {
        logger.info("[S3Updates] Starting to upload updates directory contents to bucket {} with prefix {}", bucket, prefix);
        List<CompletableFuture<?>> fileFutures = new ArrayList<>();
        final var startTime = System.currentTimeMillis();

        IOHelper.walk(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                final var fileKey = prefix + directory.relativize(file);
                try (InputStream input = IOHelper.newInput(file)) {
                    if (forceUpload) {
                        // Force upload everything
                        fileFutures.add(putObject(bucket, fileKey, file));
                    } else {
                        // Make request to get remote ETag along with local ETag
                        fileFutures.add(putCheckedObject(bucket, fileKey, file, DigestUtils.md5Hex(input)));
                    }
                } catch (IOException e) {
                    logger.error("[S3Updates] Error while trying to fetch local ETag", e);
                }
                return FileVisitResult.CONTINUE;
            }}, false);

        CompletableFuture.allOf(fileFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("[S3Updates] Uploaded update files with {} prefix in {}",
                        prefix,
                        readableTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime))));
    }

    public void cleanupBucket(String bucket, String prefix) {
        logger.info("[S3Updates] Cleaning up objects with prefix {}", prefix);

        // Collect remote objects
        final var listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();

        s3AsyncClient.listObjectsV2Paginator(listObjectsRequest)
                .subscribe(listObjectsV2Response -> {
                    final var remoteObjectsToDelete = listObjectsV2Response.contents().stream()
                            .map(S3Object::key)
                            .map(key -> ObjectIdentifier.builder().key(key).build()).toList();
                    final var deletes = Delete.builder().objects(remoteObjectsToDelete).build();
                    final var deleteObjectsRequest = DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(deletes)
                            .build();
                    // Delete objects
                    s3AsyncClient.deleteObjects(deleteObjectsRequest)
                            .exceptionallyAsync(throwable -> {
                                logger.error("[S3Updates] Other exception occurred while trying to remove objects", throwable);
                                return null;
                            })
                            .thenAcceptAsync(deleteObjectsResponse -> {
                                if (deleteObjectsResponse.hasErrors()) {
                                    logger.error("[S3Updates] Errors occurred while trying to delete objects: \n{}", deleteObjectsResponse.errors());
                                }
                                if (deleteObjectsResponse.hasDeleted()) {
                                    logger.info("[S3Updates] Successfully cleaned up {} objects",
                                            deleteObjectsResponse.deleted().size());
                                }});
                });
    }

    // FIXED: Works on VK Cloud, but not OVH?
    private CompletableFuture<PutObjectResponse> putCheckedObject(String container, String key, Path file, String localETag) {
        final var getObjectRequest = HeadObjectRequest.builder()
                .bucket(container)
                .key(key)
                .build();

        return s3AsyncClient.headObject(getObjectRequest)
                .handleAsync((headObjectResponse, throwable) -> {
                    if (throwable instanceof NoSuchKeyException) {
                        // Case: No such remote object found. For some reason some providers never throw this, and some can throw entirely different error
                        // (VK Cloud works just fine, OVH has issues)
                        logger.debug("[S3Updates] NOT FOUND: {}", key);
                    } else if (throwable instanceof SdkException ) {
                        // Case: Some other exception either service returned error code or SDK broke down
                        logger.error("[S3Updates] Other exception occurred while fetching metadata", throwable);
                        return null;
                    }
                    return headObjectResponse;
                })
                .thenComposeAsync(headObjectResponse -> {
                    if (headObjectResponse == null) {
                        // Case: No such key or other exception - upload
                        return putObject(container, key, file);
                    } else if (compareETag(headObjectResponse.eTag(), localETag)) {
                        // Case: Remote object is present and has same ETag as local - skipping
                        logger.debug("[S3Updates] SKIP: {} ETag: {}", key, headObjectResponse.eTag());
                        return null;
                    } else {
                        // Case: Remote object is present but ETag is different, or other edge case - upload
                        return putObject(container, key, file);
                    }
                });
    }

    private CompletableFuture<PutObjectResponse> putObject(String container, String key, Path file) {
        // Make a put request
        final var putObjectRequest = PutObjectRequest.builder()
                .bucket(container)
                .key(key)
                .build();
        // Put the object
        return s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromFile(file))
                .whenCompleteAsync((putObjectResponse, throwable) -> {
                    if (throwable != null) {
                        logger.error("[S3Updates] Error while uploading {} Stacktrace:", key, throwable);
                    } else {
                        logger.debug("[S3Updates] UPLOAD: {} ETag: {}", key, putObjectResponse.eTag());
                    }
                });
    }

    public static boolean compareETag(String remote, String local) {
        // AWS SDK returns ETag with quotes, no idea why
        if (remote.contains("\"")) {
            local = "\"" + local + "\"";
        }
        return remote.equals(local);
    }

    public static String readableTime(long seconds) {
        return String.format("%02dm%02ds", (seconds % 3600) / 60, (seconds % 60));
    }

    public static class Config {
        public String s3Endpoint = "";
        public String s3AccessKey = "";
        public String s3SecretKey = "";
        public String s3Region = "";
        public String s3Bucket = "";
        public S3Settings behavior = new S3Settings();

        public boolean isEmpty() {
            return s3Endpoint.isEmpty() || s3AccessKey.isEmpty() || s3SecretKey.isEmpty() || s3Bucket.isEmpty();
        }
    }

    public static class S3Settings {
        public boolean forceUpload = false;
        public String prefix = "updates/";
        public int maxConcurrentConnections = 20;
        public int maxPendingConnections = Integer.MAX_VALUE;
        public int connectionTimeout = 30;
        public int connectionAcquisitionTimeout = 80;
    }
}

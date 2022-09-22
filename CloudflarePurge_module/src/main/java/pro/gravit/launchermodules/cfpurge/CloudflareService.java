package pro.gravit.launchermodules.cfpurge;

import eu.roboflax.cloudflare.CloudflareAccess;
import eu.roboflax.cloudflare.CloudflareRequest;
import eu.roboflax.cloudflare.constants.Category;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum CloudflareService {
    INSTANCE;
    private static final Logger logger = LogManager.getLogger(CloudflareService.class);
    private CloudflareAccess cfAccess;


    public void purgeAll(String cloudflareApiToken, String zoneId) {
        if (cfAccess == null) {
            cfAccess = new CloudflareAccess(cloudflareApiToken);
        }
        final var request = new CloudflareRequest(Category.PURGE_ZONE_ALL_FILES, cfAccess)
                .identifiers(zoneId)
                .body("purge_everything", true)
                .asVoid();

        if (request.isSuccessful()) {
            logger.info("Successfully purged zone {}", zoneId);
        } else {
            logger.error("Error occurred while trying to purge zone {}\n{}",
                    zoneId, request.getErrors());
        }
    }

    public static class Config {
        public String cloudflareToken = "";
        public String zoneIdentifier = "";
    }
}

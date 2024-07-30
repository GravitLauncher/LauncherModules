package pro.gravit.launchermodules.mirrorhelper.newforge;

import java.util.List;

public record ForgeProfile(String mainClass, ForgeProfileArguments arguments, List<ForgeProfileLibrary> libraries) {
    public record ForgeProfileArguments(List<String> game, List<String> jvm) {

    }

    public record ForgeProfileLibrary(String name, ForgeProfileLibraryDownload downloads) {
        public record ForgeProfileLibraryDownload(ForgeProfileLibraryArtifact artifact) {

        }
        public record ForgeProfileLibraryArtifact(String sha1, long size, String url, String path) {

        }
    }
}

package pro.gravit.launchermodules.mirrorhelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launchermodules.mirrorhelper.commands.LwjglDownloadCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WorkspaceTools {
    private final LwjglDownloadCommand lwjgl;
    private final Logger logger = LogManager.getLogger(WorkspaceTools.class);
    public WorkspaceTools(MirrorHelperModule module) {
        lwjgl = new LwjglDownloadCommand(module.server);
    }

    public void prepareWorkspace(MirrorWorkspace workspace, Path workspacePath) throws Exception {
        Path tmp = Files.createTempDirectory("mirrorhelper");
        try {
            logger.info("Apply workspace");
            logger.info("Download libraries");
            for (var l : workspace.artifacts()) {
                if (l.data() != null) {
                    IOHelper.createParentDirs(workspacePath.resolve(l.path()));
                    IOHelper.write(workspacePath.resolve(l.path()), l.data().getBytes(StandardCharsets.UTF_8));
                    continue;
                }
                String randomName = SecurityHelper.randomStringAESKey();
                Path tmpPath = tmp.resolve(randomName);
                logger.info("Download {} to {}", l.url(), tmpPath);
                Downloader.downloadFile(WorkspaceManifest.remoteUri(l.url()), tmpPath, null).getFuture().get();
                if (l.path() != null) {
                    Path lPath = workspacePath.resolve(l.path());
                    IOHelper.createParentDirs(lPath);
                    if (l.prefixFilter() != null) {
                        Predicate<String> pred = (p) -> {
                            for (var e : l.prefixFilter()) {
                                if (p.startsWith(e)) {
                                    return true;
                                }
                            }
                            return false;
                        };
                        try (ZipInputStream input = IOHelper.newZipInput(tmpPath)) {
                            try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(lPath))) {
                                ZipEntry e = input.getNextEntry();
                                while (e != null) {
                                    if (pred.test(e.getName())) {
                                        WorkspaceManifest.resolve(workspacePath, e.getName());
                                        ZipEntry entry = new ZipEntry(e.getName());
                                        output.putNextEntry(entry);
                                        input.transferTo(output);
                                    }
                                    e = input.getNextEntry();
                                }
                            }
                        }
                    } else {
                        IOHelper.copy(tmpPath, lPath);
                    }
                }
                if (l.unpack() != null) {
                    Set<String> missing = new HashSet<>(l.unpack().keySet());
                    try (ZipInputStream input = IOHelper.newZipInput(tmpPath)) {
                        ZipEntry e = input.getNextEntry();
                        while (e != null) {
                            String target = l.unpack().get(e.getName());
                            if (target != null && !e.isDirectory() && missing.remove(e.getName())) {
                                Path targetPath = workspacePath.resolve(target);
                                IOHelper.createParentDirs(targetPath);
                                try (OutputStream output = IOHelper.newOutput(targetPath)) {
                                    input.transferTo(output);
                                }
                            }
                            e = input.getNextEntry();
                        }
                    }
                    if (!missing.isEmpty()) {
                        throw new IOException("Missing archive entries: " + missing);
                    }
                }
                Files.delete(tmpPath);
            }
            logger.info("Download multiMods");
            for (var e : workspace.clientArtifacts().entrySet()) {
                Path target = workspacePath.resolve("multimods").resolve(e.getKey().concat(".jar"));
                IOHelper.createParentDirs(target);
                logger.info("Download {} to {}", e.getValue().url(), target);
                Downloader.downloadFile(WorkspaceManifest.remoteUri(e.getValue().url()), target, null).getFuture().get();
            }
            logger.info("Install lwjgl3 directory");
            Set<String> lwjglVersions = new HashSet<>();
            lwjglVersions.add(workspace.lwjgl().defaultVersion());
            for(var e : workspace.lwjgl().overrides()) {
                lwjglVersions.add(e.version());
            }
            for(var lwjgl3Version : lwjglVersions) {
                downloadLwjgl3(lwjgl3Version, workspacePath);
            }
        } finally {
            IOHelper.deleteDir(tmp, true);
        }
    }

    private void downloadLwjgl3(String lwjgl3version, Path workspacePath) throws Exception {
        Path lwjgl3Path = workspacePath.resolve("workdir").resolve("lwjgl").resolve(lwjgl3version);
        Files.createDirectories(lwjgl3Path);
        lwjgl.download(lwjgl3version, lwjgl3Path);
    }

}

package pro.gravit.launchermodules.sentryproguardupload;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SentryProguardUploadModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public static final AtomicReference<UUID> proguardUuid = new AtomicReference<>();
    private static final Logger logger = LogManager.getLogger(SentryProguardUploadModule.class);
    public Config config;
    public JsonConfigurable<Config> configurable;

    public SentryProguardUploadModule() {
        super(new LauncherModuleInfoBuilder().setName("SentryProGuardUpload").setVersion(version).setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
    }


    public void finish(LaunchServerPostInitPhase event) {
        init(event.server);
    }

    public void init(LaunchServer launchServer) {
        Path path = modulesConfigManager.getModuleConfig(moduleInfo.name);
        SentryProguardUploadModule module = this;
        configurable = new JsonConfigurable<>(Config.class, path) {
            @Override
            public Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(Config config) {
                module.config = config;
            }

            @Override
            public Config getDefaultConfig() {
                return new Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }

        var mainTask = launchServer.launcherBinary.getTaskByClass(MainBuildTask.class).orElseThrow();
        mainTask.preBuildHook.registerHook((context ->  {
            proguardUuid.set(UUID.randomUUID());
            context.properties.put("modules.sentry.proguarduuid", proguardUuid.get().toString());
        }));
        launchServer.launcherBinary.tasks.add(new ProguardUploadTask());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
        if (initContext instanceof LaunchServerInitContext ctx) {
            init(ctx.server);
        }
    }

    public class ProguardUploadTask implements LauncherBuildTask {

        @Override
        public String getName() {
            return "proguardUpload";
        }

        @Override
        public Path process(PipelineContext context) throws IOException {
            List<String> args = new ArrayList<>();
            args.add(config.sentryCliPath);
            if(config.url != null) {
                args.add("--url");
                args.add(config.url);
            }
            args.addAll(config.customArgsBefore);
            args.add("upload-proguard");
            if(config.authToken != null) {
                args.add("--auth-token");
                args.add(config.authToken);
            }
            if(config.org != null) {
                args.add("--org");
                args.add(config.org);
            }
            if(config.project != null) {
                args.add("--project");
                args.add(config.project);
            }
            args.add("--uuid");
            args.add(proguardUuid.get().toString());
            args.addAll(config.customArgs);
            args.add(config.mappingPath);
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.inheritIO();
            Process process = builder.start();
            try {
                int r = process.waitFor();
                if(r != 0) {
                    logger.error("ProGuard mapping upload failed with code {}", r);
                }
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            return null;
        }
    }
}

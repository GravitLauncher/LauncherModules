val launchserver = sourceSets.create("launchserver")

dependencies {
    add(launchserver.compileOnlyConfigurationName, project(":components:launchserver"))
    add(launchserver.compileOnlyConfigurationName, libs.asm.tree)
}

tasks.jar {
    from(launchserver.output)
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.example.hybridmoule.HybridLauncherModule")
    manifest.attributes.put("Module-LaunchServer-Class", "pro.gravit.launchermodules.example.hybridmodule.launchserver.ModuleBuildHook")
}

tasks.sourcesJar {
    from(launchserver.allSource)
}

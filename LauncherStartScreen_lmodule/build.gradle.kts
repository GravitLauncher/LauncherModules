java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.startscreen.ModuleImpl")
    manifest.attributes.put("Module-Config-Class", "pro.gravit.launchermodules.startscreen.Config")
    manifest.attributes.put("Module-Config-Name", "LauncherStartScreen")
}

dependencies {
    
}
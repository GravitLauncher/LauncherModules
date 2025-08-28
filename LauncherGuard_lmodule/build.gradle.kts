java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.guard.ModuleImpl")
    manifest.attributes.put("Module-Config-Class", "pro.gravit.launchermodules.guard.Config")
    manifest.attributes.put("Module-Config-Name", "LauncherGuard")
}

dependencies {
    
}
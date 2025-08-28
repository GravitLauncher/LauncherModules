java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.addhash.AddHashModule")
}

dependencies {
    implementation("org.mindrot:jbcrypt:0.4")
}
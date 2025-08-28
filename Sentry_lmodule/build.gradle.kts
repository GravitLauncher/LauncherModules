java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.sentryl.SentryModule")
    manifest.attributes.put("Module-Config-Class", "pro.gravit.launchermodules.sentryl.Config")
    manifest.attributes.put("Module-Config-Name", "Sentry")
}

dependencies {
    implementation("io.sentry:sentry:8.20.0")
    compileOnly(libs.oshi)
}
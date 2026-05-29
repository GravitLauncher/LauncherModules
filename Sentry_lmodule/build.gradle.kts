java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.sentryl.SentryModule")
    manifest.attributes.put("Module-Config-Class", "pro.gravit.launchermodules.sentryl.Config")
    manifest.attributes.put("Module-Config-Name", "Sentry")
}

dependencies {
    implementation(libs.sentry)
    compileOnly(libs.oshi.core)
    compileOnly(libs.oshi.common)
}
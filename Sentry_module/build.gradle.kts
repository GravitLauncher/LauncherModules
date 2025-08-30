java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.sentrys.SentryModule")
}

dependencies {
    implementation("io.sentry:sentry:8.20.0")
    implementation("io.sentry:sentry-log4j2:8.20.0")
    compileOnly(libs.log4j.core)
}
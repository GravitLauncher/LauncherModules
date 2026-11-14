
tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.sentrys.SentryModule")
}

dependencies {
    implementation(libs.sentry)
    implementation(libs.sentry.log4j2)
    compileOnly(libs.log4j.core)
}

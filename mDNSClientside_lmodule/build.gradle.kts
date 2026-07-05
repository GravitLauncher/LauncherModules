java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.mdnsclient.MDNSClient")
    manifest.attributes.put("Module-Config-Class", "pro.gravit.launchermodules.mdnsclient.Config")
    manifest.attributes.put("Module-Config-Name", "MDNSClient")
}

dependencies {
    implementation(libs.jmdns)
    implementation(libs.jbcrypt)
}

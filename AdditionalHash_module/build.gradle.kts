java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.addhash.AddHashModule")
}

dependencies {
    implementation("org.mindrot:jbcrypt:0.4")
}
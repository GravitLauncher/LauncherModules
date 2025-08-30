java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.remotecontrol.RemoteControlModule")
}

dependencies {
    compileOnly(libs.netty.codec.http)
    compileOnly(libs.log4j.core)
}
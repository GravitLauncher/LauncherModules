java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.remotecontrol.RemoteControlModule")
}

dependencies {
    compileOnly(libs.netty.codec.http)
    compileOnly(libs.log4j.core)
}
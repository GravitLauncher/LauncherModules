
tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule")
}

dependencies {
    api(libs.jline.reader)
}

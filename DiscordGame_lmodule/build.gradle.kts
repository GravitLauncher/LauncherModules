java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.discordgame.ClientModule")
    manifest.attributes.put("Module-Config-Class", "pro.gravit.launchermodules.discordgame.Config")
    manifest.attributes.put("Module-Config-Name", "DiscordGame")
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.JnCrMx:discord-game-sdk4j:v1.0.0")
}
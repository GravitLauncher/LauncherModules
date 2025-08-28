
repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.clojars.org")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.clojars.org")
        }
    }

    if(project.name.endsWith("_module")) {
        dependencies {
            api(project(":LaunchServer"))
        }
    } else if(project.name.endsWith("_lmodule")) {
        dependencies {
            api(project(":Launcher"))
        }
    }
}
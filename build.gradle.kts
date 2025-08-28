
repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.clojars.org")
    }
}

val copyModules = tasks.register("copyModules", Copy::class) {
    from(subprojects.map { it -> it.tasks["jar"].outputs })
    into(layout.buildDirectory.dir("all-modules"))
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

tasks.assemble {
    dependsOn(copyModules)
}
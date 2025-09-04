plugins {
    id("maven-publish")
}

group = "com.gravitlauncher.launcher.modules"

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
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.clojars.org")
        }
    }
    tasks.jar {
        archiveFileName.set(project.name+".jar")
    }
    tasks.sourcesJar {
        archiveFileName.set(project.name+"-sources.jar")
    }
    tasks.javadocJar {
        archiveFileName.set(project.name+"-javadoc.jar")
    }

    if(project.name.endsWith("_module")) {
        dependencies {
            api(project(":components:launchserver"))
        }
    } else if(project.name.endsWith("_lmodule")) {
        dependencies {
            api(project(":components:launcher-runtime"))
        }
    }

    publishing {
        repositories {
            mavenLocal()
        }
        publications {
            var name = project.name
            create<MavenPublication>("maven") {
                groupId = "com.gravitlauncher.launcher.modules"
                artifactId = name
                version = project.version as String?

                from(components["java"])
            }
        }
    }
}

tasks.assemble {
    dependsOn(copyModules)
}
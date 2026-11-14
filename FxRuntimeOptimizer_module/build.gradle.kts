plugins {
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    modules("javafx.controls", "javafx.fxml")
}

tasks.jar {
    manifest.attributes.put("Module-Main-Class", "pro.gravit.launchermodules.fxruntimeoptimizer.FxRuntimeOptimizerModule")
}


dependencies {

}

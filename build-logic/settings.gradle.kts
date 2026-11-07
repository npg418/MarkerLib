plugins {
    id("dev.panuszewski.typesafe-conventions") version "0.11.1"
}

typesafeConventions {
    conventionCatalog {
        ignorePackageNames = true
    }
}

rootProject.name = "build-logic"
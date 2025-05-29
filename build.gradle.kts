plugins {
    id("java")
    id("application")
    alias(libs.plugins.shadow)

    // Lombok
    alias(libs.plugins.lombok)
    // Templating plugins
    alias(libs.plugins.blossom)
    alias(libs.plugins.ideaext)
}

group = "dev.starless"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
}

dependencies {
    // Web libraries (frameworks, gson, caches...)
    implementation(libs.javalin) // Webserver
    implementation(libs.caffeine) // Cache
    implementation(libs.gson) // Gson
    implementation(libs.tika)

    // MariaDB
    implementation(libs.hikari)
    implementation(libs.mariadb)
    // ORM
    implementation(libs.hibernate)
    implementation(libs.hibernatehikari)
    implementation(libs.hibernatevalidator)
    annotationProcessor(libs.hibernatejpamodelgen)
    // Hashing & JWT
    implementation(libs.security)
    implementation(libs.jwt)

    implementation(libs.configurate) // Configurate
    implementation(libs.sem4j) // Semantic versioning util

    implementation(libs.logback) // Logger implementation
}

application {
    mainClass.set("dev.starless.hosting.Main")
}

// Blossom configuration
sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
}

// gradle build will also run the shadowJar task
tasks.build {
    dependsOn("shadowJar")
}
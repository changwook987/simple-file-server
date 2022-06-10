plugins {
    kotlin("jvm") version "1.6.21"
}

group = "io.github.changwook987"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks {
    create<Jar>("serverJar") {
        from(sourceSets["main"].output)

        manifest {
            attributes["Main-Class"] = "io.github.changwook987.simpleFileServer.ServerKt"
        }

        archiveBaseName.set("Server")
        archiveVersion.set("")

        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        copy {
            from(archiveFile)
            into(File(project.rootDir, "result"))
        }
    }

    create<Jar>("clientJar") {
        from(sourceSets["main"].output)

        manifest {
            attributes["Main-Class"] = "io.github.changwook987.simpleFileServer.ClientKt"
        }

        archiveBaseName.set("Client")
        archiveVersion.set("")

        from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        copy {
            from(archiveFile)
            into(File(project.rootDir, "result"))
        }
    }

    task("all").dependsOn("serverJar", "clientJar")
}
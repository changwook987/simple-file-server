plugins {
    java
}

group = "kr.hs.dgsw.network"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    getByName<Test>("test") {
        useJUnitPlatform()
    }

    create<Jar>("serverJar") {
        from(sourceSets["main"].output)

        manifest {
            attributes["Main-Class"] = "kr.hs.dgsw.network.test01.n2118.server.Server"
        }

        archiveBaseName.set("Server")
        archiveVersion.set("")

        copy {
            from(archiveFile)
            into(File(project.rootDir, "result"))
        }
    }

    create<Jar>("clientJar") {
        from(sourceSets["main"].output)

        manifest {
            attributes["Main-Class"] = "kr.hs.dgsw.network.test01.n2118.client.Client"
        }

        archiveBaseName.set("Client")
        archiveVersion.set("")

        copy {
            from(archiveFile)
            into(File(project.rootDir, "result"))
        }
    }

    task("all").dependsOn("serverJar", "clientJar")
}
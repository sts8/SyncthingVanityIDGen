import org.gradle.internal.os.OperatingSystem

plugins {
    id("java")
    id("application")
    id("jacoco")
}

group = "io.github.sts8"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.2")
}

application {
    mainClass.set("io.github.sts8.vanitygen.cli.Main")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }

    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )

    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA"
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    reports {
        html.required.set(true)
    }

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.register<Exec>("packageBinary") {
    group = "distribution"
    description = "Packages the application as a native binary using jpackage (Java 25)."

    // Ensure the JAR is built first
    dependsOn("jar")

    val outputDir = layout.buildDirectory.dir("native")
    val inputDir = layout.buildDirectory.dir("libs")
    val jarName = "${project.name}-${project.version}.jar"

    // Clean up old builds before running
    doFirst {
        delete(outputDir)
    }

    // Direct call to the JDK 25 jpackage tool
    executable = "jpackage"

    args(
        "--name", "vanitygen",
        "--input", inputDir.get().asFile.absolutePath,
        "--main-jar", jarName,
        "--main-class", application.mainClass.get(),
        "--dest", outputDir.get().asFile.absolutePath,
        "--type", "app-image",
        "--add-modules", "java.base,java.naming,java.sql",
        "--jlink-options", "--strip-debug --compress zip-9 --no-header-files --no-man-pages",
        "--java-options", "-XX:+UseParallelGC -Xms2G -Xmx2G -XX:+UseCompactObjectHeaders"
    )

    if (OperatingSystem.current().isWindows) {
        args("--win-console")
    }
}

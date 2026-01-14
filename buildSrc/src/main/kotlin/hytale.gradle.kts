import java.nio.file.Files

plugins {
    id("java")
}

project.properties["group"]?.let { group = it }
project.properties["version"]?.let { version = it }

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    hytale()
}

tasks.processResources {
    val expansionVars = providers.gradlePropertiesPrefixedBy("")
        .getOrElse(emptyMap())

    expansionVars.forEach {
        inputs.property(it.key, it.value)
    }

    filteringCharset = "UTF-8"

    filesMatching("manifest.json") {
        expand(expansionVars)
    }
}

tasks.test {
    useJUnitPlatform()
}

fun DependencyHandler.hytale() = implementation(files(project.layout.buildDirectory.file("hytale/HytaleServer.jar")))

tasks.register("setupHytaleServer") {
    group = "hytale"
    description = "Ensures HytaleServer.jar and Assets.zip are available in dev environment"

    doLast {
        val buildDir = project.layout.buildDirectory.get().asFile
        val hytaleDir = File(buildDir, "hytale")
        val tmpDir = File(buildDir, "tmp")

        hytaleDir.mkdirs()
        tmpDir.mkdirs()

        val hytaleJar = File(hytaleDir, "HytaleServer.jar")
        val assetsZip = File(hytaleDir, "Assets.zip")

        if (!hytaleJar.exists() || !assetsZip.exists()) {
            val parentJar = File(project.projectDir.parentFile, "HytaleServer.jar")
            val parentAssets = File(project.projectDir.parentFile, "Assets.zip")

            if (parentJar.exists() && parentAssets.exists()) {
                logger.lifecycle("Creating symbolic links from parent directory")

                try {
                    Files.createSymbolicLink(
                        hytaleJar.toPath(),
                        parentJar.toPath().toAbsolutePath()
                    )
                    Files.createSymbolicLink(
                        assetsZip.toPath(),
                        parentAssets.toPath().toAbsolutePath()
                    )
                    logger.lifecycle("Created symbolic links")
                } catch (e: Exception) {
                    logger.warn("Symbolic links failed, copying files instead: ${e.message}")

                    parentJar.copyTo(hytaleJar, overwrite = true)
                    parentAssets.copyTo(assetsZip, overwrite = true)
                    logger.lifecycle("Copied files from parent directory")
                }
            } else {
                val downloaderName = getDownloaderName()
                val downloader = File(hytaleDir, downloaderName)

                if (!downloader.exists()) {
                    logger.lifecycle("Downloading hytale-downloader...")

                    val downloaderZip = File(tmpDir, "hytale-downloader.zip")
                    val url = "https://downloader.hytale.com/hytale-downloader.zip"

                    downloaderZip.parentFile.mkdirs()
                    ant.invokeMethod("get", mapOf(
                        "src" to url,
                        "dest" to downloaderZip
                    ))

                    ant.invokeMethod("unzip", mapOf(
                        "src" to downloaderZip,
                        "dest" to hytaleDir
                    ))

                    downloaderZip.delete()

                    logger.lifecycle("Downloaded and extracted hytale-downloader")
                }

                logger.lifecycle("Running hytale-downloader...")

                providers.exec {
                    workingDir = hytaleDir
                    commandLine = listOf(downloader.absolutePath)
                }

                logger.lifecycle("hytale-downloader completed")
            }
        } else {
            logger.lifecycle("Hytale files already exist in build/hytale")
        }
    }
}

fun getDownloaderName(): String {
    val os = System.getProperty("os.name").lowercase()
//    val arch = System.getProperty("os.arch").toLowerCase()

    return when {
        os.contains("win") -> "hytale-downloader-windows-amd64.exe"
//        os.contains("mac") -> "hytale-downloader-darwin-amd64"
//        os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
//            when {
//                arch.contains("64") -> "hytale-downloader-linux-amd64"
////                arch.contains("arm") || arch.contains("aarch") -> "hytale-downloader-linux-arm64"
//                else -> "hytale-downloader-linux-amd64" // default
//            }
//        }
        else -> "hytale-downloader-linux-amd64"
    }
}

tasks.register("prepareHytaleBuild", Copy::class) {
    group = "hytale"

    val mods = project.layout.projectDirectory.file("run/mods").asFile
    mods.mkdirs()

    from(tasks.named("jar"))
    into(mods)

    rename { _ -> "dev.jar" }
}

tasks.register("runHytaleServer", JavaExec::class) {
    val base = project.layout.buildDirectory.file("hytale").get().asFile

    val path = base.resolve("HytaleServer.jar")
    val assets = base.resolve("Assets.zip")

    group = "hytale"
    description = "Runs the hytale server with the built mod"

    mainClass = "-jar"
    args = listOf(path.toString(), "--assets", assets.toString(), "--disable-sentry")

    workingDir = project.layout.projectDirectory.file("run").asFile
    workingDir.mkdirs()

    dependsOn("prepareHytaleBuild")

    onlyIf {
        base.exists() && path.exists() && assets.exists()
    }
}
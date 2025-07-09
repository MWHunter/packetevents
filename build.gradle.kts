// TODO UPDATE
ext["fullVersion"] = "2.9.0"
ext["snapshot"] = true

ext["commitHash"] = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }.orElse("unknown")
ext["versionMeta"] = if (ext["snapshot"] == true) "-SNAPSHOT" else ""
ext["versionMetaWithHash"] = "+${ext["commitHash"]}${ext["versionMeta"]}"
ext["versionNoHash"] = "${ext["fullVersion"]}${ext["versionMeta"]}"

group = "com.github.retrooper"
description = rootProject.name
version = "${ext["fullVersion"]}${ext["versionMetaWithHash"]}"

tasks {
    wrapper {
        gradleVersion = "8.13"
        distributionType = Wrapper.DistributionType.ALL
    }

    val taskSubModules: (String) -> Array<Task> = { task ->
        subprojects.filterNot { it.path == ":patch" }.map { it.tasks[task] }.toTypedArray()
    }

    register("build") {
        dependsOn(*taskSubModules("build"))
        group = "build"

        doLast {
            val buildOut = project.layout.buildDirectory.dir("libs").get().asFile
            if (!buildOut.exists())
                buildOut.mkdirs()

            for (subproject in subprojects) {
                if (subproject.path.startsWith(":patch")) continue
                val subIn = subproject.layout.buildDirectory.dir("libs").get()

                copy {
                    from(subIn)
                    into(buildOut)
                }
            }
        }
    }

    register<Delete>("clean") {
        dependsOn(*taskSubModules("clean"))
        group = "build"
        delete(rootProject.layout.buildDirectory)
    }

    defaultTasks("build")
}

allprojects {
    tasks {
        withType<Jar> {
            archiveVersion = rootProject.ext["versionNoHash"] as String
        }
    }
}

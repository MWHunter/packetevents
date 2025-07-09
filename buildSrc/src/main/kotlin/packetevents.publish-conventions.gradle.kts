import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.modmuss50.mpp.ModPublishExtension

plugins {
    me.modmuss50.`mod-publish-plugin`
}

configure<ModPublishExtension> {
    file = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }
    additionalFiles.from(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
    changelog = providers.environmentVariable("CHANGELOG").getOrElse("No changelog provided")
    type = if (rootProject.version.toString().endsWith("-SNAPSHOT")) BETA else STABLE
    dryRun = !hasProperty("noDryPublish")

    val platform = project.projectDir.name

    github {
        accessToken = providers.environmentVariable("GITHUB_API_TOKEN")
        repository = providers.environmentVariable("GITHUB_REPOSITORY")
        commitish = providers.environmentVariable("GITHUB_REF_NAME")
        displayName = rootProject.version.toString()
        // TODO parent
    }
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_API_TOKEN")
        var versionBob = "${rootProject.ext["fullVersion"]}+${platform}"
        if (rootProject.ext["snapshot"] == true) {
            // e.g. "0.0.0+platform.abcdefg"
            version = "${versionBob}.${rootProject.ext["commitHash"]}"
        } else {
            // e.g. "0.0.0+platform"
            version = versionBob
        }
    }
}

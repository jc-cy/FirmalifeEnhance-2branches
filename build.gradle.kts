plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.+"
}

val minecraftVersion = "1.20.1"
val forgeVersion = "47.1.3"
val mixinVersion = "0.8.5"
val parchmentVersion = "2023.09.03-1.20.1"
val patchouliVersion = "1.20.1-81-FORGE"
val jeiVersion = "15.2.0.21"
val tfcCurseVersion = "5872631"
val firmalifeCurseVersion = "5456804"

val tfcSourceDir = "C:/Users/g1739/Desktop/MCmod源码/TerraFirmaCraft-3.2.21-1.20"
val firmalifeSourceDir = "C:/Users/g1739/Desktop/MCmod源码/firmalife-2.1.27-1.20"
val configuredModsDir = "C:/Users/g1739/Desktop/PCL/.minecraft/versions/TerraFirmaFarHorizons/mods"

val tfcLocalJars = fileTree("$tfcSourceDir/build/libs") {
    include("*.jar")
    exclude("*-sources.jar", "*-javadoc.jar")
}
val firmalifeLocalJars = fileTree("$firmalifeSourceDir/build/libs") {
    include("*.jar")
    exclude("*-sources.jar", "*-javadoc.jar")
}
val configuredLocalJars = fileTree(configuredModsDir) {
    include("configured-forge-1.20.1-*.jar")
}
val forgeConfigScreensLocalJars = fileTree(configuredModsDir) {
    include("ForgeConfigScreens-v*-1.20.1-Forge.jar")
}

val modId = "firmalife_greenhouse_patch"
val modVersion = System.getenv("VERSION") ?: "0.1.3-1.20.1"

group = "com.g1739.firmalifegreenhousepatch"
version = modVersion

base {
    archivesName.set("FirmalifeEnhance-Forge-$minecraftVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

idea {
    module {
        excludeDirs.add(file("run"))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://dvs1.progwml6.com/files/maven/")
    maven(url = "https://modmaven.k-4u.nl")
    maven(url = "https://maven.blamejared.com")
    maven(url = "https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge", "forge", "$minecraftVersion-$forgeVersion")

    if (tfcLocalJars.files.isNotEmpty()) {
        implementation(files(tfcLocalJars))
    } else {
        implementation(fg.deobf("curse.maven:tfc-302973:$tfcCurseVersion"))
    }

    if (firmalifeLocalJars.files.isNotEmpty()) {
        implementation(files(firmalifeLocalJars))
    } else {
        implementation(fg.deobf("curse.maven:firmalife-453394:$firmalifeCurseVersion"))
    }

    if (configuredLocalJars.files.isNotEmpty()) {
        compileOnly(files(configuredLocalJars))
    }
    if (forgeConfigScreensLocalJars.files.isNotEmpty()) {
        compileOnly(files(forgeConfigScreensLocalJars))
    }

    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-common-api:$jeiVersion"))
    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge-api:$jeiVersion"))
    runtimeOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge:$jeiVersion"))

    compileOnly(fg.deobf("vazkii.patchouli:Patchouli:$patchouliVersion"))
    runtimeOnly(fg.deobf("vazkii.patchouli:Patchouli:$patchouliVersion"))

    if (System.getProperty("idea.sync.active") != "true") {
        annotationProcessor("org.spongepowered:mixin:$mixinVersion:processor")
    }
}

minecraft {
    mappings("parchment", parchmentVersion)

    runs {
        all {
            args("-mixin.config=$modId.mixins.json")

            property("forge.logging.console.level", "debug")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "$projectDir/build/createSrgToMcp/output.srg")

            jvmArgs("-ea", "-Xmx4G", "-Xms2G")
            jvmArg("-XX:+AllowEnhancedClassRedefinition")

            mods.create(modId) {
                source(sourceSets.main.get())
            }
        }

        register("client") {
            workingDirectory(project.file("run/client"))
        }

        register("server") {
            workingDirectory(project.file("run/server"))
            arg("--nogui")
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
}

tasks.processResources {
    from(rootDir) {
        include("LICENSE", "DISCLAIMER.md")
        into("META-INF")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Firmalife Enhance",
            "Implementation-Version" to project.version,
            "Bundle-License" to "All Rights Reserved",
            "Bundle-Disclaimer" to "See META-INF/DISCLAIMER.md",
            "MixinConfigs" to "$modId.mixins.json"
        )
    }
}

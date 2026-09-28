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

// 稳定前置目录：沿用 skill 导航记录的 1.20 前置根目录（群峦前置源码/1.20），
// 不再指向会随时变动的 PCL 实例 mods 目录。
val prereqDir = providers.gradleProperty("prereqDir")
    .orElse("C:/Users/g1739/Desktop/群峦前置源码/1.20/前置jar")
    .get()

val tfcLocalJars = fileTree("$prereqDir/tfc") {
    include("*.jar")
    exclude("*-sources.jar", "*-javadoc.jar")
}
val firmalifeLocalJars = fileTree("$prereqDir/firmalife") {
    include("*.jar")
    exclude("*-sources.jar", "*-javadoc.jar")
}
val configuredLocalJars = fileTree("$prereqDir/configui") {
    include("configured-*.jar")
}
val forgeConfigScreensLocalJars = fileTree("$prereqDir/configui") {
    include("ForgeConfigScreens-*.jar")
}
val immersiveEngineeringLocalJars = fileTree("$prereqDir/immersiveengineering") {
    include("*.jar")
}

val modId = "firmalife_greenhouse_patch"
val modVersion = System.getenv("VERSION") ?: "1.0.0"

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
    if (immersiveEngineeringLocalJars.files.isNotEmpty()) {
        compileOnly(files(immersiveEngineeringLocalJars))
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
            // IgnoreUnrecognizedVMOptions 必须放在前面：JDK 17 不认识 AllowEnhancedClassRedefinition（JDK 21+ 才有）
            jvmArg("-XX:+IgnoreUnrecognizedVMOptions")
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
    doFirst {
        if (configuredLocalJars.isEmpty || forgeConfigScreensLocalJars.isEmpty || immersiveEngineeringLocalJars.isEmpty)
        {
            throw GradleException(
                "缺少可选编译前置。请把 configured-forge-1.20.1-*.jar 与 ForgeConfigScreens-v*-1.20.1-Forge.jar 放入 " +
                    "$prereqDir/configui，把 ImmersiveEngineering-1.20.1-*.jar 放入 $prereqDir/immersiveengineering（可用 -PprereqDir=<目录> 覆盖）。"
            )
        }
    }
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

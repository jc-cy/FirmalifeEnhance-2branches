plugins {
    id("net.neoforged.moddev") version "2.0.107"
}

val minecraftVersion = "1.21.1"
val neoForgeVersion = "21.1.234"
val parchmentVersion = "2024.11.17"
val parchmentMinecraftVersion = "1.21.1"
val patchouliVersion = "1.21.1-92-NEOFORGE"
// 稳定前置目录：与 1.20 同口径，放在 skill 导航记录的 1.21 前置根目录下，
// 不再指向会随时变动的 PCL 实例 mods 目录（实例被清理后构建仍可用）。
val prereqDir = providers.gradleProperty("prereqDir")
    .orElse("C:/Users/g1739/Desktop/群峦前置源码/1.21/前置jar")
    .get()
val jeiDevJars = fileTree("$prereqDir/jei") {
    include("*jei-1.21.1-neoforge-*.jar")
}
val tfcLocalJars = fileTree("$prereqDir/tfc") {
    include("*TerraFirmaCraft-NeoForge-1.21.1-*.jar")
}
val firmalifeLocalJars = fileTree("$prereqDir/firmalife") {
    include("*Firmalife-NeoForge-1.21.1-*.jar")
}

val modId = "firmalife_greenhouse_patch"
val modVersion = System.getenv("VERSION") ?: "1.0.0"
val modJavaVersion = "21"

group = "com.g1739.firmalifegreenhousepatch"
version = modVersion

base {
    archivesName.set("FirmalifeEnhance-NeoForge-$minecraftVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
}

repositories {
    mavenCentral()
    mavenLocal()
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com") }
        filter { includeGroup("vazkii.patchouli") }
    }
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") }
        filter { includeGroup("curse.maven") }
    }
}

neoForge {
    version = neoForgeVersion

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }

    runs {
        configureEach {
            jvmArguments.addAll("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition", "-ea")
        }
        register("client") {
            client()
            gameDirectory = file("run/client")
        }
        register("server") {
            server()
            gameDirectory = file("run/server")
            programArgument("--nogui")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    if (tfcLocalJars.isEmpty)
    {
        throw GradleException("未找到 TFC 4.2.9 依赖。请把 TerraFirmaCraft-NeoForge-1.21.1-4.2.9.jar 放入 $prereqDir/tfc，或通过 -PprereqDir=<目录> 指定。")
    }
    if (firmalifeLocalJars.isEmpty)
    {
        throw GradleException("未找到 Firmalife 3.0.14 依赖。请把 Firmalife-NeoForge-1.21.1-3.0.14.jar 放入 $prereqDir/firmalife，或通过 -PprereqDir=<目录> 指定。")
    }

    compileOnly(files(tfcLocalJars))
    runtimeOnly(files(tfcLocalJars))

    compileOnly(files(firmalifeLocalJars))
    runtimeOnly(files(firmalifeLocalJars))

    compileOnly(jeiDevJars)

    compileOnly("vazkii.patchouli:Patchouli:$patchouliVersion")
    runtimeOnly("vazkii.patchouli:Patchouli:$patchouliVersion")
}

tasks.processResources {
    from(rootDir) {
        include("LICENSE", "DISCLAIMER.md")
        into("META-INF")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    doFirst {
        if (jeiDevJars.isEmpty)
        {
            throw GradleException("未找到 JEI 开发依赖。请把 jei-1.21.1-neoforge-*.jar 放入 $prereqDir/jei，或通过 -PprereqDir=<目录> 指定。")
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Firmalife Enhance",
            "Implementation-Version" to project.version,
            "Bundle-License" to "All Rights Reserved",
            "Bundle-Disclaimer" to "See META-INF/DISCLAIMER.md"
        )
    }
}

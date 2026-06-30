plugins {
    id("net.neoforged.moddev") version "2.0.107"
}

val minecraftVersion = "1.21.1"
val neoForgeVersion = "21.1.197"
val parchmentVersion = "2024.11.17"
val parchmentMinecraftVersion = "1.21.1"
val patchouliVersion = "1.21.1-92-NEOFORGE"
val defaultLocalModsDir = "C:/Users/g1739/Desktop/PCL/.minecraft/versions/1.21.1-NeoForge_21.1.222/mods"
val localModsDir = providers.gradleProperty("localModsDir").orElse(defaultLocalModsDir).get()
val jeiDevJars = fileTree(localModsDir) {
    include("*jei-1.21.1-neoforge-*.jar")
}

val modId = "firmalife_greenhouse_patch"
val modVersion = System.getenv("VERSION") ?: "0.1.4"
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
    compileOnly("curse.maven:terrafirmacraft-302973:7452541")
    runtimeOnly("curse.maven:terrafirmacraft-302973:7452541")

    compileOnly("curse.maven:firmalife-453394:7790886")
    runtimeOnly("curse.maven:firmalife-453394:7790886")

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
            throw GradleException("未找到 JEI 开发依赖。请通过 -PlocalModsDir=<你的mods目录> 指向包含 jei-1.21.1-neoforge-*.jar 的目录。")
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

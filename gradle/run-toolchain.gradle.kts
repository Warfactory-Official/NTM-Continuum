import org.gradle.jvm.toolchain.JavaToolchainService

/**
 * Forces every game-launching `JavaExec` in this node to run on the node's declared Java toolchain.
 *
 * Gradle's own run tasks already do this. IntelliJ's "run main() through Gradle" action does not: it
 * generates a throwaway init script that creates its own `JavaExec` and hardcodes
 * `executable = <module SDK>/bin/java.exe`. The module SDK is inherited from the *project* SDK, so every
 * node gets the same JVM regardless of the toolchain the node declares — and in a multi-version project
 * no single JVM is correct for all of them.
 *
 * That is not a cosmetic mismatch. Minecraft 1.21.1 must run on Java 21: NeoForge 21.1.80 bundles Mixin
 * 0.8.7, whose ASM cannot read class-file major 69, so on Java 25 every JDK class it inspects fails to
 * load and mixin application dies with `ClassNotFoundException: java.lang.Boolean` — a message that
 * points nowhere near the actual problem. (Fabric survives the same mismatch only because its Mixin
 * ships a newer ASM. That is luck, not support.)
 *
 * Setting `executable` from `doFirst` rather than at configuration time is deliberate: it runs after the
 * init script's own configuration closure, so the node's toolchain wins no matter who configured what.
 */
fun Project.pinRunsToToolchain(javaVersion: Int) {
    val toolchains = extensions.getByType(JavaToolchainService::class.java)
    val launcher = toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(javaVersion)) }
    // Gradle JVM properties are not inherited by the Minecraft process. Keep the opt-in neutral so
    // `-Dhbm.gunSmokeTest=true` and `-PgunSmokeTest=true` reach only the client run that consumes it.
    val gunSmokeTest = providers.gradleProperty("gunSmokeTest")
        .orElse(providers.systemProperty("hbm.gunSmokeTest"))
        .map { if (it.isBlank()) "true" else it }

    tasks.withType(JavaExec::class.java).configureEach {
        // `this` is the JavaExec; inside doFirst the receiver is a plain Task, so hold on to it.
        val run = this
        // Both must be set, and consistently: Gradle rejects a task whose `executable` and
        // `javaLauncher` disagree. javaLauncher is a Property finalized before execution, so it is set
        // here at configuration time; executable is set in doFirst so it lands after whatever the IDE's
        // generated script configured.
        javaLauncher.set(launcher)
        if (name == "runClient") {
            gunSmokeTest.orNull?.let {
                systemProperty("hbm.gunSmokeTest", it)
                logger.lifecycle("Forwarding hbm.gunSmokeTest=$it to $path")
            }
        }
        doFirst {
            val wanted = launcher.get().executablePath.asFile.absolutePath
            val configured = run.executable
            if (configured != null && configured != wanted) {
                logger.lifecycle(
                    "Launching on the Java $javaVersion toolchain instead of $configured — " +
                        "this node's Minecraft cannot run on another major version.")
            }
            // setExecutable, not the property: the setter takes Object, so Kotlin sees only a getter.
            run.setExecutable(wanted)
        }
    }
}

pinRunsToToolchain((extra.properties["java_version"] as String).toInt())

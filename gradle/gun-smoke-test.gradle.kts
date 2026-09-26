// Gradle JVM properties are not inherited by the Minecraft process: `-Dhbm.gunSmokeTest=true` and
// `-PgunSmokeTest=true` reach only the client run that consumes it.
val gunSmokeTest = providers.gradleProperty("gunSmokeTest")
    .orElse(providers.systemProperty("hbm.gunSmokeTest"))
    .map { if (it.isBlank()) "true" else it }

tasks.withType(JavaExec::class.java).matching { it.name == "runClient" }.configureEach {
    gunSmokeTest.orNull?.let {
        systemProperty("hbm.gunSmokeTest", it)
        logger.lifecycle("Forwarding hbm.gunSmokeTest=$it to $path")
    }
}

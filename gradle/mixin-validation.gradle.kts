// Validates every mixin config against the classes that will actually ship in this node's jar.
//
// A mixin config lists its mixins by name, and nothing checks those names at compile time: a config
// entry with no matching class is a hard crash at class-transformation time ("The specified mixin
// 'x' was not found"), and a mixin class that no config lists silently does nothing. Neither shows up
// in `build`, and neither shows up in a dedicated-server GameTest run when the entry sits in the
// config's "client" section.
//
// The risk scales with this project's shape: the same shared configs are consumed by three Minecraft
// versions across two loaders, and Stonecutter guards mean a mixin class can exist on one node and
// not another. So this validates POST-processing artifacts rather than the source tree —
// build/resources/main holds exactly the configs that survive each node's processResources excludes,
// and build/classes/java/main holds exactly the classes that survived that node's Stonecutter guards.

import groovy.json.JsonSlurper

val validateMixinConfigs = tasks.register("validateMixinConfigs") {
    group = "verification"
    description = "Fails if a mixin config names a class that will not be in the jar."

    val resourcesDir = layout.buildDirectory.dir("resources/main")
    val classesDir = layout.buildDirectory.dir("classes/java/main")
    inputs.dir(resourcesDir).optional(true).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(classesDir).optional(true).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.upToDateWhen { true }

    doLast {
        val resources = resourcesDir.get().asFile
        val classes = classesDir.get().asFile
        val configs = resources.listFiles { f -> f.name.endsWith(".mixins.json") }?.sortedBy { it.name }.orEmpty()

        if (configs.isEmpty()) {
            logger.lifecycle("validateMixinConfigs: no mixin configs present, nothing to check.")
            return@doLast
        }

        val problems = mutableListOf<String>()
        // Every class declared across all configs, so we can spot mixin classes nobody references.
        val declared = mutableSetOf<String>()

        for (config in configs) {
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(config) as Map<String, Any?>
            val pkg = json["package"] as? String
            if (pkg == null) {
                problems += "${config.name}: no \"package\" field"
                continue
            }

            for (section in listOf("mixins", "client", "server")) {
                val entries = (json[section] as? List<*>).orEmpty().filterIsInstance<String>()
                for (entry in entries) {
                    val fqn = "$pkg.$entry"
                    declared += fqn
                    // Inner classes are declared as Outer$Inner; the file on disk matches that name.
                    val classFile = File(classes, fqn.replace('.', '/') + ".class")
                    if (!classFile.isFile) {
                        problems += "${config.name} [$section] declares '$fqn' but no such class is compiled " +
                                "into this node — the config entry is stale, or the class is behind a " +
                                "Stonecutter guard that excludes it from this node."
                    }
                }
            }
        }

        // Reverse direction: a mixin class nobody declares is a silent no-op, not a crash, so this
        // warns rather than fails — the package legitimately holds non-mixin helpers too.
        val mixinPackages = configs.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            (JsonSlurper().parse(it) as Map<String, Any?>)["package"] as? String
        }.distinct()
        for (pkg in mixinPackages) {
            val dir = File(classes, pkg.replace('.', '/'))
            if (!dir.isDirectory) continue
            dir.listFiles { f -> f.name.endsWith(".class") && !f.name.contains('$') }.orEmpty().forEach { f ->
                val fqn = "$pkg.${f.name.removeSuffix(".class")}"
                if (fqn !in declared) {
                    logger.warn("validateMixinConfigs: WARNING '$fqn' is compiled but no mixin config " +
                            "declares it — it will be silently inert.")
                }
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Mixin config validation failed for ${project.path}:\n  " + problems.joinToString("\n  ")
            )
        }
        logger.lifecycle("validateMixinConfigs: ${configs.size} config(s), ${declared.size} mixin(s), all resolved.")
    }
}

// Run as part of `check`, after the inputs it inspects exist.
validateMixinConfigs.configure { dependsOn(tasks.named("classes")) }
tasks.named("check") { dependsOn(validateMixinConfigs) }

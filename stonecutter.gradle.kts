plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-neoforge" /* [SC] DO NOT EDIT */

// Builds every node in one invocation. Stonecutter 0.9 replaced the old `registerChiseled` API with
// task aggregation: `stonecutter.tasks.named` returns a lazy collection of that task across all nodes.
tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds every registered version/loader node."
    dependsOn(stonecutter.tasks.named("build"))
}

stonecutter parameters {
    val loader = node.metadata.project.substringAfterLast('-')

    // Loader constants, used as `//? if fabric {` / `//? if forgelike {`.
    constants["fabric"] = loader == "fabric"
    constants["neoforge"] = loader == "neoforge"
    constants["forge"] = loader == "forge"
    constants["forgelike"] = loader == "neoforge" || loader == "forge"

    swaps["mc_version"] = "\"${node.metadata.version}\";"

    // Phase 1 will add the mechanical 26.1 -> older renames here, e.g.
    //   replacements { string(current.parsed < "1.21.11") { replace("Identifier", "ResourceLocation") } }
    // Declared in the 26.1 -> older direction because 26.1 is the source of truth for this port.
}

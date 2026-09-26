plugins {
    id("dev.golem.stonecutter")
}

stonecutter active "26.1-neoforge" /* [SC] DO NOT EDIT */

// `flywheel` = built against the published dev.engine_room artifact (none for 26.1; Core vendors the engine there).
golem.optional("jei", "flywheel")

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

// Uploads every node's jar to Modrinth and CurseForge (Golem-Core/gradle/mod/publish.gradle.kts). Add
// -Ppublish.dryRun=true to build and log the uploads without making them.
tasks.register("publishAllMods") {
    group = "publishing"
    description = "Publishes every version/loader node to Modrinth and CurseForge."
    dependsOn(stonecutter.tasks.named("publishMods"))
}

stonecutter parameters {
    val loader = node.metadata.project.substringAfterLast('-')

    // Loader constants, used as `//? if fabric {` / `//? if forgelike {`.
    constants["fabric"] = loader == "fabric"
    constants["neoforge"] = loader == "neoforge"
    constants["forge"] = loader == "forge"
    constants["forgelike"] = loader == "neoforge" || loader == "forge"

    swaps["mc_version"] = "\"${node.metadata.version}\";"

    // True only where the node declares JEI coordinates, so the integration compiles out elsewhere.
    constants["jei"] = !(node.project.findProperty("deps.jei") as String?).isNullOrBlank()
    constants["rei"] = !(node.project.findProperty("deps.rei") as String?).isNullOrBlank()
    // True only where the node declares CC: Tweaked coordinates (an optional peer, like JEI).
    constants["computercraft"] = !(node.project.findProperty("deps.cc") as String?).isNullOrBlank()
    // True only where the node declares KubeJS coordinates (an optional peer; dev.golem.core.kubejs in Golem Core).
    constants["kubejs"] = !(node.project.findProperty("deps.kubejs") as String?).isNullOrBlank()

    // Same shape for Flywheel, and false on both 26.1 nodes because the engine publishes no 26.1
    // build. That is not the same as having no engine there: Golem Core vendors a port of it for
    // both 26.1 nodes, which FlywheelCompat reaches through an entry point that exists on every node.
    // This constant means "built against the published dev.engine_room artifact" and nothing more.
    constants["flywheel"] = !(node.project.findProperty("deps.flywheel") as String?).isNullOrBlank()

    // Mechanical renames, declared 26.1 -> older because 26.1 is this port's source of truth.
    replacements {
        // Id construction changed twice across the supported range. Keep it behind Golem Core so
        // callers survive the Identifier -> ResourceLocation source rename without version branches.
        string(true) {
            replace("net.minecraft.resources.Identifier.fromNamespaceAndPath(", "dev.golem.core.Ids.of(")
            replace("Identifier.fromNamespaceAndPath(", "dev.golem.core.Ids.of(")
            replace("ResourceLocation.fromNamespaceAndPath(", "dev.golem.core.Ids.of(")
            replace("Identifier.parse(", "dev.golem.core.Ids.parse(")
            replace("ResourceLocation.parse(", "dev.golem.core.Ids.parse(")
            replace("Identifier.withDefaultNamespace(", "dev.golem.core.Ids.minecraft(")
            replace("ResourceLocation.withDefaultNamespace(", "dev.golem.core.Ids.minecraft(")
        }
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
        }
        string(current.parsed < "1.20.5") {
            // 1.20.1 has no registry-aware buffer. HBM's sync payload only writes primitives and
            // explicit ids, so the plain buffer is behaviorally equivalent for these call sites.
            replace("RegistryFriendlyByteBuf", "FriendlyByteBuf")
            // Vanilla renamed GRASS -> SHORT_GRASS in 1.20.5. The trailing `)` is load-bearing: a bare
            // `Blocks.GRASS` rule would also rewrite the four `Blocks.GRASS_BLOCK` uses into
            // `Blocks.SHORT_GRASS_BLOCK` on the way back up. Checked against the tree, not assumed.
            replace("Blocks.SHORT_GRASS)", "Blocks.GRASS)")
        }
        string(current.parsed < "1.21.1") {
            // Sound events became Holders. The trailing comma keeps the mapping injective — without it
            // the older spelling is a strict prefix of the newer one, which is the shape that produced
            // the `Identifier` mess.
            replace("SoundEvents.GENERIC_EXPLODE.value(),", "SoundEvents.GENERIC_EXPLODE,")
        }
        string(current.parsed < "26.1") {
            // 26.1 renamed the by-id registry lookup that returns the value itself; below it is `get`.
            // Qualified by registry and closed by `(`, so it cannot touch getKey/getOptional/getTags,
            // and `ITEM.get(` is not a substring of `ITEM.getValue(`.
            replace("BuiltInRegistries.ITEM.getValue(", "BuiltInRegistries.ITEM.get(")
        }
        string(current.parsed < "26.1") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
        }
        // 26.1 moved RenderType into a `rendertype` subpackage. Only the fully-qualified path is
        // replaced, never the bare type name: a substring swap of "RenderType" would also mangle
        // RenderTypes, CompatRenderTypes and any other symbol containing it.
        string(current.parsed < "26.1") {
            replace("net.minecraft.client.renderer.rendertype.RenderType", "net.minecraft.client.renderer.RenderType")
        }
        // Vanilla renames between 1.21.1 and 26.1. Each string below is distinctive enough that a
        // substring swap cannot hit an unrelated symbol — the lesson from "Identifier", which had to
        // become a helper because it appeared inside HBM's own class names.
        string(current.parsed < "26.1") {
            replace("getMinY()", "getMinBuildHeight()")
            replace("getMaxY()", "getMaxBuildHeight()")
            replace("MobEffects.SLOWNESS", "MobEffects.MOVEMENT_SLOWDOWN")
            replace("MobEffects.MINING_FATIGUE", "MobEffects.DIG_SLOWDOWN")
            // Older versions spell this with a doubled s. Injective in both directions: the 26.1
            // spelling never appears below, and vice versa.
            replace("noCollision()", "noCollission()")
            // 26.1 renamed ChunkPos.asLong to pack. Injective: neither spelling exists in the other era.
            replace("ChunkPos.pack(", "ChunkPos.asLong(")
            // 26.1 re-homed several vanilla animals into per-family subpackages. Spelled out per
            // class, not per package: Stonecutter rejects rules that are ambiguous in reverse, and
            // both `cow.` and `feline.` would map back onto plain `animal.`.
            replace("net.minecraft.world.entity.animal.cow.Cow", "net.minecraft.world.entity.animal.Cow")
            replace("net.minecraft.world.entity.animal.cow.MushroomCow", "net.minecraft.world.entity.animal.MushroomCow")
            replace("net.minecraft.world.entity.animal.equine.Horse", "net.minecraft.world.entity.animal.horse.Horse")
            replace("net.minecraft.world.entity.animal.equine.SkeletonHorse", "net.minecraft.world.entity.animal.horse.SkeletonHorse")
            replace("net.minecraft.world.entity.animal.equine.ZombieHorse", "net.minecraft.world.entity.animal.horse.ZombieHorse")
            replace("net.minecraft.world.entity.animal.feline.Cat", "net.minecraft.world.entity.animal.Cat")
            replace("net.minecraft.world.entity.animal.feline.Ocelot", "net.minecraft.world.entity.animal.Ocelot")
            replace("net.minecraft.world.entity.animal.chicken.Chicken", "net.minecraft.world.entity.animal.Chicken")
            replace("net.minecraft.world.entity.animal.squid.Squid", "net.minecraft.world.entity.animal.Squid")
            replace("net.minecraft.world.entity.animal.sheep.Sheep", "net.minecraft.world.entity.animal.Sheep")
            replace("net.minecraft.world.entity.vehicle.minecart.MinecartTNT", "net.minecraft.world.entity.vehicle.MinecartTNT")
            replace("net.minecraft.world.entity.projectile.arrow.Arrow", "net.minecraft.world.entity.projectile.Arrow")
            replace("net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg", "net.minecraft.world.entity.projectile.ThrownEgg")
            // Straight renames. Each 26.1 spelling is absent below, and each older spelling is absent
            // on 26.1, so the mapping is reversible.
            replace("EntitySpawnReason", "MobSpawnType")
            replace("SPAWN_ITEM_USE", "SPAWN_EGG")
            replace("VegetationBlock", "BushBlock")
            replace("getMinSectionY()", "getMinSection()")
            replace("snapTo(", "moveTo(")
            // More 26.1 per-family subpackages, spelled per class to stay reversible.
            replace("net.minecraft.world.entity.monster.zombie.Zombie", "net.minecraft.world.entity.monster.Zombie")
            replace("net.minecraft.world.entity.monster.skeleton.AbstractSkeleton", "net.minecraft.world.entity.monster.AbstractSkeleton")
            replace("net.minecraft.world.entity.npc.villager.Villager", "net.minecraft.world.entity.npc.Villager")
            // 26.1 moved the minecart family into its own subpackage. Spelled with the full path, as
            // with the animals above: the bare type name appears inside AbstractMinecartContainer and
            // every NTM cart class, so only the qualified form is reversible.
            replace("net.minecraft.world.entity.vehicle.minecart.AbstractMinecart", "net.minecraft.world.entity.vehicle.AbstractMinecart")
            // Only the PART moved into the enderdragon subpackage; EnderDragon itself lives there on
            // every target, so the rule has to carry the full `EnderDragonPart` name to stay injective.
            replace("net.minecraft.world.entity.boss.enderdragon.EnderDragonPart", "net.minecraft.world.entity.boss.EnderDragonPart")
            // Screen label pass: 26.1 renamed renderLabels when screen drawing became an extract pass.
            replace("extractLabels(", "renderLabels(")
            replace("getCraftingRemainder()", "getCraftingRemainingItem()")
            replace("MobEffects.INSTANT_DAMAGE", "MobEffects.HARM")
            replace("InteractionResult.TRY_WITH_EMPTY_HAND", "InteractionResult.PASS")
            replace("MobEffects.NAUSEA", "MobEffects.CONFUSION")
            replace("MobEffects.SPEED", "MobEffects.MOVEMENT_SPEED")
            replace("SoundEvents.GENERIC_EAT.value()", "SoundEvents.GENERIC_EAT")
            replace("SoundEvents.CHICKEN_STEP.value()", "SoundEvents.CHICKEN_STEP")
            replace("ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND", "ClientboundGameEventPacket.ARROW_HIT_PLAYER")
            // Fabric API's own rename, not vanilla's. Qualified with the owning type so the rule can
            // only ever hit these calls, and reversible: neither spelling exists in the other era.
            replace("PayloadTypeRegistry.clientboundPlay()", "PayloadTypeRegistry.playS2C()")
            replace("PayloadTypeRegistry.serverboundPlay()", "PayloadTypeRegistry.playC2S()")
        }
    }

}

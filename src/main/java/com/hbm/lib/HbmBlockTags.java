package com.hbm.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class HbmBlockTags {

    /**
     * 1.7 {@code IShearable}: what an ability tool with shears may claim on top of what it is effective on.
     * Membership is 1.7 vanilla's implementors of that interface.
     */
    public static final TagKey<Block> SHEARABLE = TagKey.create(
            Registries.BLOCK,
            Library.id("shearable"));

    /**
     * Blocks 1.7 built on {@code Material.sand}; membership from the 1.7 REGISTRATIONS.
     */
    public static final TagKey<Block> MATERIAL_SAND = TagKey.create(
            Registries.BLOCK,
            Library.id("material_sand"));

    /**
     * Blocks 1.7 built on {@code Material.cloth}; membership from the 1.7 REGISTRATIONS.
     */
    public static final TagKey<Block> MATERIAL_CLOTH = TagKey.create(
            Registries.BLOCK,
            Library.id("material_cloth"));

    /**
     * Blocks 1.7 built on {@code Material.ground}; membership from the 1.7 REGISTRATIONS.
     */
    public static final TagKey<Block> MATERIAL_GROUND = TagKey.create(
            Registries.BLOCK,
            Library.id("material_ground"));

    /**
     * The ground a groundwater pump (steam and electric) counts as valid below itself (1.7
     * {@code TileEntityMachinePumpBase.validBlocks}, a fixed set there); an addon adds its planets' soils.
     */
    public static final TagKey<Block> PUMP_GROUND = TagKey.create(
            Registries.BLOCK,
            Library.id("pump_ground"));

    /** The stone 1.7's veins, strata and the coltan deposit replace by default (1.7 stone, plus deepslate and tuff). */
    public static final TagKey<Block> LEGACY_STONE = TagKey.create(
            Registries.BLOCK,
            Library.id("legacy_stone"));

    /** What the tar-sand bubble replaces by default (1.7 {@code Blocks.sand}, both metadata). */
    public static final TagKey<Block> TAR_SAND_HOSTS = TagKey.create(
            Registries.BLOCK,
            Library.id("tar_sand_hosts"));

    public static final TagKey<Block> FLEIJA_IMMUNE = TagKey.create(
            Registries.BLOCK,
            Library.id("fleija_immune"));

    /**
     * Blocks 1.7 built on a setTranslucent() material; membership from the 1.7 REGISTRATIONS.
     */
    public static final TagKey<Block> BEAM_TRANSLUCENT = TagKey.create(
            Registries.BLOCK,
            Library.id("beam_translucent"));

    /**
     * What the mining laser unloads into; 1.7 names the seven inline
     * ({@code TileEntityMachineMiningLaser.tryFillContainer}), leaving {@code crate_tungsten} out.
     */
    public static final TagKey<Block> MINING_LASER_UNLOAD = TagKey.create(
            Registries.BLOCK,
            Library.id("mining_laser_unload"));

    /**
     * 1.7 {@code BlockDepth} (and {@code BlockDepthOre}, which extends it): the excavator stops rather than
     * cutting one. Membership is whatever {@code ModBlocks.depthBlock} mints.
     */
    public static final TagKey<Block> DEPTH_ROCK = TagKey.create(
            Registries.BLOCK,
            Library.id("depth_rock"));

    /**
     * What the ancient tomb's cladding pass leaves standing; 1.7 names the six inline
     * ({@code AncientTomb} L118, repeated L136). Air is the consumer's own test, not a member.
     */
    public static final TagKey<Block> ANCIENT_TOMB_MATERIAL = TagKey.create(
            Registries.BLOCK,
            Library.id("ancient_tomb_material"));

    /**
     * What a safe-mode meteorite may still overwrite; 1.7 {@code Meteorite.generateReplacables} names six,
     * whose {@code ore_meteor} is one meta-block -> the five ores here.
     */
    public static final TagKey<Block> METEORITE_REPLACEABLE = TagKey.create(
            Registries.BLOCK,
            Library.id("meteorite_replaceable"));

    /**
     * What the FBI cuts through on its raid swing; 1.7 {@code EntityFBI} L147-167 names the nineteen.
     */
    public static final TagKey<Block> FBI_BREAKABLE = TagKey.create(
            Registries.BLOCK,
            Library.id("fbi_breakable"));

    /**
     * 1.7 {@code Material.fire}: what foam puts out and boron sand hisses over ({@code XFactoryTool} L116, L175).
     */
    public static final TagKey<Block> EXTINGUISHABLE_FIRE = TagKey.create(
            Registries.BLOCK,
            Library.id("extinguishable_fire"));

    /**
     * What 1.7 {@code ModEventHandlerImpact.populateChunkPost} clears beyond {@code BlockBush}, which is the
     * consumer's {@code VegetationBlock} test: {@code BlockLog}, {@code BlockLeaves}, {@code Material.leaves},
     * the rest of {@code Material.plants}.
     */
    public static final TagKey<Block> IMPACT_CLEARED = TagKey.create(
            Registries.BLOCK,
            Library.id("impact_cleared"));

    /** Blocks that seal radiation besides the {@code IRadResistantBlock} classes; empty, as in 1.7. */
    public static final TagKey<Block> RADIATION_RESISTANT = TagKey.create(
            Registries.BLOCK,
            Library.id("radiation_resistant"));

    private HbmBlockTags() {
    }
}

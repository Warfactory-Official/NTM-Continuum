package com.hbm.blocks.fluid;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.items.ModFluidNetItems;
import com.hbm.registration.Presets;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.fluid.WorldFluids;
import dev.golem.core.fluid.WorldFluids.Physics;
import dev.golem.core.fluid.WorldFluids.Spec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.7 world fluids ({@code ModBlocks} 2305-2335): the Forge {@code Fluid}s 1.7 registered for its placeable
 * fluid blocks, as Golem Core {@link WorldFluids}. Each value is the 1.7 one, from the {@code Fluid} setters, the
 * block class's {@code setQuantaPerBlock}/{@code tickRate}, its block {@code Material} (the physics) and its
 * {@code getRenderBlockPass} (translucent unless the class returns pass 0). The block ids stay the 1.7 block names;
 * the fluids take 1.7's Forge fluid names ({@code hbm:mud_fluid}, flowing {@code hbm:flowing_mud_fluid}), which no
 * HBM network fluid ({@code hbm:<FluidType key>}) uses.
 *
 * <p>These are world blocks, not the duct network's {@code FluidType}s. 1.7 linked three buckets to network fluids
 * through its {@code FluidContainerRegistry} (mud: WATZ, schrabidic: SCHRABIDIC, sulfuric: SULFURIC_ACID); that
 * link is unchanged ({@code FluidContainers}).
 */
public final class ModWorldFluids {

    private ModWorldFluids() {}

    private static final String NS = Constants.MOD_ID;

    /** 1.7 MudFluid 2500/3000/5/2773, MudBlock quanta 4, tickRate 15, material fluidmud (no physics), pass 1. */
    public static final WorldFluids.Handle MUD = WorldFluids.declare(NS, Spec.of("mud_fluid")
                    .name("fluid.hbm.mud_fluid", "Poisonous Mud").sprites("hbm:blocks/mud_still", "hbm:blocks/mud_flowing")
                    .density(2500).viscosity(3000).luminosity(5).temperature(2773).quanta(4).tickDelay(15),
            () -> ModHazardBlocks.mud_block.get(), () -> ModFluidNetItems.bucket_mud.get());

    /** 1.7 AcidFluid 2500/1500/5/2773, AcidBlock quanta 4, tickRate 5, material fluidacid, pass 1. */
    public static final WorldFluids.Handle ACID = WorldFluids.declare(NS, Spec.of("acid_fluid")
                    .name("fluid.hbm.acid_fluid", "Acid").sprites("hbm:blocks/acid_still", "hbm:blocks/acid_flowing")
                    .density(2500).viscosity(1500).luminosity(5).temperature(2773).quanta(4).tickDelay(5),
            () -> ModFluidNetBlocks.acid_block.get(), () -> ModFluidNetItems.bucket_acid.get());

    /** 1.7 ToxicFluid 2500/2000/15/2773, ToxicBlock quanta 4, tickRate 15, material fluidtoxic, pass 1. */
    public static final WorldFluids.Handle TOXIC = WorldFluids.declare(NS, Spec.of("toxic_fluid")
                    .name("fluid.hbm.toxic_fluid", "Stereotypical Green Ooze").sprites("hbm:blocks/toxic_still", "hbm:blocks/toxic_flowing")
                    .density(2500).viscosity(2000).luminosity(15).temperature(2773).quanta(4).tickDelay(15),
            () -> ModFluidNetBlocks.toxic_block.get(), () -> ModFluidNetItems.bucket_toxic.get());

    /** 1.7 SchrabidicFluid 31200/500/-/273, SchrabidicBlock quanta 4, tickRate 15, material fluidschrabidic, pass 0. */
    public static final WorldFluids.Handle SCHRABIDIC = WorldFluids.declare(NS, Spec.of("schrabidic_fluid")
                    .name("fluid.hbm.schrabidic_fluid", "Schrabidic Acid")
                    .sprites("hbm:blocks/schrabidic_acid_still", "hbm:blocks/schrabidic_acid_flowing")
                    .density(31200).viscosity(500).temperature(273).quanta(4).tickDelay(15).translucent(false),
            () -> ModFluidNetBlocks.schrabidic_block.get(), () -> ModFluidNetItems.bucket_schrabidic_acid.get());

    /**
     * 1.7 GenericFluid("sulfuric_acid_fluid") 1840/1000/-/273, GenericFluidBlock (BlockFluidClassic defaults: quanta 8,
     * tickRate 1000 / 200 = 5), Material.water, pass 1.
     */
    public static final WorldFluids.Handle SULFURIC_ACID = WorldFluids.declare(NS, Spec.of("sulfuric_acid_fluid")
                    .name("fluid.hbm.sulfuric_acid_fluid", "Sulfuric Acid")
                    .sprites("hbm:blocks/sulfuric_acid_still", "hbm:blocks/sulfuric_acid_flowing")
                    .density(1840).viscosity(1000).temperature(273).physics(Physics.WATER),
            () -> ModFluidNetBlocks.sulfuric_acid_block.get(), () -> ModFluidNetItems.bucket_sulfuric_acid.get());

    /** 1.7 CoriumFluid 600000/12000/10/1500, CoriumFinite (BlockFluidFinite) quanta 5, tickRate 30, fluidcorium, pass 0. */
    public static final WorldFluids.Handle CORIUM = WorldFluids.declare(NS, Spec.of("corium_fluid")
                    .name("fluid.hbm.corium_fluid", "Corium").sprites("hbm:blocks/corium_still", "hbm:blocks/corium_flowing")
                    .density(600000).viscosity(12000).luminosity(10).temperature(1500).quanta(5).finite().tickDelay(30)
                    .translucent(false),
            () -> ModFluidNetBlocks.corium_block.get(), null);

    /** 1.7 VolcanicFluid lum 15, 3000/3000/1300, VolcanicBlock quanta 4 (tickRate 3000 / 200 = 15), Material.lava, pass 0. */
    public static final WorldFluids.Handle VOLCANIC_LAVA = WorldFluids.declare(NS, Spec.of("volcanic_lava_fluid")
                    .name("fluid.hbm.volcanic_lava_fluid", "Volcanic Lava")
                    .sprites("hbm:blocks/volcanic_lava_still", "hbm:blocks/volcanic_lava_flowing")
                    .density(3000).viscosity(3000).luminosity(15).temperature(1300).quanta(4).physics(Physics.LAVA)
                    .translucent(false),
            () -> ModFluidNetBlocks.volcanic_lava_block.get(), null);

    /** 1.7 RadFluid, the same values as VolcanicFluid; RadBlock extends VolcanicBlock. */
    public static final WorldFluids.Handle RAD_LAVA = WorldFluids.declare(NS, Spec.of("rad_lava_fluid")
                    .name("fluid.hbm.rad_lava_fluid", "Volcanic Lava")
                    .sprites("hbm:blocks/rad_lava_still", "hbm:blocks/rad_lava_flowing")
                    .density(3000).viscosity(3000).luminosity(15).temperature(1300).quanta(4).physics(Physics.LAVA)
                    .translucent(false),
            () -> ModFluidNetBlocks.rad_lava_block.get(), null);

    /** Loads the declarations; called before the registries are flushed. */
    public static void init() {}

    /**
     * Golem Core's world-fluid block properties (1.7 {@code BlockFluidBase}: no collision or shape, hardness 100, no
     * drops, the fluid's level-scaled luminosity), 1.7's {@code setResistance(500)} and its material's replaceability.
     */
    public static BlockBehaviour.Properties properties(
            BlockBehaviour.Properties p, WorldFluids.Handle fluid, boolean replaceable) {
        p = FluidBlock.properties(p, fluid).strength(100.0F, 500.0F);
        return replaceable ? p.replaceable() : p;
    }

    /**
     * A world fluid's blockstate: every level shows one model that only names the particle sprite (the fluid
     * renderer draws the fluid itself, as vanilla's water model does); no item, no drops.
     */
    public static Presets.BlockModel blockstate(String texture) {
        return Presets.shape(Models.parented("minecraft:block/block",
                        Slots.of("particle", "hbm:blocks/" + texture + "_still")))
                .noItem();
    }

    /** The 1.7 {@code Entity.setInWeb()} these fluids call on contact; 26.1 spells it as a movement multiplier. */
    static void setInWeb(Entity entity, BlockState state) {
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
    }

    /**
     * 1.7 {@code getMaterial().isLiquid()} for another material than {@code self}'s: vanilla water or lava, or another
     * world fluid. The fluid's own flowing blocks are the same block, as they were the same material in 1.7.
     */
    static boolean isForeignLiquid(Level level, BlockPos pos,
                                   Block self) {
        BlockState state = level.getBlockState(pos);
        return !state.is(self) && FluidBlock.isLiquid(state);
    }
}

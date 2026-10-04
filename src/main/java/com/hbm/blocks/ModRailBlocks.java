package com.hbm.blocks;

import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.blocks.rail.BlockRailNTM;
import com.hbm.blocks.rail.RailNarrowCurve;
import com.hbm.blocks.rail.RailNarrowStraight;
import com.hbm.blocks.rail.RailStandardBuffer;
import com.hbm.blocks.rail.RailStandardCurveBase;
import com.hbm.blocks.rail.RailStandardCurveWide7;
import com.hbm.blocks.rail.RailStandardCurveWide9;
import com.hbm.blocks.rail.RailStandardRamp;
import com.hbm.blocks.rail.RailStandardStraight;
import com.hbm.blocks.rail.RailStandardStraightShort;
import com.hbm.blocks.rail.RailStandardSwitch;
import com.hbm.blocks.rail.RailStandardSwitchFlipped;
import com.hbm.creativetabs.HbmTab;
import com.hbm.tileentity.BlockEntityRail;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * The folded standard/narrow-gauge rails the ntm-next train entities run on (ntm-next's
 * {@code ModBlocks.RAIL_LARGE_*}/{@code RAIL_NARROW_*} entries; ids, strengths and the rail family are as
 * there). 1.7 gave this family no creative tab entry beyond the item; the items sit in the machines tab.
 *
 * <p>The renderer and Dejson declarations live beside this registration: the folded core owns one
 * {@code BlockEntityRail}, and its client BER draws the source OBJ across the full footprint. The
 * source-faithful meshes, skins, and machine_obj item definitions are emitted by
 * {@code port-tools/gen_rail_assets.py}.
 */
public final class ModRailBlocks {

    private ModRailBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    /** Typed register(s): REG picks them without naming a registry key, which 1.20.1 cannot do in the build-time Dejson JVM. */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES);

    /**
     * The blockstate is deliberately empty: the rail BER owns the OBJ, while the state still has to enumerate
     * every {@code cell/facing/waterlogged} combination so the client never falls back to a missing model.
     */
    private static final Preset RAIL = Presets.rendered(Variants.product(
            Variants.uniform("cell", "false", "true"),
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("waterlogged", "false", "true")));

    public static final List<RegistryObject<? extends BlockRailNTM>> RAILS = new ArrayList<>();

    private static <B extends BlockRailNTM> RegistryObject<B> rail(String name,
            java.util.function.Function<BlockBehaviour.Properties, B> factory) {
        RegistryObject<B> block = REG.block(name, factory,
                        p -> p.strength(5.0F, 6.0F).noOcclusion().requiresCorrectToolForDrops())
                .transform(RAIL)
                // 1.7 rails are BlockDummyable(Material.iron) with no drop override: BlockDummyable.onBlockHarvested
                // drops the block itself once per break, whichever part was mined (pickaxe needed).
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
        REG.blockItem(block).tab(HbmTab.MACHINES);
        RAILS.add(block);
        return block;
    }

    public static final RegistryObject<RailStandardStraight> rail_large_straight =
            rail("rail_large_straight", RailStandardStraight::new);
    public static final RegistryObject<RailStandardStraightShort> rail_large_straight_short =
            rail("rail_large_straight_short", RailStandardStraightShort::new);
    public static final RegistryObject<RailStandardCurveBase> rail_large_curve =
            rail("rail_large_curve", RailStandardCurveBase::new);
    public static final RegistryObject<RailStandardCurveWide7> rail_large_curve_7 =
            rail("rail_large_curve_7", RailStandardCurveWide7::new);
    public static final RegistryObject<RailStandardCurveWide9> rail_large_curve_9 =
            rail("rail_large_curve_9", RailStandardCurveWide9::new);
    public static final RegistryObject<RailStandardRamp> rail_large_ramp =
            rail("rail_large_ramp", RailStandardRamp::new);
    public static final RegistryObject<RailStandardBuffer> rail_large_buffer =
            rail("rail_large_buffer", RailStandardBuffer::new);
    public static final RegistryObject<RailStandardSwitch> rail_large_switch =
            rail("rail_large_switch", RailStandardSwitch::new);
    public static final RegistryObject<RailStandardSwitchFlipped> rail_large_switch_flipped =
            rail("rail_large_switch_flipped", RailStandardSwitchFlipped::new);
    public static final RegistryObject<RailNarrowStraight> rail_narrow_straight =
            rail("rail_narrow_straight", RailNarrowStraight::new);
    public static final RegistryObject<RailNarrowCurve> rail_narrow_curve =
            rail("rail_narrow_curve", RailNarrowCurve::new);

    /** One block entity type for the family: every rail carries the switch state (1.7 TileEntityRailSwitch). */
    public static final RegistryObject<BlockEntityType<BlockEntityRail>> RAIL_ENTITY =
            REG.blockEntity("rail", () -> BlockEntities.type(BlockEntityRail::new,
                    rail_large_straight.get(), rail_large_straight_short.get(), rail_large_curve.get(),
                    rail_large_curve_7.get(), rail_large_curve_9.get(), rail_large_ramp.get(), rail_large_buffer.get(),
                    rail_large_switch.get(), rail_large_switch_flipped.get(), rail_narrow_straight.get(),
                    rail_narrow_curve.get()));

    public static void init() {}
}

package com.hbm.blocks;

import com.hbm.blocks.fluid.ModWorldFluids;
import com.hbm.blocks.generic.BlockMud;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockHazard;
import com.hbm.blocks.generic.BlockHazard.DisplayEffect;
import com.hbm.blocks.generic.BlockRadResistant;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.hazard.HazardSystem;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The radioactive / hazardous blocks (1.12.2 {@code BlockHazard}) + the radiation‑sealing blocks
 * ({@code BlockRadResistant}). Hand‑registered (the generator skips {@code new BlockHazard(}/{@code
 * BlockRadResistant(} so there's no id clash). The generator only emits their <b>assets</b> via its
 * {@code HAZARD_BLOCK_IDS} pass. Radiation values are assigned in {@link #initHazards()} <b>after</b>
 * {@code HazardRegistry.registerItems()} (which registers the block‑items' RADIATION hazards), mirroring
 * the old {@code initializeHazardsAndPlacables -> getRawRadsFromBlock} two‑step.
 */
public final class ModHazardBlocks {

    private ModHazardBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** A storage/building block: one texture all round (item model via the block), drops itself, pickaxe. */
    private static final Preset STORAGE = Presets.cubeAll().viaItemModel()
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));

    /** Every hazard block (for the initHazards radiation‑value pass). */
    public static final List<RegistryObject<? extends Block>> HAZARD_BLOCKS = new ArrayList<>();

    /** Watz meltdown sludge, placed by the reactor, never crafted, so it has no block item. */
    // The 1.7 MudBlock: a Golem Core world fluid (com.hbm.blocks.fluid.ModWorldFluids.MUD), material fluidmud
    // (replaceable), setResistance(500).
    public static final RegistryObject<BlockMud> mud_block = REG.block(
                    "mud_block", BlockMud::new,
                    p -> ModWorldFluids.properties(p, ModWorldFluids.MUD, true))
            .transform(ModWorldFluids.blockstate("mud"));

    // Radioactive metal storage blocks
    public static final RegistryObject<BlockHazard> block_uranium = hazard("block_uranium").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_u233 = hazard("block_u233").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_u235 = hazard("block_u235").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_u238 = hazard("block_u238").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_thorium = hazard("block_thorium").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_plutonium = hazard("block_plutonium").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_pu238 = hazard("block_pu238").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_pu239 = hazard("block_pu239").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_pu240 = hazard("block_pu240").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_pu_mix = hazard("block_pu_mix", DisplayEffect.RADFOG).transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_neptunium = hazard("block_neptunium").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_polonium = hazard("block_polonium").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_ra226 = hazard("block_ra226").transform(STORAGE);
    // 1.7 block_actinium = new BlockHazard().makeBeaconable() (resistance 10); beaconable through minecraft:beacon_base_blocks
    public static final RegistryObject<BlockHazard> block_actinium = hazard("block_actinium").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_au198 = hazard("block_au198")
            .transform(Presets.cubeAll("minecraft:block/gold_block").viaItemModel()).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockHazard> block_schrabidium = hazard("block_schrabidium", 300F).transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_solinium = hazard("block_solinium", 300F).transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_schrabidate = hazard("block_schrabidate", 300F).transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_schraranium = hazard("block_schraranium", 300F).transform(STORAGE);

    // Reactor fuel blocks
    public static final RegistryObject<BlockHazard> block_uranium_fuel = hazard("block_uranium_fuel").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_plutonium_fuel = hazard("block_plutonium_fuel").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_thorium_fuel = hazard("block_thorium_fuel").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_mox_fuel = hazard("block_mox_fuel").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_schrabidium_fuel = hazard("block_schrabidium_fuel", 300F).transform(STORAGE);

    // Specials
    public static final RegistryObject<BlockHazard> block_trinitite = hazard("block_trinitite").transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_white_phosphorus = hazard("block_white_phosphorus", DisplayEffect.FLAMES).transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_magnetized_tungsten = hazard("block_magnetized_tungsten", 35F).transform(STORAGE);
    public static final RegistryObject<BlockHazard> block_corium = REG.block("block_corium",
                    p -> new BlockHazard(p, DisplayEffect.LAVAPOP),
                    p -> p.strength(100.0F, 9000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().randomTicks())
            .transform(STORAGE);

    // Radiation-sealing blocks (IRadResistantBlock)
    public static final RegistryObject<BlockRadResistant> block_lead = radResistant("block_lead").transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> block_boron = radResistant("block_boron").transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> brick_concrete = radResistant("brick_concrete", 15F, 160F).transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> reinforced_brick = radResistant("reinforced_brick", 25F, 300F).transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> cmb_brick_reinforced = radResistant("cmb_brick_reinforced", 50F, 900F).transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> brick_concrete_mossy = radResistant("brick_concrete_mossy", 15F, 160F).transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> brick_compound = radResistant("brick_compound", 15F, 400F).transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> ducrete = radResistant("ducrete", 20F, 500F).transform(STORAGE);
    public static final RegistryObject<BlockRadResistant> ducrete_smooth = radResistant("ducrete_smooth", 20F, 500F).transform(STORAGE);

    static {
        blockItem(block_corium);
        HAZARD_BLOCKS.add(block_corium);
    }

    // Helpers
    private static Reg.Handle<BlockHazard> hazard(String id) { return hazard(id, DisplayEffect.NONE, 10F); }
    private static Reg.Handle<BlockHazard> hazard(String id, float resistance) { return hazard(id, DisplayEffect.NONE, resistance); }
    private static Reg.Handle<BlockHazard> hazard(String id, DisplayEffect fx) { return hazard(id, fx, 10F); }

    private static Reg.Handle<BlockHazard> hazard(String id, DisplayEffect fx, float resistance) {
        Reg.Handle<BlockHazard> block = REG.block(id,
                p -> new BlockHazard(p, fx),
                p -> p.strength(5.0F, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops().randomTicks());
        blockItem(block);
        HAZARD_BLOCKS.add(block);
        return block;
    }

    private static Reg.Handle<BlockRadResistant> radResistant(String id) { return radResistant(id, 5F, 10F); }

    private static Reg.Handle<BlockRadResistant> radResistant(String id, float hardness, float resistance) {
        Reg.Handle<BlockRadResistant> block = REG.block(id, BlockRadResistant::new,
                p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops());
        blockItem(block);
        return block;
    }

    private static void blockItem(RegistryObject<? extends Block> block) {
        REG.blockItem(block, p -> Compat.blockDescription(p)).tab(HbmTab.BLOCKS);
    }

    // Each hazard block's radiation is the hbm:radiation_emission block data map, read by BlockHazard at use time (1.7
    // copied the block item's RADIATION hazard into the block once, at start-up; corium's 150,000 area pulse is there too).

    public static void init() {}
}

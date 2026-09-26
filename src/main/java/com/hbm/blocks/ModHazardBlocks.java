package com.hbm.blocks;

import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockHazard;
import com.hbm.blocks.generic.BlockHazard.DisplayEffect;
import com.hbm.blocks.generic.BlockRadResistant;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
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
 * BlockRadResistant(} so there's no id clash) — the generator only emits their <b>assets</b> via its
 * {@code HAZARD_BLOCK_IDS} pass. Radiation values are assigned in {@link #initHazards()} <b>after</b>
 * {@code HazardRegistry.registerItems()} (which registers the block‑items' RADIATION hazards), mirroring
 * the old {@code initializeHazardsAndPlacables → getRawRadsFromBlock} two‑step.
 */
public final class ModHazardBlocks {

    private ModHazardBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Every hazard block (for the initHazards radiation‑value pass). */
    public static final List<RegistryObject<? extends Block>> HAZARD_BLOCKS = new ArrayList<>();

    /** Watz meltdown sludge — placed by the reactor, never crafted, so it has no block item. */
    public static final RegistryObject<com.hbm.blocks.generic.BlockMud> mud_block = BLOCKS.registerBlock(
            "mud_block", com.hbm.blocks.generic.BlockMud::new,
            p -> p.strength(1.0F, 500.0F).sound(SoundType.SLIME_BLOCK).noOcclusion());

    // ---- radioactive metal storage blocks ----
    public static final RegistryObject<BlockHazard> block_uranium = hazard("block_uranium");
    public static final RegistryObject<BlockHazard> block_u233 = hazard("block_u233");
    public static final RegistryObject<BlockHazard> block_u235 = hazard("block_u235");
    public static final RegistryObject<BlockHazard> block_u238 = hazard("block_u238");
    public static final RegistryObject<BlockHazard> block_thorium = hazard("block_thorium");
    public static final RegistryObject<BlockHazard> block_plutonium = hazard("block_plutonium");
    public static final RegistryObject<BlockHazard> block_pu238 = hazard("block_pu238");
    public static final RegistryObject<BlockHazard> block_pu239 = hazard("block_pu239");
    public static final RegistryObject<BlockHazard> block_pu240 = hazard("block_pu240");
    public static final RegistryObject<BlockHazard> block_pu_mix = hazard("block_pu_mix", DisplayEffect.RADFOG);
    public static final RegistryObject<BlockHazard> block_neptunium = hazard("block_neptunium");
    public static final RegistryObject<BlockHazard> block_polonium = hazard("block_polonium");
    public static final RegistryObject<BlockHazard> block_ra226 = hazard("block_ra226");
    public static final RegistryObject<BlockHazard> block_au198 = hazard("block_au198");
    public static final RegistryObject<BlockHazard> block_schrabidium = hazard("block_schrabidium", 300F);
    public static final RegistryObject<BlockHazard> block_solinium = hazard("block_solinium", 300F);
    public static final RegistryObject<BlockHazard> block_schrabidate = hazard("block_schrabidate", 300F);
    public static final RegistryObject<BlockHazard> block_schraranium = hazard("block_schraranium", 300F);

    // ---- reactor fuel blocks ----
    public static final RegistryObject<BlockHazard> block_uranium_fuel = hazard("block_uranium_fuel");
    public static final RegistryObject<BlockHazard> block_plutonium_fuel = hazard("block_plutonium_fuel");
    public static final RegistryObject<BlockHazard> block_thorium_fuel = hazard("block_thorium_fuel");
    public static final RegistryObject<BlockHazard> block_mox_fuel = hazard("block_mox_fuel");
    public static final RegistryObject<BlockHazard> block_schrabidium_fuel = hazard("block_schrabidium_fuel", 300F);

    // ---- specials ----
    public static final RegistryObject<BlockHazard> block_trinitite = hazard("block_trinitite");
    public static final RegistryObject<BlockHazard> block_white_phosphorus = hazard("block_white_phosphorus", DisplayEffect.FLAMES);
    public static final RegistryObject<BlockHazard> block_magnetized_tungsten = hazard("block_magnetized_tungsten", 35F);
    public static final RegistryObject<BlockHazard> block_corium = BLOCKS.registerBlock("block_corium",
            p -> new BlockHazard(p, DisplayEffect.LAVAPOP),
            p -> p.strength(100.0F, 9000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().randomTicks());

    // ---- radiation‑sealing blocks (IRadResistantBlock) ----
    public static final RegistryObject<BlockRadResistant> block_lead = radResistant("block_lead");
    public static final RegistryObject<BlockRadResistant> block_boron = radResistant("block_boron");
    public static final RegistryObject<BlockRadResistant> brick_concrete = radResistant("brick_concrete", 15F, 160F);
    public static final RegistryObject<BlockRadResistant> reinforced_brick = radResistant("reinforced_brick", 25F, 300F);
    public static final RegistryObject<BlockRadResistant> cmb_brick_reinforced = radResistant("cmb_brick_reinforced", 50F, 900F);
    public static final RegistryObject<BlockRadResistant> brick_concrete_mossy = radResistant("brick_concrete_mossy", 15F, 160F);
    public static final RegistryObject<BlockRadResistant> brick_compound = radResistant("brick_compound", 15F, 400F);
    public static final RegistryObject<BlockRadResistant> ducrete = radResistant("ducrete", 20F, 500F);
    public static final RegistryObject<BlockRadResistant> ducrete_smooth = radResistant("ducrete_smooth", 20F, 500F);

    static {
        blockItem("block_corium", block_corium);
        HAZARD_BLOCKS.add(block_corium);
    }

    // ---- helpers ----
    private static RegistryObject<BlockHazard> hazard(String id) { return hazard(id, DisplayEffect.NONE, 10F); }
    private static RegistryObject<BlockHazard> hazard(String id, float resistance) { return hazard(id, DisplayEffect.NONE, resistance); }
    private static RegistryObject<BlockHazard> hazard(String id, DisplayEffect fx) { return hazard(id, fx, 10F); }

    private static RegistryObject<BlockHazard> hazard(String id, DisplayEffect fx, float resistance) {
        RegistryObject<BlockHazard> block = BLOCKS.registerBlock(id,
                p -> new BlockHazard(p, fx),
                p -> p.strength(5.0F, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops().randomTicks());
        blockItem(id, block);
        HAZARD_BLOCKS.add(block);
        return block;
    }

    private static RegistryObject<BlockRadResistant> radResistant(String id) { return radResistant(id, 5F, 10F); }

    private static RegistryObject<BlockRadResistant> radResistant(String id, float hardness, float resistance) {
        RegistryObject<BlockRadResistant> block = BLOCKS.registerBlock(id, BlockRadResistant::new,
                p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops());
        blockItem(id, block);
        return block;
    }

    private static void blockItem(String id, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
    }

    /** Assigns each hazard block its radiation from the block‑item's configured RADIATION hazard (must run
     * after HazardRegistry.registerItems). Corium's extra area pulse (rad3d) is set here directly. */
    public static void initHazards() {
        for (RegistryObject<? extends Block> ro : HAZARD_BLOCKS) {
            Block b = ro.get();
            if (b instanceof BlockHazard hazard) {
                hazard.addRadiation((float) HazardSystem.getRawRadsFromBlock(b));
            }
        }
        block_corium.get().addRad3d(150000F);
    }

    public static void init() {}
}

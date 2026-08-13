package com.hbm.blocks;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import com.hbm.blocks.generic.BlockFallingBase;
import net.minecraft.world.level.block.SoundType;

/**
 * The 1.12.2 {@code BlockFallingBase} family: gravity-affected, shovel-mineable blocks. Hand-registered
 * (the generated {@link ModBlocks} only covers plain full cubes), so codegen never clobbers them.
 *
 * <p>{@code BlockFallingBase} is a bare {@code BlockFalling}, so the port's {@link BlockFallingBase} is a
 * drop-in — the only per-block variation is hardness / blast resistance / sound, which the 1.12.2
 * registrations set individually and which are reproduced here.
 *
 * <p><b>Deferred:</b> the {@code gravel_diamond} easter-egg tooltip (26.1 {@code Block} has no
 * {@code appendHoverText}; it would need a custom {@code BlockItem}, as the anvils do).
 */
public final class ModFallingBlocks {

    private ModFallingBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Shredding obsidian / obsidian bricks yields this; blast-resistant like the obsidian it came from. */
    public static final RegistryObject<BlockFallingBase> gravel_obsidian =
            falling("gravel_obsidian", 5.0F, 300.0F, SoundType.GRAVEL);

    /** The shredded-diamond-ore drop (and the mod's long-running non-joke — see the class javadoc). */
    public static final RegistryObject<BlockFallingBase> gravel_diamond =
            falling("gravel_diamond", 0.6F, 0.6F, SoundType.GRAVEL);

    // --- fracking / oil-spill debris (shredded back into oil scrap) ---
    public static final RegistryObject<BlockFallingBase> dirt_dead = falling("dirt_dead", 0.5F, 0.5F, SoundType.GRAVEL);
    public static final RegistryObject<BlockFallingBase> dirt_oily = falling("dirt_oily", 0.5F, 0.5F, SoundType.GRAVEL);
    public static final RegistryObject<BlockFallingBase> sand_dirty = falling("sand_dirty", 0.5F, 0.5F, SoundType.SAND);
    public static final RegistryObject<BlockFallingBase> sand_dirty_red = falling("sand_dirty_red", 0.5F, 0.5F, SoundType.SAND);

    /** Tar sand — the oil-sand-bubble worldgen block; liquefies into BITUMEN (old {@code ModBlocks.ore_oil_sand}). */
    public static final RegistryObject<BlockFallingBase> ore_oil_sand = falling("ore_oil_sand", 0.5F, 1.0F, SoundType.SAND);

    /** Lead-mixed sand — the PUREX vitrification medium (old {@code ModBlocks.sand_lead}, hardness 0.5). */
    public static final RegistryObject<BlockFallingBase> sand_lead = falling("sand_lead", 0.5F, 0.5F, SoundType.SAND);

    /** Digamma ash — laid under every cell a DRX laser burns through (old {@code BlockHazardFalling}). */
    public static final RegistryObject<BlockFallingBase> ash_digamma = falling("ash_digamma", 0.5F, 150.0F, SoundType.SAND);

    private static RegistryObject<BlockFallingBase> falling(String id, float hardness, float resistance, SoundType sound) {
        RegistryObject<BlockFallingBase> block = BLOCKS.registerBlock(id, BlockFallingBase::new,
                p -> p.strength(hardness, resistance).sound(sound));
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.RESOURCES, item);
        return block;
    }

    public static void init() {}
}

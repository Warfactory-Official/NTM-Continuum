package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import com.hbm.blocks.generic.BlockFallingBase;
import net.minecraft.world.level.block.SoundType;

/**
 * The 1.12.2 {@code BlockFallingBase} family: gravity-affected, shovel-mineable blocks. Hand-registered
 * (the generated {@link ModBlocks} only covers plain full cubes), so codegen never clobbers them.
 *
 * <p>{@code BlockFallingBase} is a bare {@code BlockFalling}, so the only per-block variation is hardness /
 * blast resistance / sound, which the 1.12.2 registrations set individually and which are reproduced here.
 *
 * <p>The {@code gravel_diamond} easter-egg tooltip is not ported (26.1 {@code Block} has no
 * {@code appendHoverText}; it would need a custom {@code BlockItem}, as the anvils do).
 */
public final class ModFallingBlocks {

    private ModFallingBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Shredding obsidian / obsidian bricks yields this; blast-resistant like the obsidian it came from. */
    public static final RegistryObject<BlockFallingBase> gravel_obsidian =
            falling("gravel_obsidian", 5.0F, 300.0F, () -> SoundType.GRAVEL, HbmTab.RESOURCES, true)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** The shredded-diamond-ore drop (and the mod's long-running non-joke, see the class javadoc). */
    public static final RegistryObject<BlockFallingBase> gravel_diamond =
            falling("gravel_diamond", 0.6F, 0.6F, () -> SoundType.GRAVEL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    // --- fracking / oil-spill debris (shredded back into oil scrap) ---
    public static final RegistryObject<BlockFallingBase> dirt_dead = falling("dirt_dead", 0.5F, 0.5F, () -> SoundType.GRAVEL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> dirt_oily = falling("dirt_oily", 0.5F, 0.5F, () -> SoundType.GRAVEL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> sand_dirty = falling("sand_dirty", 0.5F, 0.5F, () -> SoundType.SAND)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> sand_dirty_red = falling("sand_dirty_red", 0.5F, 0.5F, () -> SoundType.SAND)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    /** Tar sand, the oil-sand-bubble worldgen block; liquefies into BITUMEN (old {@code ModBlocks.ore_oil_sand}). */
    public static final RegistryObject<BlockFallingBase> ore_oil_sand = falling("ore_oil_sand", 0.5F, 1.0F, () -> SoundType.SAND)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    /** Lead-mixed sand, the PUREX vitrification medium (old {@code ModBlocks.sand_lead}, hardness 0.5). */
    public static final RegistryObject<BlockFallingBase> sand_lead = falling("sand_lead", 0.5F, 0.5F, () -> SoundType.SAND)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    // --- remaining plain BlockFallingBase registrations ---
    public static final RegistryObject<BlockFallingBase> block_scrap =
            falling("block_scrap", 2.5F, 5.0F, () -> SoundType.GRAVEL, HbmTab.BLOCKS)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> block_electrical_scrap =
            falling("block_electrical_scrap", 2.5F, 5.0F, () -> SoundType.METAL, HbmTab.BLOCKS, true)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockFallingBase> moon_turf =
            falling("moon_turf", 0.5F, 0.5F, () -> SoundType.SAND, HbmTab.RESOURCES)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> block_red_phosphorus =
            falling("block_red_phosphorus", 5.0F, 10.0F, () -> SoundType.SAND, HbmTab.BLOCKS)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> sand_boron =
            falling("sand_boron", 0.5F, 0.5F, () -> SoundType.SAND, HbmTab.RESOURCES)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> sand_gold =
            falling("sand_gold", 0.5F, 0.5F, () -> SoundType.SAND, HbmTab.RESOURCES)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> sand_quartz =
            falling("sand_quartz", 0.5F, 0.5F, () -> SoundType.SAND, HbmTab.RESOURCES)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    /** Digamma ash, laid under every cell a DRX laser burns through (old {@code BlockHazardFalling}). */
    public static final RegistryObject<BlockFallingBase> ash_digamma = falling("ash_digamma", 0.5F, 150.0F, () -> SoundType.SAND)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    private static Reg.Handle<BlockFallingBase> falling(String id, float hardness, float resistance, Supplier<SoundType> sound) {
        return falling(id, hardness, resistance, sound, HbmTab.RESOURCES);
    }

    private static Reg.Handle<BlockFallingBase> falling(String id, float hardness, float resistance,
                                                             Supplier<SoundType> sound, HbmTab tab) {
        return falling(id, hardness, resistance, sound, tab, false);
    }

    /** {@code iron}: 1.7 {@code Material.iron}, which drops only when mined with a pickaxe. */
    private static Reg.Handle<BlockFallingBase> falling(String id, float hardness, float resistance,
                                                             Supplier<SoundType> sound, HbmTab tab, boolean iron) {
        Reg.Handle<BlockFallingBase> block = REG.block(id, BlockFallingBase::new, p -> {
            p.strength(hardness, resistance).sound(sound.get());
            return iron ? p.requiresCorrectToolForDrops() : p;
        });
        Reg.Handle<BlockItem> item = REG.blockItem(block, p -> Compat.blockDescription(p));
        ModTabs.addTo(tab, item);
        return block;
    }

    public static void init() {}
}

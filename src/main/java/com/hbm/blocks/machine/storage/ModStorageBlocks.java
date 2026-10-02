package com.hbm.blocks.machine.storage;

import com.hbm.registration.BlockTag;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.block.ItemBlockStorageCrate;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.MassStorageType;
import net.minecraft.world.level.block.SoundType;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.dejson.Dejson;

/**
 * Hand-registered storage blocks (the iron / steel / desh crates; future: tungsten crate, safe, file
 * cabinet, mass storage). Separate from generated {@link com.hbm.blocks.ModBlocks} so codegen never
 * clobbers them.
 */
public final class ModStorageBlocks {

    private ModStorageBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);
    /** A renderer-drawn block with one state. */
    private static final Preset RENDERED_SIMPLE = Presets.rendered(Variants.simple());
    /** Crates keep their name and contents when broken. */
    private static final Loot CRATE_LOOT = Loot.container("minecraft:custom_name", "minecraft:container");

    public static final RegistryObject<BlockCrate> crate_iron = REG.<BlockCrate>block(
            "crate_iron", p -> new BlockCrate(p, CrateType.IRON),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/crate_iron_top", "hbm:blocks/crate_iron_side", "hbm:blocks/crate_iron_top"))
                    .states(Variants.facing())
                    .blockItem())
            .loot(CRATE_LOOT)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockCrate> crate_steel = REG.<BlockCrate>block(
            "crate_steel", p -> new BlockCrate(p, CrateType.STEEL),
            p -> p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/crate_steel_top", "hbm:blocks/crate_steel_side", "hbm:blocks/crate_steel_top"))
                    .states(Variants.facing())
                    .blockItem())
            .loot(CRATE_LOOT)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockCrate> crate_desh = REG.<BlockCrate>block(
            "crate_desh", p -> new BlockCrate(p, CrateType.DESH),
            p -> p.strength(7.5F, 300.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/crate_desh_top", "hbm:blocks/crate_desh_side", "hbm:blocks/crate_desh_top"))
                    .states(Variants.facing())
                    .blockItem())
            .loot(CRATE_LOOT)
            .tag(BlockTag.PICKAXE);

    /** 1.7 ModBlocks.crate_supply: wood, 1.0/2.5, missile tab; placed by EntityParachuteCrate. */
    public static final RegistryObject<BlockCrateSupply> crate_supply = REG.<BlockCrateSupply>block(
            "crate_supply", BlockCrateSupply::new,
            p -> p.strength(1.0F, 2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava())
            .transform(RENDERED_SIMPLE);

    public static final RegistryObject<BlockMassStorage> mass_storage_wood = massStorage(MassStorageType.WOOD);
    public static final RegistryObject<BlockMassStorage> mass_storage_iron = massStorage(MassStorageType.IRON);
    public static final RegistryObject<BlockMassStorage> mass_storage_desh = massStorage(MassStorageType.DESH);
    public static final RegistryObject<BlockMassStorage> mass_storage = massStorage(MassStorageType.MASS);
    public static final RegistryObject<BlockTemplateCrate> crate_template = REG.<BlockTemplateCrate>block(
            "crate_template", BlockTemplateCrate::new,
            p -> p.strength(7.5F, 300.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/crate_template", "hbm:blocks/crate_template", "hbm:blocks/crate_template"))
                    .states(Variants.facing())
                    .blockItem())
            .loot(CRATE_LOOT)
            .tag(BlockTag.PICKAXE);

    static {
        tabItem("crate_iron", crate_iron, CrateType.IRON);
        tabItem("crate_steel", crate_steel, CrateType.STEEL);
        tabItem("crate_desh", crate_desh, CrateType.DESH);
        REG.blockItem(crate_supply).tab(HbmTab.MISSILES);
        massStorageItem(MassStorageType.WOOD, mass_storage_wood);
        massStorageItem(MassStorageType.IRON, mass_storage_iron);
        massStorageItem(MassStorageType.DESH, mass_storage_desh);
        massStorageItem(MassStorageType.MASS, mass_storage);
        REG.<BlockTemplateCrate, com.hbm.items.block.ItemBlockTemplateCrate>blockItem(crate_template,
                        com.hbm.items.block.ItemBlockTemplateCrate::new, Compat::blockDescription)
                .tab(HbmTab.MACHINES);
    }

    private static void tabItem(String name, RegistryObject<BlockCrate> block, CrateType type) {
        // Custom block item so the tooltip shows the "X/Y Slots used" readout (it carries the crate's
        // contents in the CONTAINER component). useBlockDescriptionPrefix() keeps the block.hbm.<id> key.
        REG.<BlockCrate, ItemBlockStorageCrate>blockItem(block,
                        (crate, p) -> new ItemBlockStorageCrate(crate, p, type), p -> Compat.blockDescription(p))
                .tab(HbmTab.MACHINES);
    }

    private static RegistryObject<BlockMassStorage> massStorage(MassStorageType type) {
        return REG.<BlockMassStorage>block(type.id, p -> new BlockMassStorage(p, type),
                        p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
                .model(d -> massStorageModel(d, type.id))
                .loot(Loot.container("minecraft:container", "minecraft:custom_data", "minecraft:custom_name"))
                .tag(BlockTag.PICKAXE);
    }

    /**
     * 1.7 BlockMassStorage: front texture on the placer-facing side (meta / 4 + 2), top/bottom shared. The item
     * keeps the front-less cube: 1.7 renders item meta 0-3 with the front on side 2, which the inventory view
     * never shows. Textures are {@code mass_storage_<part><_tier>}; the plain unit has no tier suffix.
     */
    private static Dejson.Declaration massStorageModel(Dejson.Declaration d, String id) {
        String tier = id.substring("mass_storage".length());
        String front = "hbm:blocks/mass_storage_front" + tier;
        String side = "hbm:blocks/mass_storage_side" + tier;
        String top = "hbm:blocks/mass_storage_top" + tier;
        return d.models(Models.orientable(front, side, top))
                .states(Variants.facing())
                .item(Models.cubeBottomTop(top, side, top));
    }

    private static void massStorageItem(MassStorageType type, RegistryObject<BlockMassStorage> block) {
        REG.<BlockMassStorage, com.hbm.items.block.ItemBlockMassStorage>blockItem(block,
                        (storage, p) -> new com.hbm.items.block.ItemBlockMassStorage(storage, p, type),
                        Compat::blockDescription)
                .tab(HbmTab.MACHINES);
    }

    public static void init() {}
}

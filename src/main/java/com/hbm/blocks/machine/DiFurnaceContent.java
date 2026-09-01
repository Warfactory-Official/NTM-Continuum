package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.tileentity.machine.TileEntityDiFurnace;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Feature-local registrations for the legacy fuel-fired DiFurnace and its extension. */
public final class DiFurnaceContent {

    private DiFurnaceContent() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    /** One registered block; LIT replaces the old _off/_on block swap. */
    public static final RegistryObject<MachineDiFurnace> MACHINE_DIFURNACE = BLOCKS.registerBlock(
            "machine_difurnace_off", MachineDiFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineDiFurnace.LIT) ? 1 : 0));

    public static final RegistryObject<MachineDiFurnaceExtension> MACHINE_DIFURNACE_EXTENSION = BLOCKS.registerBlock(
            "machine_difurnace_ext", MachineDiFurnaceExtension::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    public static final RegistryObject<BlockItem> MACHINE_DIFURNACE_ITEM = ITEMS.registerBlockItem(
            "machine_difurnace_off", MACHINE_DIFURNACE);
    public static final RegistryObject<BlockItem> MACHINE_DIFURNACE_EXTENSION_ITEM = ITEMS.registerBlockItem(
            "machine_difurnace_ext", MACHINE_DIFURNACE_EXTENSION);

    public static final RegistryObject<BlockEntityType<TileEntityDiFurnace>> DI_FURNACE = BLOCK_ENTITIES.register(
            "machine_difurnace_off", () -> BlockEntities.type(
                    TileEntityDiFurnace::new, MACHINE_DIFURNACE.get()));

    public static final RegistryObject<MenuType<ContainerDiFurnace>> DI_FURNACE_MENU = MENUS.register(
            "machine_difurnace", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerDiFurnace::new));

    static {
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_DIFURNACE_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_DIFURNACE_EXTENSION_ITEM);
    }

    /** Forces feature-local deferred entries to be declared before a loader flushes registries. */
    public static void init() {}
}

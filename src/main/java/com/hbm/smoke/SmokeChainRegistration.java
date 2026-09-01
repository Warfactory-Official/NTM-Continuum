package com.hbm.smoke;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineAshpit;
import com.hbm.blocks.machine.MachineChimneyBrick;
import com.hbm.blocks.machine.MachineChimneyIndustrial;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.container.ContainerAshpit;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityChimneyBrick;
import com.hbm.tileentity.machine.TileEntityChimneyIndustrial;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Isolated deferred registries for the chimney and ashpit feature. */
public final class SmokeChainRegistration {

    private SmokeChainRegistration() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MachineChimneyBrick> chimney_brick = BLOCKS.registerBlock(
            "chimney_brick", MachineChimneyBrick::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final RegistryObject<MachineChimneyIndustrial> chimney_industrial = BLOCKS.registerBlock(
            "chimney_industrial", MachineChimneyIndustrial::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final RegistryObject<MachineAshpit> machine_ashpit = BLOCKS.registerBlock(
            "machine_ashpit", MachineAshpit::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    static {
        blockItem("chimney_brick", chimney_brick);
        blockItem("chimney_industrial", chimney_industrial);
        blockItem("machine_ashpit", machine_ashpit);
    }

    public static final RegistryObject<BlockEntityType<TileEntityChimneyBrick>> CHIMNEY_BRICK =
            BLOCK_ENTITIES.register("chimney_brick", () -> BlockEntities.type(
                    TileEntityChimneyBrick::new, chimney_brick.get()));
    public static final RegistryObject<BlockEntityType<TileEntityChimneyIndustrial>> CHIMNEY_INDUSTRIAL =
            BLOCK_ENTITIES.register("chimney_industrial", () -> BlockEntities.type(
                    TileEntityChimneyIndustrial::new, chimney_industrial.get()));
    public static final RegistryObject<BlockEntityType<TileEntityAshpit>> ASHPIT =
            BLOCK_ENTITIES.register("machine_ashpit", () -> BlockEntities.type(
                    TileEntityAshpit::new, machine_ashpit.get()));

    public static final RegistryObject<MenuType<ContainerAshpit>> ASHPIT_MENU = MENUS.register(
            "ashpit", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerAshpit::new));

    private static void blockItem(String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    /** Class-load hook for the eventual common bootstrap integration. */
    public static void init() {}
}

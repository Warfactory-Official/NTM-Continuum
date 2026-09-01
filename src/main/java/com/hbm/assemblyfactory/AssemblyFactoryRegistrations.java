package com.hbm.assemblyfactory;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineAssemblyFactory;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import dev.golem.core.registry.BlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Factory-local deferred entries. The common bootstrap must load this class before registry flush. */
public final class AssemblyFactoryRegistrations {

    private AssemblyFactoryRegistrations() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MachineAssemblyFactory> MACHINE_ASSEMBLY_FACTORY = BLOCKS.registerBlock(
            "machine_assembly_factory", MachineAssemblyFactory::new,
            AssemblyFactoryRegistrations::machineProperties);

    public static final RegistryObject<BlockItem> MACHINE_ASSEMBLY_FACTORY_ITEM = ITEMS.registerBlockItem(
            "machine_assembly_factory", MACHINE_ASSEMBLY_FACTORY);

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyFactory>> MACHINE_ASSEMBLY_FACTORY_BE =
            BLOCK_ENTITIES.register("machine_assembly_factory", () -> BlockEntities.type(
                    TileEntityMachineAssemblyFactory::new, MACHINE_ASSEMBLY_FACTORY.get()));

    public static final RegistryObject<MenuType<ContainerMachineAssemblyFactory>> MACHINE_ASSEMBLY_FACTORY_MENU =
            MENUS.register("machine_assembly_factory", () -> Menus.typeWithPos(ContainerMachineAssemblyFactory::new));

    static {
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_ASSEMBLY_FACTORY_ITEM);
    }

    private static BlockBehaviour.Properties machineProperties(BlockBehaviour.Properties properties) {
        return properties.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .noOcclusion().isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    /** Forces this leaf's deferred entries to exist before the loader flushes all registers. */
    public static void init() {}
}

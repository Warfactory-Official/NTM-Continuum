package com.hbm.assemblyfactory;

import dev.golem.core.registry.BlockTag;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;

/** Factory-local deferred entries. The common bootstrap must load this class before registry flush. */
public final class AssemblyFactoryRegistrations {

    private AssemblyFactoryRegistrations() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    // Typed registers: REG picks them without naming a registry key, which on 1.20.1 fails in DejsonMain's
    // plain JVM (this class is initialised there: its blocks carry declarations).
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.createMenuTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);
    /** Folded multiblocks drawn by their renderer: every core/cell state is the shared empty placeholder. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    public static final RegistryObject<MachineAssemblyFactory> MACHINE_ASSEMBLY_FACTORY = REG.<MachineAssemblyFactory>block(
            "machine_assembly_factory", MachineAssemblyFactory::new,
            AssemblyFactoryRegistrations::machineProperties)
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> MACHINE_ASSEMBLY_FACTORY_ITEM = REG.blockItem(MACHINE_ASSEMBLY_FACTORY);

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyFactory>> MACHINE_ASSEMBLY_FACTORY_BE =
            REG.blockEntity("machine_assembly_factory", () -> BlockEntities.ticking(TileEntityMachineAssemblyFactory::new, TileEntityMachineAssemblyFactory::clientTick, TileEntityMachineAssemblyFactory::serverTick, MACHINE_ASSEMBLY_FACTORY.get()));

    public static final RegistryObject<MenuType<ContainerMachineAssemblyFactory>> MACHINE_ASSEMBLY_FACTORY_MENU =
            REG.menu("machine_assembly_factory", () -> Menus.typeWithPos(ContainerMachineAssemblyFactory::new));

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

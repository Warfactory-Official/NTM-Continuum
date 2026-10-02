package com.hbm.smoke;

import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.tileentity.machine.TileEntityChimneyBase;
import com.hbm.blocks.machine.MachineAshpit;
import com.hbm.blocks.machine.MachineChimneyBrick;
import com.hbm.blocks.machine.MachineChimneyIndustrial;
import com.hbm.creativetabs.HbmTab;
import com.hbm.inventory.container.ContainerAshpit;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityChimneyBrick;
import com.hbm.tileentity.machine.TileEntityChimneyIndustrial;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Isolated deferred registries for the chimney and ashpit feature. */
public final class SmokeChainRegistration {

    private SmokeChainRegistration() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.createMenuTypes(Constants.MOD_ID);
    /** Typed register(s): REG picks them without naming a registry key, which 1.20.1 cannot do in the build-time Dejson JVM. */
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);
    /** Folded multiblocks drawn by their renderers: the empty placeholder blockstate; they drop themselves. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
                    Variants.uniform("facing", "north", "east", "south", "west"), Variants.uniform("cell", "false", "true")))
            .and(handle -> handle.loot(Loot.self()));

    public static final RegistryObject<MachineChimneyBrick> chimney_brick = REG.block(
            "chimney_brick", MachineChimneyBrick::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineChimneyIndustrial> chimney_industrial = REG.block(
            "chimney_industrial", MachineChimneyIndustrial::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineAshpit> machine_ashpit = REG.block(
            "machine_ashpit", MachineAshpit::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .tag(BlockTag.PICKAXE);

    static {
        blockItem("chimney_brick", chimney_brick);
        blockItem("chimney_industrial", chimney_industrial);
        blockItem("machine_ashpit", machine_ashpit);
    }

    public static final RegistryObject<BlockEntityType<TileEntityChimneyBrick>> CHIMNEY_BRICK =
            REG.blockEntity("chimney_brick", () -> BlockEntities.ticking(TileEntityChimneyBrick::new, TileEntityChimneyBase::clientTick, TileEntityChimneyBase::serverTick, chimney_brick.get()));
    public static final RegistryObject<BlockEntityType<TileEntityChimneyIndustrial>> CHIMNEY_INDUSTRIAL =
            REG.blockEntity("chimney_industrial", () -> BlockEntities.ticking(TileEntityChimneyIndustrial::new, TileEntityChimneyBase::clientTick, TileEntityChimneyBase::serverTick, chimney_industrial.get()));
    public static final RegistryObject<BlockEntityType<TileEntityAshpit>> ASHPIT =
            REG.blockEntity("machine_ashpit", () -> BlockEntities.ticking(TileEntityAshpit::new, TileEntityAshpit::clientTick, TileEntityAshpit::serverTick, machine_ashpit.get()));

    public static final RegistryObject<MenuType<ContainerAshpit>> ASHPIT_MENU = REG.menu(
            "ashpit", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerAshpit::new));

    private static void blockItem(String name, RegistryObject<? extends Block> block) {
        REG.blockItem(block).tab(HbmTab.MACHINES);
    }

    /** Class-load hook for the eventual common bootstrap integration. */
    public static void init() {}
}

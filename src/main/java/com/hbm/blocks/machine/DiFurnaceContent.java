package com.hbm.blocks.machine;

import dev.golem.core.registry.BlockTag;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.tileentity.machine.TileEntityDiFurnace;
import com.hbm.tileentity.machine.TileEntityDiFurnaceExtension;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.States;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.Variants;

/** Feature-local registrations for the legacy fuel-fired DiFurnace and its extension. */
public final class DiFurnaceContent {

    private DiFurnaceContent() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    // Typed registers: REG picks them without naming a registry key, which on 1.20.1 fails in DejsonMain's
    // plain JVM (this class is initialised there: its blocks carry declarations).
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.createMenuTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);
    /** A renderer-drawn block with one state. */
    private static final Preset RENDERED_SIMPLE = Presets.rendered(Variants.simple());

    /** One registered block; LIT replaces the old _off/_on block swap. */
    public static final RegistryObject<MachineDiFurnace> MACHINE_DIFURNACE = REG.<MachineDiFurnace>block(
            "machine_difurnace_off", MachineDiFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineDiFurnace.LIT) ? 1 : 0))
            .loot(Loot.self())
            .andThen(h -> States.variants(h.namespace(), h.path())
                    .variant("facing=north,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off"))
                    .variant("facing=east,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off", 0, 90, false))
                    .variant("facing=south,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off", 0, 180, false))
                    .variant("facing=west,lit=false,ext=false", new Variant("hbm:block/machine_difurnace_off", 0, 270, false))
                    .variant("facing=north,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on"))
                    .variant("facing=east,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on", 0, 90, false))
                    .variant("facing=south,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on", 0, 180, false))
                    .variant("facing=west,lit=true,ext=false", new Variant("hbm:block/machine_difurnace_on", 0, 270, false))
                    .variant("facing=north,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall"))
                    .variant("facing=east,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall", 0, 90, false))
                    .variant("facing=south,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall", 0, 180, false))
                    .variant("facing=west,lit=false,ext=true", new Variant("hbm:block/machine_difurnace_off_tall", 0, 270, false))
                    .variant("facing=north,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall"))
                    .variant("facing=east,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall", 0, 90, false))
                    .variant("facing=south,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall", 0, 180, false))
                    .variant("facing=west,lit=true,ext=true", new Variant("hbm:block/machine_difurnace_on_tall", 0, 270, false))
                    .register())
            .icon(d -> d.shares("hbm:block/machine_difurnace_off"))
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineDiFurnaceExtension> MACHINE_DIFURNACE_EXTENSION = REG.<MachineDiFurnaceExtension>block(
            "machine_difurnace_ext", MachineDiFurnaceExtension::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false))
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> MACHINE_DIFURNACE_ITEM = REG.blockItem(MACHINE_DIFURNACE);
    public static final RegistryObject<BlockItem> MACHINE_DIFURNACE_EXTENSION_ITEM = REG.blockItem(
            MACHINE_DIFURNACE_EXTENSION);

    public static final RegistryObject<BlockEntityType<TileEntityDiFurnace>> DI_FURNACE = REG.blockEntity(
            "machine_difurnace_off", () -> BlockEntities.serverTicking(TileEntityDiFurnace::new, TileEntityDiFurnace::serverTick, MACHINE_DIFURNACE.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDiFurnaceExtension>> DI_FURNACE_EXTENSION =
            REG.blockEntity("machine_difurnace_ext", () -> BlockEntities.type(
                    TileEntityDiFurnaceExtension::new, MACHINE_DIFURNACE_EXTENSION.get()));

    public static final RegistryObject<MenuType<ContainerDiFurnace>> DI_FURNACE_MENU = REG.menu(
            "machine_difurnace", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerDiFurnace::new));

    static {
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_DIFURNACE_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_DIFURNACE_EXTENSION_ITEM);
    }

    /** Forces feature-local deferred entries to be declared before a loader flushes registries. */
    public static void init() {}
}

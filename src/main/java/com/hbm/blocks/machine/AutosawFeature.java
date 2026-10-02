package com.hbm.blocks.machine;

import com.hbm.registration.BlockTag;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockAutosaw;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;

/** Registration boundary for the autosaw leaf; the common registries wire this helper during handoff. */
public final class AutosawFeature {

    private AutosawFeature() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    // Typed registers: REG picks them without naming a registry key, which on 1.20.1 fails in DejsonMain's
    // plain JVM (this class is initialised there: its blocks carry declarations).
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES);
    /** A renderer-drawn block with one state. */
    private static final Preset RENDERED_SIMPLE = Presets.rendered(Variants.simple());

    public static final RegistryObject<MachineAutosaw> machine_autosaw = REG.<MachineAutosaw>block(
            "machine_autosaw", MachineAutosaw::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<ItemBlockAutosaw> machine_autosaw_item = REG.<MachineAutosaw, ItemBlockAutosaw>blockItem(
            machine_autosaw, ItemBlockAutosaw::new, Compat::blockDescription);

    public static final RegistryObject<BlockEntityType<TileEntityMachineAutosaw>> MACHINE_AUTOSAW =
            REG.blockEntity("machine_autosaw", () -> dev.golem.core.registry.BlockEntities.ticking(TileEntityMachineAutosaw::new, TileEntityMachineAutosaw::clientTick, TileEntityMachineAutosaw::serverTick, machine_autosaw.get()));

    static {
        ModTabs.addTo(HbmTab.MACHINES, machine_autosaw_item);
    }

    /** Forces class loading from the common bootstrap without changing the shared registries. */
    public static void init() {}
}

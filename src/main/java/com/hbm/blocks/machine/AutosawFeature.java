package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockAutosaw;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Registration boundary for the autosaw leaf; the common registries wire this helper during handoff. */
public final class AutosawFeature {

    private AutosawFeature() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<MachineAutosaw> machine_autosaw = BLOCKS.registerBlock(
            "machine_autosaw", MachineAutosaw::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    public static final RegistryObject<ItemBlockAutosaw> machine_autosaw_item = ITEMS.registerItem(
            "machine_autosaw", p -> new ItemBlockAutosaw(machine_autosaw.get(), p), Compat::blockDescription);

    public static final RegistryObject<BlockEntityType<TileEntityMachineAutosaw>> MACHINE_AUTOSAW =
            BLOCK_ENTITIES.register("machine_autosaw", () -> dev.golem.core.registry.BlockEntities.type(
                    TileEntityMachineAutosaw::new, machine_autosaw.get()));

    static {
        ModTabs.addTo(HbmTab.MACHINES, machine_autosaw_item);
    }

    /** Forces class loading from the common bootstrap without changing the shared registries. */
    public static void init() {}
}

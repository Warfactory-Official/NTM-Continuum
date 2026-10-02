package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.ModArmorSetBBlocks;
import com.hbm.tileentity.deco.TileEntityDecoBlockAltF;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity type of the final armour wave B's one block, the {@code statue_elb_f} statue. */
public final class ModArmorSetBBlockEntities {

    private ModArmorSetBBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    /** 1.7 updateEntity ran on both sides; the aura only matters on the server. */
    public static final RegistryObject<BlockEntityType<TileEntityDecoBlockAltF>> STATUE_ELB_F =
            REG.blockEntity("statue_elb_f", () -> BlockEntities.serverTicking(TileEntityDecoBlockAltF::new,
                    TileEntityDecoBlockAltF::tick, ModArmorSetBBlocks.statue_elb_f.get()));

    public static void init() {}
}

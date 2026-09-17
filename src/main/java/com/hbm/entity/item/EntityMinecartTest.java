package com.hbm.entity.item;

import dev.golem.core.data.CompatMinecartTNT;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EntityMinecartTest extends CompatMinecartTNT {

    public EntityMinecartTest(EntityType<? extends EntityMinecartTest> type, Level level) {
        super(type, level);
    }

    // TODO unported: 1.7 render/entity/item/RenderMinecartTest:144-145 binds textures/models/LilBoy2.png over
    // models/LilBoy1.obj, with an additive second pass while the fuse burns (:158-159). Its display-block draw
    // is commented out (:143, :157), so 1.7 shows the bomb mesh and no block at all. Until the mesh is ported
    // the cart draws no display block rather than 1.7's suppressed crate or MinecartTNT's TNT.
    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }
}

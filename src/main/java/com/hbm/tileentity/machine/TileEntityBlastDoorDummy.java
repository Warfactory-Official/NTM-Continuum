package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.BlastDoor;
import com.hbm.blocks.machine.DummyBlockBlast;
import com.hbm.tileentity.ModDoorBlockEntities;

/**
 * 1.7.10 {@code TileEntityDummy} as the blast door uses it: one block of the leaf, pointing at the
 * door that owns it. Removes itself once the door it points at is gone.
 */
public class TileEntityBlastDoorDummy extends CompatBlockEntity {

    public BlockPos target = BlockPos.ZERO;

    public TileEntityBlastDoorDummy(BlockPos pos, BlockState state) {
        super(ModDoorBlockEntities.BLAST_DOOR_DUMMY.get(), pos, state);
    }

    public void serverTick() {
        if (level == null) return;
        if (!(level.getBlockState(target).getBlock() instanceof BlastDoor)) {
            level.destroyBlock(worldPosition, false);
        }
    }

    /** 1.7 {@code DummyBlockBlast.breakBlock}: unless the door itself is retracting this leaf, breaking it breaks the door. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || DummyBlockBlast.safeBreak) return;
        level.destroyBlock(target, true);
    }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        target = new BlockPos(nbt.getIntOr("tx", 0), nbt.getIntOr("ty", 0), nbt.getIntOr("tz", 0));
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        nbt.putInt("tx", target.getX());
        nbt.putInt("ty", target.getY());
        nbt.putInt("tz", target.getZ());
    }
}

package com.hbm.tileentity.machine.rbmk;

import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityRBMKDisplay}: a 7x7 window onto a linked reactor, rescanned every ten ticks. The
 * target is set with the RBMK console linking device and the view is turned in quarter steps with a
 * screwdriver. Column data travels as the same typed {@link RBMKColumn} snapshot the console uses.
 */
public class BlockEntityRBMKDisplay extends TileEntityLoadedBase implements IRBMKTicking, com.hbm.tileentity.IRelocatable {

    /** A sub-level move carried this display; it keeps its reactor when the reactor was carried too. */
    @Override
    public void relocate(dev.golem.core.world.SubLevels.Move move) {
        net.minecraft.core.BlockPos target = com.hbm.tileentity.IRelocatable.follow(move, new net.minecraft.core.BlockPos(targetX, targetY, targetZ));
        setTarget(target.getX(), target.getY(), target.getZ());
    }


    private int targetX, targetY, targetZ;
    private byte rotation;
    public final RBMKColumn[] columns = new RBMKColumn[7 * 7];

    public BlockEntityRBMKDisplay(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_DISPLAY.get(), pos, state);
    }

    @Override public void tickServer() {
        if (level.getGameTime() % 10 == 0) {
            rescan();
            networkPackNT();
        }
    }

    public void setTarget(int x, int y, int z) {
        targetX = x; targetY = y; targetZ = z;
        setChanged();
    }

    public void rotate() {
        rotation = (byte) ((rotation + 1) % 4);
        // The original never marked the tile dirty after turning; the new view is saved here.
        setChanged();
    }

    public byte rotation() { return rotation; }

    private void rescan() {
        for (int index = 0; index < columns.length; index++) {
            BlockEntity te = level.getBlockEntity(new BlockPos(targetX + getXFromIndex(index), targetY, targetZ + getZFromIndex(index)));
            columns[index] = te instanceof BlockEntityRBMKBase rbmk ? rbmk.getConsoleData() : null;
        }
    }

    public int getXFromIndex(int col) {
        int i = col % 7 - 3, j = col / 7 - 3;
        return switch (rotation) { case 1 -> -j; case 2 -> -i; case 3 -> j; default -> i; };
    }

    public int getZFromIndex(int col) {
        int i = col % 7 - 3, j = col / 7 - 3;
        return switch (rotation) { case 1 -> i; case 2 -> -j; case 3 -> -i; default -> j; };
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (RBMKColumn column : columns) RBMKColumn.writeToBuf(buf, column);
    }

    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < columns.length; i++) columns[i] = RBMKColumn.readFromBuf(buf);
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("tX", targetX); output.putInt("tY", targetY); output.putInt("tZ", targetZ);
        output.putByte("rotation", rotation);
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        targetX = input.getIntOr("tX", 0); targetY = input.getIntOr("tY", 0); targetZ = input.getIntOr("tZ", 0);
        rotation = input.getByteOr("rotation", (byte) 0);
    }
}

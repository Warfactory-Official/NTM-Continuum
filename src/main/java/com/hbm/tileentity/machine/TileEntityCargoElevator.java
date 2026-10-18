package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.BlockCargoElevator;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.server.level.ServerLevel;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7's stacking freight lift. Placing a whole elevator directly beneath an existing one splices the two
 * together, which is why the tick begins by looking down. Riders whose feet lie in the slab the platform swept
 * this tick are snapped onto its new height, so a fast lift never drops them.
 */
// port: 1.7 lists VAL:extension but implements only the function half; this provides the value too.
public class TileEntityCargoElevator extends TileEntityLoadedBase implements IRORValueProvider, IRORInteractive {

    public static final double SPEED = 2D / 20D;

    public int height;
    public int targetExtension;
    public double extension;
    public double prevExtension;
    public double syncExtension;
    private int sync;

    public TileEntityCargoElevator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_ELEVATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCargoElevator lift) {
        lift.prevExtension = lift.extension;

        if (lift.spliceIntoLower(level, pos)) return;

        if (lift.extension < lift.targetExtension) {
            lift.extension = Mth.clamp(lift.extension + SPEED, 0D, lift.targetExtension);
        } else if (lift.extension > lift.targetExtension) {
            lift.extension = Mth.clamp(lift.extension - SPEED, lift.targetExtension, lift.height);
        }
        lift.extension = Mth.clamp(lift.extension, 0D, lift.height);

        lift.networkPackNT();
        lift.carryRiders(level, pos);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCargoElevator lift) {
        lift.prevExtension = lift.extension;

        if (lift.sync > 0) {
            lift.extension += (lift.syncExtension - lift.extension) / lift.sync;
            lift.sync--;
        } else {
            lift.extension = lift.syncExtension;
        }
        lift.carryRiders(level, pos);
    }

    /** True when this lift has just been swallowed by one below it and must stop ticking as a core. */
    private boolean spliceIntoLower(Level level, BlockPos pos) {
        BlockCargoElevator elevator = ModMachineBlocks.cargo_elevator.get();
        if (level.getBlockState(pos.below()).getBlock() != elevator) return false;

        BlockPos lowerCore = MultiblockSurface.coreOfAny(level, pos.below());
        if (lowerCore == null || lowerCore.getX() != pos.getX() || lowerCore.getZ() != pos.getZ()) return false;
        if (!(level.getBlockEntity(lowerCore) instanceof TileEntityCargoElevator lower)) return false;

        lower.height += height + 1;
        for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
            for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
                for (int y = pos.getY(); y <= pos.getY() + height; y++) {
                    BlockPos cell = new BlockPos(x, y, z);
                    level.setBlock(cell, elevator.defaultBlockState()
                            .setValue(BlockCargoElevator.CELL, Boolean.TRUE), 3);
                    if (level instanceof ServerLevel server) {
                        MultiblockCoreIndex.put(server, cell, lowerCore);
                    }
                }
            }
        }
        // 1.7's meta-1 setBlock dropped this tile entity. A state change within one block keeps it, and it would
        // splice again every tick, growing the lower lift's height without end.
        level.removeBlockEntity(pos);
        lower.markChanged();
        return true;
    }

    private void carryRiders(Level level, BlockPos pos) {
        if (extension == prevExtension) return;

        double upper = pos.getY() + 1D + Math.max(extension, prevExtension);
        double lower = pos.getY() + 1D + Math.min(extension, prevExtension);
        List<Entity> riders = level.getEntitiesOfClass(Entity.class,
                new AABB(pos.getX() - 0.99D, lower, pos.getZ() - 0.99D,
                        pos.getX() + 1.99D, upper, pos.getZ() + 1.99D));

        for (Entity entity : riders) {
            if (entity instanceof Player && !level.isClientSide()) continue;
            double feet = entity.getBoundingBox().minY;
            if (feet < lower || feet > upper) continue;

            entity.move(MoverType.SELF, new Vec3(
                    0D, -(feet - (pos.getY() + 1D + extension)), 0D));
            entity.setOnGround(true);
            entity.move(MoverType.SELF, new Vec3(0D, -0.125D, 0D));
        }
    }

    public void toggleElevator() {
        targetExtension = targetExtension == 0 ? height : 0;
        markChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(height);
        buf.writeDouble(extension);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        height = buf.readShort();
        syncExtension = buf.readDouble();
        if (syncExtension > 0 && syncExtension < height) sync = 3;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putDouble("extension", extension);
        output.putInt("targetExtension", targetExtension);
        output.putInt("height", height);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        extension = input.getDoubleOr("extension", 0D);
        targetExtension = input.getIntOr("targetExtension", 0);
        height = input.getIntOr("height", 0);
        syncExtension = extension;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "extension",
                PREFIX_FUNCTION + "setextension" + NAME_SEPARATOR + "amount"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "extension").equals(name)) return "" + (int) extension;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setextension").equals(name) && params.length > 0) {
            targetExtension = IRORInteractive.parseInt(params[0], 0, height);
            markChanged();
        }
        return null;
    }
}

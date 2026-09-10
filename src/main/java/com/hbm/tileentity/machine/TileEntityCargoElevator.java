package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
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

/**
 * A stacking freight lift, ported from 1.12.2's {@code TileEntityCargoElevator}.
 *
 * <p>It grows a block at a time: right-clicking the structure with another elevator adds a 3×3 course
 * on top and raises {@link #height}. Placing a whole elevator directly beneath an existing one splices
 * the two together — the lower core absorbs the upper's height and the upper's cells are rewritten as
 * dummies — which is why the tick begins by looking down before it does anything else.
 *
 * <p>Riders are carried rather than pushed: anything whose feet lie in the slab the platform swept this
 * tick is snapped onto the platform's new height, so a rider never falls through a fast lift the way a
 * pure collision box would let it.
 *
 * <p><b>Deviation from the fork.</b> There the tile advertises a {@code VAL:extension} value while
 * implementing only the function half of the interface, so nothing can read it. This port implements
 * the value half too rather than dropping the advertised name.
 */
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
        BlockDummyable elevator = ModMachineBlocks.cargo_elevator.get();
        if (level.getBlockState(pos.below()).getBlock() != elevator) return false;

        BlockPos lowerCore = elevator.findCore(level, pos.below());
        if (lowerCore == null || lowerCore.getX() != pos.getX() || lowerCore.getZ() != pos.getZ()) return false;
        if (!(level.getBlockEntity(lowerCore) instanceof TileEntityCargoElevator lower)) return false;

        lower.height += height + 1;
        for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
            for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
                for (int y = pos.getY(); y <= pos.getY() + height; y++) {
                    level.setBlock(new BlockPos(x, y, z),
                            elevator.defaultBlockState().setValue(BlockDummyable.META, 1), 3);
                }
            }
        }
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

            entity.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(
                    0D, -(feet - (pos.getY() + 1D + extension)), 0D));
            entity.setOnGround(true);
            entity.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(0D, -0.125D, 0D));
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
                PREFIX_FUNCTION + "setextension" + NAME_SEPARATOR + "extension"};
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

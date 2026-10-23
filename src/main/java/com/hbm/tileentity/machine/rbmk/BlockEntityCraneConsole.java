package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.items.machine.ItemRBMKRod;
import dev.golem.core.data.Tags;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumCraneKey;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Crane carriage state and the load/unload action at its current column target. */
public class BlockEntityCraneConsole extends TileEntityMachineBase implements IRBMKTicking {

    private static final double SPEED = 0.05D;

    public int centerX;
    public int centerY;
    public int centerZ;
    public int spanF;
    public int spanB;
    public int spanL;
    public int spanR;
    public int height;
    public boolean setUpCrane;
    public int craneRotationOffset;
    public double lastTiltFront;
    public double lastTiltLeft;
    public double tiltFront;
    public double tiltLeft;
    public double lastPosFront;
    public double lastPosLeft;
    public double posFront;
    public double posLeft;
    public double syncFront;
    public double syncLeft;
    public double lastProgress = 1D;
    public double progress = 1D;
    public double syncProgress = 1D;
    public double loadedHeat;
    public double loadedEnrichment;
    private boolean goesDown;
    private boolean hasLoaded;
    private int turnProgress;

    public BlockEntityCraneConsole(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_CRANE_CONSOLE.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.rbmk_crane_console";
    }

    @Override
    public void tickClient() {
        if (BlockMultiblockMachine.removeIfAtCell(this)) return; // stale pre-fix cell desk
        lastTiltFront = tiltFront;
        lastTiltLeft = tiltLeft;
        lastPosFront = posFront;
        lastPosLeft = posLeft;
        lastProgress = progress;
        if (turnProgress > 0) {
            posFront += (syncFront - posFront) / turnProgress;
            posLeft += (syncLeft - posLeft) / turnProgress;
            progress += (syncProgress - progress) / turnProgress;
            turnProgress--;
        } else {
            posFront = syncFront;
            posLeft = syncLeft;
            progress = syncProgress;
        }
        readControls(true);
    }

    @Override
    public void tickServer() {
        if (BlockMultiblockMachine.removeIfAtCell(this)) return; // stale pre-fix cell desk
        BlockEntityRBMKBase column = getColumnAtPos();
        if (column != null) {
            column.craneIndicator = 10;
        }

        if (goesDown) {
            if (progress > 0D) {
                progress -= 0.04D;
            } else {
                progress = 0D;
                goesDown = false;
                performLoadAction();
            }
        } else if (progress != 1D) {
            progress = Math.min(1D, progress + 0.04D);
        }

        readControls(false);
        updateLoadedReadout();
        networkPackNT();
    }

    /** Tilt and motion come from the nearest player standing at the console. */
    private void readControls(boolean client) {
        Direction direction = coreDir();
        Direction side = direction.getClockWise();
        double minX = worldPosition.getX() + .5D - side.getStepX() * 1.5D;
        double maxX = worldPosition.getX() + .5D + side.getStepX() * 1.5D + direction.getStepX() * 2D;
        double minZ = worldPosition.getZ() + .5D - side.getStepZ() * 1.5D;
        double maxZ = worldPosition.getZ() + .5D + side.getStepZ() * 1.5D + direction.getStepZ() * 2D;
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(
                Math.min(minX, maxX), worldPosition.getY(), Math.min(minZ, maxZ),
                Math.max(minX, maxX), worldPosition.getY() + 2, Math.max(minZ, maxZ)));

        tiltFront = 0;
        tiltLeft = 0;
        if (!players.isEmpty() && !isCraneLoading()) {
            var props = HbmPlayerProps.getData(players.get(0));
            boolean up = props.isCraneKeyPressed(EnumCraneKey.UP);
            boolean down = props.isCraneKeyPressed(EnumCraneKey.DOWN);
            boolean left = props.isCraneKeyPressed(EnumCraneKey.LEFT);
            boolean right = props.isCraneKeyPressed(EnumCraneKey.RIGHT);
            if (up && !down) {
                tiltFront = 30;
                if (!client) posFront += SPEED;
            }
            if (!up && down) {
                tiltFront = -30;
                if (!client) posFront -= SPEED;
            }
            if (left && !right) {
                tiltLeft = 30;
                if (!client) posLeft += SPEED;
            }
            if (!left && right) {
                tiltLeft = -30;
                if (!client) posLeft -= SPEED;
            }
            if (props.isCraneKeyPressed(EnumCraneKey.LOAD)) goesDown = true;
        }
        posFront = Mth.clamp(posFront, -spanB, spanF);
        posLeft = Mth.clamp(posLeft, -spanR, spanL);
    }

    /** Used by the crane input bridge; movement is unavailable while the hoist is moving. */
    public void move(double front, double left) {
        if (!setUpCrane || isCraneLoading()) return;
        posFront = Mth.clamp(posFront + front, -spanB, spanF);
        posLeft = Mth.clamp(posLeft + left, -spanR, spanL);
        setChanged();
    }

    public void beginLoad() {
        if (setUpCrane && !isCraneLoading()) {
            goesDown = true;
            setChanged();
        }
    }

    private void performLoadAction() {
        IRBMKLoadable target = getLoadableAtPos();
        if (!canTargetInteract(target)) return;

        ItemStack carried = inventory.getStackInSlot(0);
        if (!carried.isEmpty()) {
            target.load(carried);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        } else {
            inventory.setStackInSlot(0, target.provideNext().copy());
            target.unload();
        }
        setChanged();
    }

    private void updateLoadedReadout() {
        ItemStack carried = inventory.getStackInSlot(0);
        if (carried.getItem() instanceof ItemRBMKRod) {
            loadedHeat = ItemRBMKRod.getHullHeat(carried);
            loadedEnrichment = ItemRBMKRod.getEnrichment(carried);
        } else {
            loadedHeat = 0D;
            loadedEnrichment = 0D;
        }
    }

    public boolean hasItemLoaded() {
        return level != null && level.isClientSide() ? hasLoaded : !inventory.getStackInSlot(0).isEmpty();
    }

    public boolean isCraneLoading() {
        return progress != 1D;
    }

    public boolean isAboveValidTarget() {
        return getLoadableAtPos() != null;
    }

    public boolean canTargetInteract(@Nullable IRBMKLoadable column) {
        return column != null && (hasItemLoaded() ? column.canLoad(inventory.getStackInSlot(0)) : column.canUnload());
    }

    public @Nullable BlockEntityRBMKBase getColumnAtPos() {
        Direction forward = coreDir();
        Direction left = forward.getCounterClockWise();
        int x = (int) Math.floor(centerX - forward.getStepX() * posFront - left.getStepX() * posLeft + 0.5D);
        int y = centerY - 1;
        int z = (int) Math.floor(centerZ - forward.getStepZ() * posFront - left.getStepZ() * posLeft + 0.5D);
        BlockPos pos = new BlockPos(x, y, z);
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof RBMKBase)) return null;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof BlockEntityRBMKBase column ? column : null;
    }

    public @Nullable IRBMKLoadable getLoadableAtPos() {
        return getColumnAtPos() instanceof IRBMKLoadable loadable ? loadable : null;
    }

    public void setTarget(int x, int y, int z) {
        centerX = x;
        centerY = y + RBMKDials.getColumnHeight(level) + 1;
        centerZ = z;
        int girderY = centerY + 6;
        Direction direction = coreDir().getOpposite();
        spanF = findRoomExtent(x, girderY, z, direction, 16);
        spanR = findRoomExtent(x, girderY, z, direction = direction.getClockWise(), 16);
        spanB = findRoomExtent(x, girderY, z, direction = direction.getClockWise(), 16);
        spanL = findRoomExtent(x, girderY, z, direction.getClockWise(), 16);
        height = 7;
        setUpCrane = true;
        setChanged();
    }

    private int findRoomExtent(int x, int y, int z, Direction direction, int max) {
        for (int i = 1; i < max; i++) {
            if (!level.getBlockState(new BlockPos(x + direction.getStepX() * i, y, z + direction.getStepZ() * i)).isAir()) {
                return i - 1;
            }
        }
        return max;
    }

    /** 1.7 OpenComputers {@code load()}: sets the hoist going down without {@link #beginLoad}'s loading check. */
    public void requestLoad() {
        goesDown = true;
    }

    public Direction coreDir() {
        return getBlockState().getValue(BlockMultiblockCore.FACING);
    }

    public void cycleCraneRotation() {
        craneRotationOffset = (craneRotationOffset + 90) % 360;
        setChanged();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        setUpCrane = input.getBooleanOr("crane", false);
        craneRotationOffset = input.getIntOr("craneRotationOffset", 0);
        centerX = input.getIntOr("centerX", 0);
        centerY = input.getIntOr("centerY", 0);
        centerZ = input.getIntOr("centerZ", 0);
        spanF = input.getIntOr("spanF", 0);
        spanB = input.getIntOr("spanB", 0);
        spanL = input.getIntOr("spanL", 0);
        spanR = input.getIntOr("spanR", 0);
        height = input.getIntOr("height", 0);
        posFront = input.getDoubleOr("posFront", 0D);
        posLeft = input.getDoubleOr("posLeft", 0D);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("crane", setUpCrane);
        output.putInt("craneRotationOffset", craneRotationOffset);
        output.putInt("centerX", centerX);
        output.putInt("centerY", centerY);
        output.putInt("centerZ", centerZ);
        output.putInt("spanF", spanF);
        output.putInt("spanB", spanB);
        output.putInt("spanL", spanL);
        output.putInt("spanR", spanR);
        output.putInt("height", height);
        output.putDouble("posFront", posFront);
        output.putDouble("posLeft", posLeft);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeBoolean(setUpCrane);
        if (!setUpCrane) return;
        buffer.writeInt(craneRotationOffset);
        buffer.writeInt(centerX);
        buffer.writeInt(centerY);
        buffer.writeInt(centerZ);
        buffer.writeInt(spanF);
        buffer.writeInt(spanB);
        buffer.writeInt(spanL);
        buffer.writeInt(spanR);
        buffer.writeInt(height);
        buffer.writeDouble(posFront);
        buffer.writeDouble(posLeft);
        buffer.writeDouble(progress);
        buffer.writeBoolean(hasItemLoaded());
        buffer.writeDouble(loadedHeat);
        buffer.writeDouble(loadedEnrichment);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        setUpCrane = buffer.readBoolean();
        if (!setUpCrane) return;
        craneRotationOffset = buffer.readInt();
        centerX = buffer.readInt();
        centerY = buffer.readInt();
        centerZ = buffer.readInt();
        spanF = buffer.readInt();
        spanB = buffer.readInt();
        spanL = buffer.readInt();
        spanR = buffer.readInt();
        height = buffer.readInt();
        syncFront = buffer.readDouble();
        syncLeft = buffer.readDouble();
        syncProgress = buffer.readDouble();
        hasLoaded = buffer.readBoolean();
        loadedHeat = buffer.readDouble();
        loadedEnrichment = buffer.readDouble();
    }
}

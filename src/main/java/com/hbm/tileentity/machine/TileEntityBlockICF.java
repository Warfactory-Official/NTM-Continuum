package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModIcfBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The block entity behind every assembled ICF laser cell (1.7 {@code BlockICF.TileEntityBlockICF}). It remembers
 * the laser part the cell was assembled from and the controller it belongs to, puts that part back when the cell
 * is broken, and — on a port cell only — is the controller's power inlet.
 *
 * <p>1.7 stored the part as a block id plus metadata; the flattened part's registry id is the whole record here.
 * The entity was a plain {@code TileEntity}; it sits on {@link TileEntityLoadedBase} for the loaded check the
 * energy graph asks every receiver.
 */
public class TileEntityBlockICF extends TileEntityLoadedBase implements IEnergyReceiverMK2, com.hbm.tileentity.IRelocatable {

    /** A sub-level move carried this block; the controller follows when it was carried too. */
    @Override
    public void relocate(dev.golem.core.world.SubLevels.Move move) {
        net.minecraft.core.BlockPos core = com.hbm.tileentity.IRelocatable.follow(move, new net.minecraft.core.BlockPos(coreX, coreY, coreZ));
        coreX = core.getX(); coreY = core.getY(); coreZ = core.getZ();
        cachedCore = null;
        setChanged();
    }


    public @Nullable Block block;
    public int coreX;
    public int coreY;
    public int coreZ;

    private @Nullable TileEntityICFController cachedCore;

    public TileEntityBlockICF(BlockPos pos, BlockState state) {
        super(ModIcfBlockEntities.ICF_BLOCK.get(), pos, state);
    }

    /** 1.7 {@code updateEntity}: once a second, a cell whose controller is gone or disassembled reverts. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBlockICF cell) {
        if (level.getGameTime() % 20 != 0 || cell.block == null) return;

        TileEntityICFController controller = cell.getCore();
        if (controller != null) {
            if (!controller.assembled) BlockICF.revert(level, pos);
        } else if (level.hasChunk(cell.coreX >> 4, cell.coreZ >> 4)) {
            BlockICF.revert(level, pos);
        }
    }

    public boolean isPort() {
        return getBlockState().getBlock() instanceof BlockICF && getBlockState().getValue(BlockICF.PORT);
    }

    /** 1.7 {@code getCore}: the controller, cached while it stays valid, and only looked up in loaded chunks. */
    public @Nullable TileEntityICFController getCore() {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (level == null) return null;

        if (level.hasChunk(coreX >> 4, coreZ >> 4)) {
            BlockEntity tile = level.getBlockEntity(new BlockPos(coreX, coreY, coreZ));
            if (tile instanceof TileEntityICFController controller) {
                cachedCore = controller;
                return controller;
            }
        }
        return null;
    }

    /**
     * 1.7 {@code BlockICF.breakBlock}: breaking a cell puts the laser part it was assembled from back and flags
     * the controller as disassembled — the other cells then revert themselves within a second.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || block == null) return;
        Block original = block;
        block = null; // never restore twice
        level.setBlock(pos, original.defaultBlockState(), 3);
        BlockEntity controller = level.getBlockEntity(new BlockPos(coreX, coreY, coreZ));
        if (controller instanceof TileEntityICFController icf) icf.assembled = false;
    }

    // --- persistence ---

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (block != null) {
            output.putString("block", BuiltInRegistries.BLOCK.getKey(block).toString());
            output.putInt("cX", coreX);
            output.putInt("cY", coreY);
            output.putInt("cZ", coreZ);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        String id = input.getStringOr("block", "");
        block = null;
        if (!id.isEmpty()) {
            Block found;
            try {
                found = BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElse(null);
            } catch (RuntimeException malformed) {
                found = null;
            }
            if (found != null && found != Blocks.AIR) {
                block = found;
                coreX = input.getIntOr("cX", 0);
                coreY = input.getIntOr("cY", 0);
                coreZ = input.getIntOr("cZ", 0);
            }
        }
    }

    // --- energy: only an assembled port cell answers, and it answers with the controller's buffer ---

    private @Nullable TileEntityICFController portCore() {
        if (!isPort() || block == null) return null;
        return getCore();
    }

    @Override
    public long getPower() {
        TileEntityICFController controller = portCore();
        return controller != null ? controller.getPower() : 0;
    }

    @Override
    public void setPower(long power) {
        TileEntityICFController controller = portCore();
        if (controller != null) controller.setPower(power);
    }

    @Override
    public long getMaxPower() {
        TileEntityICFController controller = portCore();
        return controller != null ? controller.getMaxPower() : 0;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        if (!isPort()) return false;
        return dir != ForgeDirection.UNKNOWN;
    }
}

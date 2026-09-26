package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.api.block.IInsertable;
import com.hbm.sound.ModSounds;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.PistonInserter;
import com.hbm.tileentity.ModConveyorBlockEntities;

/** 1.7 {@code PistonInserter.TileEntityPistonInserter}: average coding session involving tile entities. */
public class TileEntityPistonInserter extends SyncedBlockEntity {

    public ItemStack slot = ItemStack.EMPTY;

    public int extend; // why don't we just make all these ones serverside? we're never using them on the client anyway
    public static final int MAX_EXTEND = 25;
    public boolean isRetracting = true;
    public int delay;

    // prevents funkies from happening with block updates or loading into a server
    public boolean lastState;

    // when a fake animatorcel gives you something so 20fps you gotta hit him with the true interpolation stare
    public double renderExtend;
    public double lastExtend;
    private int syncExtend;
    private int turnProgress;

    public TileEntityPistonInserter(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.PISTON_INSERTER.get(), pos, state);
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(PistonInserter.FACING) ? state.getValue(PistonInserter.FACING) : Direction.UP;
    }

    public void serverTick() {
        if (level == null) return;

        if (delay <= 0) {

            if (this.isRetracting && this.extend > 0) {
                this.extend--;
            } else if (!this.isRetracting) {
                this.extend++;

                if (this.extend >= MAX_EXTEND) {
                    level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                            ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.0F, 1.5F);

                    Direction dir = getFacing();
                    BlockPos target = worldPosition.relative(dir, 2);
                    Block b = level.getBlockState(target).getBlock();

                    if (b instanceof IInsertable insertable && !slot.isEmpty()
                            && insertable.insertItem(level, target, dir, slot)) {
                        slot.shrink(1);
                        if (slot.isEmpty()) slot = ItemStack.EMPTY;
                        setChanged();
                    }

                    this.isRetracting = true;
                    this.delay = 5;
                }
            }

        } else {
            delay--;
        }

        syncState();
    }

    public void clientTick() {
        this.lastExtend = this.renderExtend;

        if (this.turnProgress > 0) {
            this.renderExtend += (this.syncExtend - this.renderExtend) / (double) this.turnProgress;
            this.turnProgress--;
        } else {
            this.renderExtend = this.syncExtend;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(extend);
        ItemStacks.writeOptional(buf, slot);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.syncExtend = buf.readInt();
        this.slot = ItemStacks.readOptional(buf);
        this.turnProgress = 2;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("extend", extend);
        output.putBoolean("retract", isRetracting);
        output.putBoolean("state", lastState); // saved so loading into a world doesn't cause issues
        if (!slot.isEmpty()) output.store("stack", ItemStacks.optionalCodec(), slot);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.extend = input.getIntOr("extend", 0);
        this.isRetracting = input.getBooleanOr("retract", true);
        this.lastState = input.getBooleanOr("state", false);
        this.slot = input.read("stack", ItemStacks.optionalCodec()).orElse(ItemStack.EMPTY);
    }

    /** 1.7 {@code PistonInserter.breakBlock}: the single held stack is thrown into the world. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || slot.isEmpty()) return;
        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), slot);
        slot = ItemStack.EMPTY;
    }
}

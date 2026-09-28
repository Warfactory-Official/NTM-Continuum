package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerFileCabinet;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code TileEntityFileCabinet}: eight slots in two drawers. While someone has it open the lower drawer
 * slides out at 1/16 per tick and, ten ticks later, the upper one; closed, both slide back at 1/25, to at most
 * 0.8 out. The open/close clunks are the crate sounds, pitched as 1.7 had them. The server sends the timer and
 * the user count every tick (1.7 {@code networkPackNT(25)}) and both sides integrate the extents themselves.
 *
 * <p>No automation: it's a filing cabinet. It does not keep its contents when broken (1.7
 * {@code BlockDecoContainer.breakBlock} spills them).
 */
public class TileEntityFileCabinet extends TileEntityCrateBase {

    private int timer = 0;
    private int playersUsing = 0;
    public float lowerExtent = 0; // i don't know a term for how 'open' something is
    public float prevLowerExtent = 0;
    public float upperExtent = 0;
    public float prevUpperExtent = 0;

    public TileEntityFileCabinet(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.FILE_CABINET.get(), pos, state, 8);
    }

    @Override
    public String getDefaultName() {
        return "container.fileCabinet";
    }

    /** 1.7 ignored any custom name here. */
    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public void openInventory() {
        if (level != null && !level.isClientSide()) this.playersUsing++;
    }

    @Override
    public void closeInventory() {
        if (level != null && !level.isClientSide()) this.playersUsing--;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(timer);
        buf.writeInt(playersUsing);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        timer = buf.readInt();
        playersUsing = buf.readInt();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityFileCabinet cabinet) {
        cabinet.update(level);
    }

    private void update(Level level) {
        if (!level.isClientSide()) {
            if (this.playersUsing > 0) {
                if (timer < 10) timer++;
            } else {
                timer = 0;
            }
            networkPackNT();
        } else {
            this.prevLowerExtent = lowerExtent;
            this.prevUpperExtent = upperExtent;
        }

        float openSpeed = playersUsing > 0 ? 1F / 16F : 1F / 25F;
        float maxExtent = 0.8F;

        if (this.playersUsing > 0) {
            if (lowerExtent == 0F && upperExtent == 0F) {
                sound(level, ModSounds.CRATE_OPEN.get(), 0.8F, 1.0F);
            } else {
                if (upperExtent + openSpeed >= maxExtent && lowerExtent < maxExtent)
                    sound(level, ModSounds.CRATE_OPEN.get(), 0.5F, level.getRandom().nextFloat() * 0.1F + 0.7F);
                if (lowerExtent + openSpeed >= maxExtent && lowerExtent < maxExtent)
                    sound(level, ModSounds.CRATE_OPEN.get(), 0.5F, level.getRandom().nextFloat() * 0.1F + 0.7F);
            }

            this.lowerExtent += openSpeed;
            if (timer >= 10) this.upperExtent += openSpeed;

        } else if (lowerExtent > 0) {
            if (upperExtent - openSpeed < maxExtent / 2 && upperExtent >= maxExtent / 2 && upperExtent != lowerExtent)
                sound(level, ModSounds.CRATE_CLOSE.get(), 0.8F, 1.0F);
            if (lowerExtent - openSpeed < maxExtent / 2 && lowerExtent >= maxExtent / 2)
                sound(level, ModSounds.CRATE_CLOSE.get(), 0.8F, 1.0F);

            this.upperExtent -= openSpeed;
            this.lowerExtent -= openSpeed;
        }

        this.lowerExtent = Mth.clamp(lowerExtent, 0F, maxExtent);
        this.upperExtent = Mth.clamp(upperExtent, 0F, maxExtent);
    }

    /** 1.7 {@code worldObj.playSoundEffect}: heard only when the server plays it. */
    private void sound(Level level, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level.isClientSide()) return;
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                sound, SoundSource.BLOCKS, volume, pitch);
    }

    // No automation, it's a filing cabinet.
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    /** The drawers spill, as 1.7's {@code BlockDecoContainer.breakBlock} did; nothing rides on the item. */
    @Override
    protected net.minecraft.core.NonNullList<ItemStack> carriedItems() {
        return null;
    }

    @Override
    protected net.minecraft.nbt.@Nullable CompoundTag collectCarriedData() {
        return null;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) net.minecraft.world.Containers.dropContents(level, pos, this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerFileCabinet(containerId, inventory, this);
    }
}

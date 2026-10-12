package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.items.ModItems;
import com.hbm.items.ModToolItems;
import com.hbm.items.tool.ItemKey;
import com.hbm.Constants;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Inventories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.hbm.items.tool.ItemLock;
import com.hbm.sound.ModDoorSounds;

/**
 * Port of 1.7.10 {@code TileEntityLockableBase}: a pin-coded lock that a matching key, the red key
 * card or a pin-and-screwdriver pick can open.
 *
 * <p>The padlock/key-kit items that <i>set</i> a lock are not part of this family; what is declared
 * here is the lock surface those items will drive ({@link #setPins}, {@link #lock}, {@link #setMod})
 * and the access check the doors already use. A key item declares its pins through
 * {@link KeyPins}; the port's plain {@link ItemKey} carries none yet and never opens a lock.
 * The lock-picking success bonus the original granted to the {@code jackt} jackets is registered
 * through {@link #LOCKPICK_JACKETS} by whoever ports them.
 */
public abstract class TileEntityLockableBase extends TileEntityLoadedBase {

    /** A key item that carries a pin code. */
    public interface KeyPins {
        int getPins(ItemStack stack);
    }

    /**
     * Chest pieces that multiply the pick chance a hundredfold, in code; the data form is the {@code hbm:lockpick_jacket}
     * item data map (1.7's {@code jackt}/{@code jackt2}).
     */
    public static final List<Supplier<? extends Item>> LOCKPICK_JACKETS = new ArrayList<>();

    /**
     * Held items that configure a lock rather than operate the block (the original's {@code ItemLock}
     * padlocks and {@code key_kit}); a lockable block lets their own use handler run instead of
     * toggling. Set by whoever ports the padlock items.
     */
    public static java.util.function.Predicate<ItemStack> LOCK_TOOLS = stack -> false;

    protected int lock;
    private boolean isLocked = false;
    protected double lockMod = 0.1D;
    /** Whether a counterfeit lock can be made out of it */
    public boolean cheesable = true;

    protected TileEntityLockableBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void lock() {
        if (lock == 0) {
            Constants.LOG.error("A block has been set to locked state before setting pins, this should not happen and may cause errors! " + this);
        }
        isLocked = true;
        setChanged();
    }

    public void unlock() {
        isLocked = false;
        setChanged();
    }

    public void setPins(int pins) { lock = pins; setChanged(); }
    public int getPins() { return lock; }
    public void setMod(double mod) { lockMod = mod; setChanged(); }
    public double getMod() { return lockMod; }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        lock = nbt.getIntOr("lock", 0);
        cheesable = nbt.getBooleanOr("cheesable", false);
        isLocked = nbt.getBooleanOr("isLocked", false);
        lockMod = nbt.getDoubleOr("lockMod", 0D);
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        nbt.putInt("lock", lock);
        nbt.putBoolean("cheesable", cheesable);
        nbt.putBoolean("isLocked", isLocked);
        nbt.putDouble("lockMod", lockMod);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(lock);
        buf.writeBoolean(cheesable);
        buf.writeBoolean(isLocked);
        buf.writeDouble(lockMod);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        lock = buf.readInt();
        cheesable = buf.readBoolean();
        isLocked = buf.readBoolean();
        lockMod = buf.readDouble();
    }

    public boolean canAccess(@Nullable Player player) {

        if (!isLocked) {
            return true;
        } else {
            if (player == null)
                return false;
            ItemStack stack = player.getMainHandItem();

            if (!stack.isEmpty() && stack.getItem() instanceof KeyPins key && key.getPins(stack) == this.lock) {
                playAtPlayer(player, ModDoorSounds.LOCK_OPEN.get(), 1.0F, 1.0F);
                return true;
            }

            if (!stack.isEmpty() && stack.getItem() == ModItems.key_red.get()) {
                playAtPlayer(player, ModDoorSounds.LOCK_OPEN.get(), 1.0F, 1.0F);
                return true;
            }

            return tryPick(player);
        }
    }

    private void playAtPlayer(Player player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level != null) level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    private boolean tryPick(Player player) {

        boolean canPick = false;
        ItemStack stack = player.getMainHandItem();
        double chanceOfSuccess = this.lockMod * 100;
        Item pin = ModItems.pin.get();
        Item screwdriver = ModToolItems.screwdriver.get();

        if (!stack.isEmpty() && stack.getItem() == pin && hasItem(player, screwdriver)) {
            stack.shrink(1);
            canPick = true;
        }

        if (!stack.isEmpty() && stack.getItem() == screwdriver && hasItem(player, pin)) {
            consumeItem(player, pin);
            player.inventoryMenu.broadcastChanges();
            canPick = true;
        }

        if (canPick) {

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty() && Boolean.TRUE.equals(com.hbm.api.data.ArmorDataMaps.LOCKPICK_JACKET.get(chest.getItem()))) {
                chanceOfSuccess *= 100D;
            } else for (Supplier<? extends Item> jacket : LOCKPICK_JACKETS) {
                if (!chest.isEmpty() && chest.getItem() == jacket.get()) {
                    chanceOfSuccess *= 100D;
                    break;
                }
            }

            double rand = player.level().getRandom().nextDouble() * 100;

            if (chanceOfSuccess > rand) {
                playAtPlayer(player, ModDoorSounds.PIN_UNLOCK.get(), 1.0F, 1.0F);
                return true;
            }

            playAtPlayer(player, ModDoorSounds.PIN_BREAK.get(), 1.0F, 0.8F + player.level().getRandom().nextFloat() * 0.2F);
        }

        return false;
    }

    private static boolean hasItem(Player player, Item item) {
        NonNullList<ItemStack> main = Inventories.main(player.getInventory());
        for (ItemStack stack : main) if (!stack.isEmpty() && stack.getItem() == item) return true;
        return false;
    }

    private static void consumeItem(Player player, Item item) {
        NonNullList<ItemStack> main = Inventories.main(player.getInventory());
        for (ItemStack stack : main) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                return;
            }
        }
    }
}

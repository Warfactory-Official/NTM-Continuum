package com.hbm.tileentity.bomb;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerNukeFstbmb;
import com.hbm.tileentity.ModBombs2BlockEntities;

/**
 * The Balefire Bomb's tile: slot 0 takes the balefire egg, slot 1 a Spark or Trixite battery. Once loaded
 * the GUI's start button runs the countdown (default 15 minutes, pinging every second); at zero, or on
 * any external trigger, it removes itself and spawns a 250-block {@link EntityBalefire} with the balefire
 * Torex. Removing a part while counting resets the start flag but not the timer.
 */
public class TileEntityNukeBalefire extends TileEntityNukeBomb implements IControlReceiver {

    public boolean loaded;
    public boolean started;
    public int timer = 18000;

    public TileEntityNukeBalefire(BlockPos pos, BlockState state) {
        super(ModBombs2BlockEntities.NUKE_FSTBMB.get(), pos, state, 2);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityNukeBalefire bomb) {
        bomb.updateEntity();
    }

    public void updateEntity() {
        if (level == null || level.isClientSide()) return;

        this.loaded = this.isLoaded();
        if (!loaded) started = false;

        if (started) {
            timer--;
            if (timer % 20 == 0) {
                level.playSound(null, worldPosition, ModSounds.FSTBMB_PING.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            }
        }

        if (timer <= 0) {
            explode();
            return;
        }

        // 1.7: networkPackNT(250)
        syncState();
    }

    /** 1.7 {@code handleButtonPacket}: meta 0 starts (when loaded), meta 1 sets the timer in seconds. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == 0 && this.isLoaded()) {
            if (level != null) {
                level.playSound(null, worldPosition, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            }
            started = true;
        }
        if (meta == 1) timer = value * 20;
        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.getBooleanOr(data, "start", false)) handleButtonPacket(0, 0);
        if (data.contains("timer")) handleButtonPacket(Mth.clamp(Tags.getIntOr(data, "timer", 1), 1, 999), 1);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerNukeFstbmb menu && menu.getBomb() == this;
    }

    public boolean isLoaded() {
        return hasEgg() && hasBattery();
    }

    @Override
    public boolean isReady() {
        return isLoaded();
    }

    public boolean hasEgg() {
        ItemStack stack = inventory.getStackInSlot(0);
        return !stack.isEmpty() && stack.getItem() == ModItems.egg_balefire.get();
    }

    public boolean hasBattery() {
        return getBattery() > 0;
    }

    /** 1 for a Spark battery, 2 for Trixite, 0 otherwise. */
    public int getBattery() {
        ItemStack stack = inventory.getStackInSlot(1);
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == ModItems.battery_spark.get()) return 1;
        if (stack.getItem() == ModItems.battery_trixite.get()) return 2;
        return 0;
    }

    public void explode() {
        if (level == null || level.isClientSide()) return;
        clearSlots();
        level.destroyBlock(worldPosition, false);

        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        EntityBalefire bf = new EntityBalefire(ModNtmEntities.BALEFIRE.get(), level);
        bf.setPos(x, y, z);
        bf.destructionRange = 250;
        level.addFreshEntity(bf);
        EntityNukeTorex torex = EntityNukeTorex.statFacBale(level, x, y, z, 250);
        if (level.addFreshEntity(torex)) torex.startChunkLoading();
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        return mins.length() == 1 ? "0" + mins : mins;
    }

    public String getSeconds() {
        String secs = "" + ((timer / 20) % 60);
        return secs.length() == 1 ? "0" + secs : secs;
    }

    @Override
    public String getDefaultName() {
        return "container.nukeFstbmb";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeFstbmb(containerId, inventory, this);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("started", started);
        output.putInt("timer", timer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        started = input.getBooleanOr("started", false);
        timer = input.getIntOr("timer", 18000);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeInt(timer);
        buffer.writeBoolean(started);
        buffer.writeBoolean(loaded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        timer = buffer.readInt();
        started = buffer.readBoolean();
        loaded = buffer.readBoolean();
    }
}

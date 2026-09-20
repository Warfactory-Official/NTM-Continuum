package com.hbm.tileentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileChannel;
import dev.golem.core.data.DataIn;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ram that shoves a fuel rod into a horizontal channel. One stroke pushes a single rod in at the
 * mouth, which shunts every rod already in the channel one place deeper and drops the last one out of the
 * far end — so a pile is refuelled by feeding it, not by emptying it.
 *
 * <p>Upstream calls the ram's position {@code level}; here it is {@code travel}, because
 * {@code BlockEntity} already owns a field called {@code level}. Sided automation is per <i>face</i>
 * rather than per cell, which is what 26.1's {@code WorldlyContainer} asks.
 */
public class TileEntityPileLoader extends TileEntityPileDeviceBase implements WorldlyContainer, IRORValueProvider {

    /** Fraction of a full stroke the ram travels per tick. */
    public static final double SPEED = 1D / 7D;

    private static final int[] SLOTS = {0};

    public double travel;
    public double lastTravel;
    public double syncTravel;
    public int turnProgress;

    public boolean loading;
    public int delay;
    public boolean wasRedstone;

    public ItemStack stack = ItemStack.EMPTY;
    public ItemStack syncStack = ItemStack.EMPTY;

    /** The rod at the far end of the docked channel, mirrored for the overlay and the radio readouts. */
    public ItemStack channelStack = ItemStack.EMPTY;
    public double channelDepletion;
    public double channelTemp;

    public TileEntityPileLoader(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_LOADER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileLoader loader) {
        ForgeDirection dir = loader.getOrientation();
        PileChannel fuelChannel = null;
        loader.channelStack = ItemStack.EMPTY;
        loader.channelDepletion = 0D;
        loader.channelTemp = 0D;

        BlockPos port = pos.offset(-dir.offsetX, 0, -dir.offsetZ);
        BlockState portState = level.getBlockState(port);

        if (portState.is(ModPileBlocks.pile_block.get())
                && portState.getValue(BlockPile.META) == BlockPile.META_FUEL_IN
                && level.getBlockEntity(port) instanceof TileEntityPileBaseMK2 pile) {

            TileEntityPileCore core = pile.getCore();
            if (core != null) {
                fuelChannel = core.getFuelChannel(port.getX(), port.getY(), port.getZ());
                if (fuelChannel != null && fuelChannel.rods.length > 0) {
                    loader.chanNum = core.getFuelChannelNum(fuelChannel);
                    loader.channelStack = fuelChannel.rods[fuelChannel.rods.length - 1];
                    loader.channelDepletion = ItemPileRodMK2.getDepletionPercent(loader.channelStack);
                    loader.channelTemp = fuelChannel.heat;
                }
            }
        }

        boolean redstone = level.getSignal(pos.offset(dir.offsetX, 0, dir.offsetZ),
                dir.getOpposite().toDirection()) > 0;
        if (redstone && !loader.wasRedstone && loader.delay <= 0 && loader.travel <= 0D) loader.loading = true;
        loader.wasRedstone = redstone;

        if (loader.delay > 0) {
            loader.delay--;
        } else if (loader.loading) {
            if (loader.travel == 0D) {
                level.playSound(null, pos, ModGunSounds.BOLT_OPEN.get(), SoundSource.BLOCKS, loader.getVolume(1F), 1F);
            }
            loader.travel += SPEED;
            if (loader.travel >= 1D) {
                loader.travel = 1D;
                loader.loading = false;
                loader.delay = 5;
            }
        } else {
            if (loader.travel == 1D) {
                level.playSound(null, pos, ModGunSounds.BOLT_OPEN.get(), SoundSource.BLOCKS, loader.getVolume(1F), 0.75F);
                if (fuelChannel != null) {
                    fuelChannel.loadItem(loader.stack);
                    loader.setItem(0, ItemStack.EMPTY);
                }
            }
            if (loader.travel > 0D) {
                loader.travel = Math.max(0D, loader.travel - SPEED);
            }
        }

        loader.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPileLoader loader) {
        loader.lastTravel = loader.travel;

        if (loader.turnProgress > 0) {
            loader.travel += (loader.syncTravel - loader.travel) / loader.turnProgress;
            loader.turnProgress--;
        } else {
            loader.travel = loader.syncTravel;
        }
    }

    public static boolean isItemLoadable(ItemStack stack) {
        return stack.getItem() instanceof ItemPileRodMK2;
    }

    // --- inventory ---

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return stack.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return stack; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return false; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public void clearContent() { stack = ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount != 1 || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stack.copy();
        setItem(0, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.stack = itemStack;
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack itemStack) { return isItemLoadable(itemStack); }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction side) { return isItemLoadable(itemStack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction side) { return false; }

    // --- sync + save ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.travel);
        ItemStacks.writeOptional(buf, this.stack);
        ItemStacks.writeOptional(buf, this.channelStack);
        buf.writeDouble(this.channelDepletion);
        buf.writeDouble(this.channelTemp);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        double lastSync = this.syncTravel;
        this.syncTravel = buf.readDouble();
        this.syncStack = ItemStacks.readOptional(buf);
        this.channelStack = ItemStacks.readOptional(buf);
        this.channelDepletion = buf.readDouble();
        this.channelTemp = buf.readDouble();

        if (this.syncTravel != lastSync) this.turnProgress = 2;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("loading", loading);
        output.putDouble("level", travel);
        output.putInt("delay", delay);
        output.putBoolean("redstone", wasRedstone);
        if (!stack.isEmpty()) output.store("stack", ItemStack.CODEC, stack);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        loading = input.getBooleanOr("loading", false);
        travel = input.getDoubleOr("level", 0D);
        delay = input.getIntOr("delay", 0);
        wasRedstone = input.getBooleanOr("redstone", false);
        stack = input.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    // --- redstone over radio ---

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "meta",
                PREFIX_VALUE + "depletion",
                PREFIX_VALUE + "deppercent",
                PREFIX_VALUE + "lifetime",
                PREFIX_VALUE + "temp",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "meta").equals(name)) {
            EnumPileRod rod = ItemPileRodMK2.typeOf(channelStack);
            return rod == null ? "-1" : "" + rod.ordinal();
        }
        if ((PREFIX_VALUE + "deppercent").equals(name)) {
            return "" + (int) Math.round(this.channelDepletion);
        }
        if ((PREFIX_VALUE + "depletion").equals(name)) {
            return "" + (int) Math.round(ItemPileRodMK2.getDepletionPercent(channelStack));
        }
        if ((PREFIX_VALUE + "lifetime").equals(name)) {
            EnumPileRod rod = ItemPileRodMK2.typeOf(channelStack);
            return rod == null ? "0" : "" + (int) Math.round(rod.life);
        }
        if ((PREFIX_VALUE + "temp").equals(name)) {
            return "" + (int) Math.round(this.channelTemp);
        }
        return null;
    }
}

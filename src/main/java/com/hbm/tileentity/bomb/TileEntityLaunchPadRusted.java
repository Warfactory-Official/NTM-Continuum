package com.hbm.tileentity.bomb;

import com.hbm.api.event.HbmEvents;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.entity.missile.EntityMissileTier4;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerLaunchPadRusted;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.SubLevels;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code TileEntityLaunchPadRusted}: the derelict silo pad the missile silo structure leaves behind with a
 * rusted doomsday missile locked on it. Four slots: 0 the released missile (take-only), 1 launch codes, 2 launch
 * key, 3 designator. Codes, key and a ready designator fire the loaded missile (consuming the codes); the GUI's
 * release button instead drops the missile item into slot 0, after which it cannot be put back.
 *
 * <p>{@code missileLoaded} is only ever set by the structure generator, as upstream; a pad placed by hand starts
 * empty. Deferred: the launch smoke particles and the 500-block tracking range of the fired missile.
 */
public class TileEntityLaunchPadRusted extends TileEntityMachineBase implements MenuProvider, IControlReceiver {

    public int prevRedstonePower;
    public int redstonePower;
    private final LongOpenHashSet activatedBlocks = new LongOpenHashSet(4);

    public boolean missileLoaded;

    public TileEntityLaunchPadRusted(BlockPos pos, BlockState state) {
        super(ModCustomMissileBlockEntities.LAUNCH_PAD_RUSTED.get(), pos, state, 4);
    }

    @Override
    public String getDefaultName() {
        return "container.launchPadRusted";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPadRusted te) {
        if (te.redstonePower > 0 && te.prevRedstonePower <= 0) te.launch();
        te.prevRedstonePower = te.redstonePower;
        te.networkPackNT();
    }

    public BombReturnCode launch() {
        ItemStack codes = inventory.getStackInSlot(1);
        ItemStack key = inventory.getStackInSlot(2);
        ItemStack designatorStack = inventory.getStackInSlot(3);

        if (!codes.isEmpty() && !key.isEmpty() && !designatorStack.isEmpty() && this.missileLoaded) {
            if (codes.getItem() == ModItems.launch_code.get() && key.getItem() == ModItems.launch_key.get()) {
                if (designatorStack.getItem() instanceof IDesignatorItem designator) {
                    // Range and launch point are the pad as the world sees it (it may ride a moving sub-level).
                    BlockPos inWorld = SubLevels.toWorldBlock(level, worldPosition);
                    if (!designator.isReady(level, designatorStack, inWorld)) return BombReturnCode.ERROR_MISSING_COMPONENT;

                    Vec3 coords = designator.getCoords(level, designatorStack, inWorld);
                    int targetX = (int) Math.floor(coords.x);
                    int targetZ = (int) Math.floor(coords.z);

                    Vec3 launch = SubLevels.toWorld(level, worldPosition.getX() + 0.5D,
                            worldPosition.getY() + 1D, worldPosition.getZ() + 0.5D);
                    EntityMissileTier4.EntityMissileDoomsdayRusted missile = new EntityMissileTier4.EntityMissileDoomsdayRusted(
                            level, (float) launch.x, (float) launch.y, (float) launch.z,
                            targetX, targetZ);
                    level.addFreshEntity(missile);
                    HbmEvents.missileLaunched(level, worldPosition, missile, targetX, targetZ, null);
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5,
                            ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                    this.missileLoaded = false;
                    inventory.extractItem(1, 1, false);
                    setChanged();
                    return BombReturnCode.LAUNCHED;
                }
            }
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    public void updateRedstonePower(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos.asLong());
        if (!contained && powered) {
            activatedBlocks.add(pos.asLong());
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos.asLong());
            redstonePower--;
            if (redstonePower == 0) redstonePower = -1;
        }
        setChanged();
    }

    // --- IControlReceiver: the GUI's "release missile" button ---

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.getBooleanOr(data, "release", false)) {
            if (this.missileLoaded && inventory.getStackInSlot(0).isEmpty()) {
                this.missileLoaded = false;
                inventory.setStackInSlot(0, new ItemStack(ModMissileItems.missile_doomsday_rusted.get()));
                setChanged();
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != 0;
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.missileLoaded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.missileLoaded = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("missileLoaded", missileLoaded);
        output.putInt("redstonePower", redstonePower);
        output.putInt("prevRedstonePower", prevRedstonePower);
        int[] blocks = new int[activatedBlocks.size() * 3];
        int i = 0;
        for (long packed : activatedBlocks) {
            BlockPos p = BlockPos.of(packed);
            blocks[i++] = p.getX();
            blocks[i++] = p.getY();
            blocks[i++] = p.getZ();
        }
        output.putIntArray("activatedBlocks", blocks);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        missileLoaded = input.getBooleanOr("missileLoaded", false);
        redstonePower = input.getIntOr("redstonePower", 0);
        prevRedstonePower = input.getIntOr("prevRedstonePower", 0);
        activatedBlocks.clear();
        int[] blocks = input.getIntArrayOr("activatedBlocks", new int[0]);
        for (int i = 0; i + 2 < blocks.length; i += 3) {
            activatedBlocks.add(new BlockPos(blocks[i], blocks[i + 1], blocks[i + 2]).asLong());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerLaunchPadRusted(containerId, playerInventory, this);
    }
}

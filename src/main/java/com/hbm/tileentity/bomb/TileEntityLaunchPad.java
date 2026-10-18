package com.hbm.tileentity.bomb;

import dev.golem.core.data.Tags;

import com.hbm.inventory.container.ContainerLaunchPad;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * The small launch pad: 1.12.2's {@code TileEntityLaunchPad}. A 3×3 pad that fires anything from a V2
 * to an Atlas, with a 100-tick reload between launches.
 *
 * <p>The reload is also the "missing" timer: any tick without a fuelled, valid missile resets it to
 * 100, so a pad that has just been loaded still waits five seconds before it is {@code STATE_READY}.
 *
 * <p>Deferred: the launch smoke plume (its particle family is unported).
 */
public class TileEntityLaunchPad extends TileEntityLaunchPadBase implements MenuProvider {

    /** Ticks left before this pad can fire again. */
    public int delay = 0;

    public TileEntityLaunchPad(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD.get(), pos, state, 7);
    }

    @Override
    public String getDefaultName() {
        return "container.launchPad";
    }

    @Override
    public boolean isReadyForLaunch() {
        return delay <= 0;
    }

    @Override
    public double getLaunchOffset() {
        return 1D;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPad te) {
        if (te.delay > 0) te.delay--;

        if (!te.isMissileValid() || !te.hasFuel()) {
            te.delay = 100;
            te.state = STATE_MISSING;
        } else {
            te.state = te.delay > 0 ? STATE_LOADING : STATE_READY;
        }

        te.serverTickBase(level);
    }

    @Override
    public void finalizeLaunch(Entity missile) {
        super.finalizeLaunch(missile);
        this.delay = 100;
    }

    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.refinery(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.launchPad");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerLaunchPad(containerId, playerInventory, this);
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(this.delay);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.delay = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("delay", delay);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        delay = input.getIntOr("delay", 0);
    }
}

package com.hbm.tileentity.machine.pile;

import net.minecraft.core.Direction;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileChannel;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The blower that forces compressed air down a ventilation channel. It only ever pushes air inward; the
 * cooling itself is the core's job, which spends the channel's air on whatever fuel sits at its height.
 */
public class TileEntityPileVent extends TileEntityPileDeviceBase implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM compair;
    public boolean isActive;

    public float fan;
    public float lastFan;

    public TileEntityPileVent(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_VENT.get(), pos, state);
        this.compair = new FluidTankNTM(Fluids.AIR, 4_000).withPressure(1);
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{compair}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{compair}; }

    @Override
    public boolean canConnect(FluidType type, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return type == compair.getTankType() && dir == getOrientation();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileVent vent) {
        ForgeDirection dir = vent.getOrientation();
        vent.isActive = false;

        BlockPos inlet = pos.offset(-dir.offsetX, 0, -dir.offsetZ);
        BlockState inletState = level.getBlockState(inlet);

        if (inletState.is(ModPileBlocks.pile_block.get())
                && inletState.getValue(BlockPile.META) == BlockPile.META_AIR_IN
                && level.getBlockEntity(inlet) instanceof TileEntityPileBaseMK2 pile) {

            TileEntityPileCore core = pile.getCore();
            if (core != null) {
                PileChannel channel = core.getVentilationChannel(inlet.getX(), inlet.getY(), inlet.getZ());
                if (channel != null) {
                    vent.chanNum = core.getVentilationChannelNum(channel);
                    int toFill = Math.min(vent.compair.getFill(), PileChannel.MAX_AIR - channel.air);
                    channel.air += toFill;
                    vent.compair.setFill(vent.compair.getFill() - toFill);
                    vent.isActive = toFill > 0;
                }
            }
        }

        vent.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPileVent vent) {
        vent.lastFan = vent.fan;

        if (vent.isActive) {
            vent.fan += 45F;
            if (level.getRandom().nextInt(20) == 0) {
                level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D,
                        0D, 0.05D, 0D);
            }
        }

        if (vent.fan >= 360F) {
            vent.lastFan -= 360F;
            vent.fan -= 360F;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        compair.writeToNBT(output, "t");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        compair.readFromNBT(input, "t");
    }
}

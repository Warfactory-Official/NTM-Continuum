package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Refueler — 1:1 port of the 1.12.2 {@code TileEntityRefueler}: a single-block fuel pad with a
 * {@code KEROSENE 100} tank. Each tick it fills any {@link IFillableItem} in the inventory of players
 * standing on it (AABB expanded ±0.5), playing the extinguish sound every 20 op-ticks. Fluid enters via
 * a pipe on the side opposite the facing.
 *
 * <p>Deferred (§20): the {@code ArmorModHandler} jetpack-mod branch (armor mods unported) and the
 * {@code HbmEffectNT.FluidFill} particle (skipped). {@code fillLevel} interp is kept for the renderer.
 */
public class TileEntityRefueler extends TileEntityLoadedBase implements IFluidStandardReceiverMK2 {

    public double fillLevel;
    public double prevFillLevel;

    public boolean isOperating = false;
    private int operatingTime;

    public FluidTankNTM tank;

    public TileEntityRefueler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFUELER.get(), pos, state);
        tank = new FluidTankNTM(Fluids.KEROSENE, 100);
    }

    private ForgeDirection portDir() {
        Direction facing = getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        return ForgeDirection.getOrientation(facing.get3DDataValue()).getOpposite();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRefueler te) {
        ForgeDirection dir = te.portDir();
        te.trySubscribe(te.tank.getTankType(), level, pos.getX() + dir.offsetX, pos.getY(), pos.getZ() + dir.offsetZ, dir);

        te.isOperating = false;

        AABB box = new AABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).inflate(0.5, 0.0, 0.5);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);

        for (Player player : players) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                if (te.fillFillable(stack)) te.isOperating = true;
            }
        }

        if (te.isOperating) {
            if (te.operatingTime % 20 == 0) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
            te.operatingTime++;
        } else {
            te.operatingTime = 0;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityRefueler te) {
        te.prevFillLevel = te.fillLevel;
        double target = (double) te.tank.getFill() / (double) te.tank.getMaxFill();
        double speed = (target > te.fillLevel || !te.isOperating) ? 0.1 : 0.01;
        te.fillLevel += (target - te.fillLevel) * speed;
    }

    private boolean fillFillable(ItemStack stack) {
        if (stack.getItem() instanceof IFillableItem fillable && fillable.acceptsFluid(tank.getTankType(), stack)) {
            int prevFill = tank.getFill();
            tank.setFill(fillable.tryFill(tank.getTankType(), tank.getFill(), stack));
            return tank.getFill() < prevFill;
        }
        return false;
    }

    public void setTankType(com.hbm.inventory.fluid.FluidType type) {
        tank.setTankType(type);
        setChanged();
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOperating);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOperating = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.writeToNBT(output, "t");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.readFromNBT(input, "t");
    }
}

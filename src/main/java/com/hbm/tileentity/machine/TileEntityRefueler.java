package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.machine.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Refueler — 1:1 port of the 1.12.2 {@code TileEntityRefueler}: a single-block fuel pad with a
 * {@code KEROSENE 100} tank. Each tick it fills any {@link IFillableItem} in the inventory of players
 * standing on it (AABB expanded ±0.5), playing the extinguish sound every 20 op-ticks. Fluid enters via
 * a pipe on the side opposite the facing.
 */
public class TileEntityRefueler extends SyncedBlockEntity implements IFluidStandardReceiverMK2 {

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

        te.isOperating = false;

        AABB box = new AABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).inflate(0.5, 0.0, 0.5);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);

        for (Player player : players) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                te.isOperating |= te.fillFillable(stack);
                if (ArmorModHandler.hasMods(stack)) {
                    for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
                        if (mod.isEmpty() || !te.fillFillable(mod)) continue;
                        ArmorModHandler.applyMod(stack, mod);
                        te.isOperating = true;
                    }
                }
            }

            // Armor inventory is separate from Inventory's 36 carried slots on modern Minecraft.
            for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                    net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                te.isOperating |= te.fillFillable(stack);
                if (ArmorModHandler.hasMods(stack)) {
                    for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
                        if (mod.isEmpty() || !te.fillFillable(mod)) continue;
                        ArmorModHandler.applyMod(stack, mod);
                        te.isOperating = true;
                    }
                }
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

        te.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityRefueler te) {
        if (te.isOperating) {
            ForgeDirection dir = te.portDir();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            var random = level.getRandom();
            CompoundTag data = new CompoundTag();
            data.putInt("color", te.tank.getTankType().getColor());
            data.putDouble("mX", -dir.offsetX + random.nextGaussian() * 0.1D);
            data.putDouble("mY", 0D);
            data.putDouble("mZ", -dir.offsetZ + random.nextGaussian() * 0.1D);
            HbmEffectNT.FluidFill.summonParticle(level,
                    pos.getX() + 0.5D + random.nextDouble() * 0.0625D + dir.offsetX * 0.5D + rot.offsetX * 0.25D,
                    pos.getY() + 0.375D,
                    pos.getZ() + 0.5D + random.nextDouble() * 0.0625D + dir.offsetZ * 0.5D + rot.offsetZ * 0.25D,
                    data);
        }
        te.prevFillLevel = te.fillLevel;
        double target = (double) te.tank.getFill() / (double) te.tank.getMaxFill();
        double speed = (target > te.fillLevel || !te.isOperating) ? 0.1 : 0.01;
        te.fillLevel += (target - te.fillLevel) * speed;
    }

    private boolean fillFillable(ItemStack stack) {
        if (stack.getItem() instanceof IFillableItem fillable && fillable.acceptsFluid(tank.getTankType(), stack)) {
            int offered = tank.getFill();
            int remainder = fillable.tryFill(tank.getTankType(), offered, stack);
            int moved = offered - remainder;
            if (moved <= 0) return false;
            // The fill API returns the amount left in the source tank; drill magazines accept at most 50mB.
            tank.setFill(remainder);
            return true;
        }
        return false;
    }

    public void setTankType(com.hbm.api.fluid.FluidType type) {
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
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "t");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "t");
    }
}

package com.hbm.tileentity.machine.oil;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * Pumpjack, 1:1 port of the 1.12.2 {@code TileEntityMachinePumpjack} (config name "pumpjack"):
 * maxPower 250k, 200 HE per eligible tick, delay 25t, 750 mB oil + 50-250 mB gas per deposit, 2.5%
 * drain chance. The beam animation speed is computed server-side ({@code 5 + 2*speed + (over-1)*10},
 * 0 when idle) and appended to the sync packet; the client ticker integrates it into {@code rot}.
 * Shares the derrick's container/GUI.
 */
public class TileEntityMachinePumpjack extends TileEntityOilDrillBase {

    protected static final int maxPower = 250_000;
    protected static final int consumption = 200;
    protected static final int delay = 25;
    protected static final int oilPerDeposit = 750;
    protected static final int gasPerDepositMin = 50;
    protected static final int gasPerDepositMax = 250;
    protected static final double drainChance = 0.025D;

    public float rot = 0;
    public float prevRot = 0;
    public float speed = 0;

    public TileEntityMachinePumpjack(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMPJACK.get(), pos, state);
    }

    @Override
    public String getDefaultName() {
        return "container.pumpjack";
    }

    @Override public long getMaxPower() { return maxPower; }
    @Override public int getPowerReq() { return consumption; }
    @Override public int getDelay() { return delay; }

    /** Client ticker: advance the beam rotation by the synced speed (old client-side update half). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePumpjack te) {
        te.prevRot = te.rot;

        if (te.indicator == 0) {
            te.rot += te.speed;
        }

        if (te.rot >= 360) {
            te.prevRot -= 360;
            te.rot -= 360;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(this.indicator == 0 ? (5F + (2F * this.speedLevel)) + (this.overLevel - 1F) * 10 : 0F);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.speed = buf.readFloat();
    }

    @Override
    public void onDrill(int y) {
        Block gas = gasOfDrilledOre(y);
        if (gas == null) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) releaseGas(worldPosition.relative(dir), gas);
    }

    @Override
    public void onSuck(BlockPos pos, DepositDataMaps.Deposit deposit) {
        suckDeposit(pos, deposit, oilPerDeposit, gasPerDepositMin, gasPerDepositMax, drainChance);
    }

    protected PortDef[] cachedPorts;
    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = ForgeDirection.getOrientation(MachineCore.facingOrdinal(getBlockState(), 2));
            ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX * 2 + dir.offsetX * 1, y, z + rot.offsetZ * 2 + dir.offsetZ * 1, dir),
                    PortDef.make(x + rot.offsetX * 2 - dir.offsetX * 1, y, z + rot.offsetZ * 2 - dir.offsetZ * 1, dir.getOpposite()),
                    PortDef.make(x + rot.offsetX * 4 + dir.offsetX * 1, y, z + rot.offsetZ * 4 + dir.offsetZ * 1, dir),
                    PortDef.make(x + rot.offsetX * 4 - dir.offsetX * 1, y, z + rot.offsetZ * 4 - dir.offsetZ * 1, dir.getOpposite()),
            };
        }
        return cachedPorts;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.pumpjack");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineOilWell(containerId, playerInventory, this);
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_pumpjack.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 25) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 10) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.AFTERBURN) {
            info.add(Component.translatable(KEY_BURN, level * 10, level * 50).withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }
}

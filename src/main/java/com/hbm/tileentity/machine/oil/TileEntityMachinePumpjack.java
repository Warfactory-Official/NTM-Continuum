package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Pumpjack — 1:1 port of the 1.12.2 {@code TileEntityMachinePumpjack} (config name "pumpjack"):
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
    public void onSuck(BlockPos pos, com.hbm.api.data.DepositDataMaps.Deposit deposit) {
        suckDeposit(pos, deposit, oilPerDeposit, gasPerDepositMin, gasPerDepositMax, drainChance);
    }

    @Override
    public DirPos[] getConPos() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        BlockPos pos = worldPosition;

        // The 4 fluid/power ports sit on the 4 `makeExtra` corner cells of the pump base — at
        // {@code core + rot*3 ± 1} on both world axes (see MachinePumpjack.fillSpace) — each exposing
        // its face along the machine's facing axis. (The 1.12.2 getConPos derived these with a
        // ±2/±4 offset formula that only lands all 4 for N/S‑facing pumpjacks; for E/W facings its
        // ×4 term aimed 2 ports at non‑existent cells — the "missing 2 ports" bug. Recomputed from the
        // corner cells so all 4 ports work at every facing; N/S behaviour is unchanged. §20 deviation.)
        int bx = pos.getX() + rot.offsetX * 3;
        int bz = pos.getZ() + rot.offsetZ * 3;
        DirPos[] ports = new DirPos[4];
        int i = 0;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                int cx = bx + dx, cz = bz + dz;             // corner cell
                // Exposed face = the machine's facing axis; sign = which side the corner sits on.
                int comp = (cx - pos.getX()) * dir.offsetX + (cz - pos.getZ()) * dir.offsetZ;
                ForgeDirection pd = comp >= 0 ? dir : dir.getOpposite();
                ports[i++] = new DirPos(cx + pd.offsetX, pos.getY(), cz + pd.offsetZ, pd);
            }
        }
        return ports;
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

package com.hbm.tileentity.machine.albion;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.ContainerPAQuadrupole;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.items.machine.ItemPACoil.EnumCoilType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.TileEntityPASource.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Quadrupole magnet: it squeezes the beam back together, undoing the defocus every RF cavity adds. Runs
 * on a {@link ItemPACoil coil} whose momentum window decides both the power draw and whether the
 * particle survives the pass. Ported from 1.12.2's {@code albion/TileEntityPAQuadrupole}.
 */
public class TileEntityPAQuadrupole extends TileEntityCooledBase
        implements MenuProvider, IParticleUser, IRORValueProvider {

    public static final long usage = 100_000;
    public static final int focusGain = 100;

    public TileEntityPAQuadrupole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_QUADRUPOLE.get(), pos, state, 2);
    }

    @Override
    public long getMaxPower() {
        return 2_500_000;
    }

    @Override
    public String getDefaultName() {
        return "container.paQuadrupole";
    }

    private ForgeDirection getBeamDir() {
        return MachineCore.facing(getBlockState())
                .getRotation(ForgeDirection.DOWN);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPAQuadrupole te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.getMaxPower());
        te.exchangeCoolant(level);
        te.updateCooling();
        te.networkPackNT();
    }

    @Override
    public boolean canParticleEnter(Particle particle, ForgeDirection dir, BlockPos pos) {
        ForgeDirection beamlineDir = getBeamDir();
        BlockPos input = worldPosition.offset(-beamlineDir.offsetX, -beamlineDir.offsetY, -beamlineDir.offsetZ);
        return input.equals(pos) && beamlineDir == dir;
    }

    @Override
    public void onEnter(Particle particle, ForgeDirection dir) {
        EnumCoilType type = ItemPACoil.typeOf(inventory.getStackInSlot(1));

        int mult = 1;
        if (type != null) mult = type.quadMin > particle.momentum ? 10 : 1;

        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (this.power < usage * mult) particle.crash(PAState.CRASH_NOPOWER);
        if (type == null) particle.crash(PAState.CRASH_NOCOIL);
        if (type != null && type.quadMax < particle.momentum) particle.crash(PAState.CRASH_OVERSPEED);

        if (particle.invalid) return;

        particle.addDistance(3);
        particle.focus(focusGain);
        this.power -= usage * mult;
    }

    @Override
    public @Nullable BlockPos getExitPos(Particle particle) {
        ForgeDirection beamlineDir = getBeamDir();
        return worldPosition.offset(beamlineDir.offsetX * 2, beamlineDir.offsetY * 2, beamlineDir.offsetZ * 2);
    }

    @Override
    public List<DirPos> getConPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x, y + 2, z, Library.POS_Y),
                new DirPos(x, y - 2, z, Library.NEG_Y),
                new DirPos(x + dir.offsetX * 2, y, z + dir.offsetZ * 2, dir),
                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        return slot == 1;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerPAQuadrupole(id, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "temperature",
                PREFIX_VALUE + "pfmcold",
                PREFIX_VALUE + "pfm"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "temperature").equals(name)) return "" + (int) this.temperature;
        if ((PREFIX_VALUE + "pfmcold").equals(name)) return "" + coolantTanks[0].getFill();
        if ((PREFIX_VALUE + "pfm").equals(name)) return "" + coolantTanks[1].getFill();
        return null;
    }
}

package com.hbm.tileentity.machine.albion;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.ContainerPARFC;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
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
 * Radio-frequency cavity: the accelerator's only source of momentum. Every pass adds a flat
 * {@link #momentumGain} and the same amount of defocus, which is why a working ring needs quadrupoles
 * between its cavities. Ported from 1.12.2's {@code albion/TileEntityPARFC}.
 */
public class TileEntityPARFC extends TileEntityCooledBase implements MenuProvider, IParticleUser, IRORValueProvider {

    public static final long usage = 250_000;
    public static final int momentumGain = 100;
    public static final int defocusGain = 100;

    public TileEntityPARFC(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_RFC.get(), pos, state, 1);
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    @Override
    public String getDefaultName() {
        return "container.paRFC";
    }

    private ForgeDirection getBeamDir() {
        return ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset)
                .getRotation(ForgeDirection.DOWN);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPARFC te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.getMaxPower());
        te.exchangeCoolant(level);
        te.updateCooling();
        te.networkPackNT();
    }

    @Override
    public boolean canParticleEnter(Particle particle, ForgeDirection dir, BlockPos pos) {
        ForgeDirection rfcDir = getBeamDir();
        BlockPos input = worldPosition.offset(-rfcDir.offsetX * 4, -rfcDir.offsetY * 4, -rfcDir.offsetZ * 4);
        return input.equals(pos) && rfcDir == dir;
    }

    @Override
    public void onEnter(Particle particle, ForgeDirection dir) {
        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (this.power < usage) particle.crash(PAState.CRASH_NOPOWER);

        if (particle.invalid) return;

        particle.addDistance(9);
        particle.momentum += momentumGain;
        particle.defocus(defocusGain);
        this.power -= usage;
    }

    @Override
    public @Nullable BlockPos getExitPos(Particle particle) {
        ForgeDirection beamlineDir = getBeamDir();
        return worldPosition.offset(beamlineDir.offsetX * 5, beamlineDir.offsetY * 5, beamlineDir.offsetZ * 5);
    }

    @Override
    public List<DirPos> getConPos() {
        ForgeDirection dir = getBeamDir().getOpposite();
        BlockPos p = worldPosition;
        return List.of(
                new DirPos(p.getX() + dir.offsetX * 3, p.getY() + 2, p.getZ() + dir.offsetZ * 3, Library.POS_Y),
                new DirPos(p.getX() - dir.offsetX * 3, p.getY() + 2, p.getZ() - dir.offsetZ * 3, Library.POS_Y),
                new DirPos(p.getX(), p.getY() + 2, p.getZ(), Library.POS_Y),
                new DirPos(p.getX() + dir.offsetX * 3, p.getY() - 2, p.getZ() + dir.offsetZ * 3, Library.NEG_Y),
                new DirPos(p.getX() - dir.offsetX * 3, p.getY() - 2, p.getZ() - dir.offsetZ * 3, Library.NEG_Y),
                new DirPos(p.getX(), p.getY() - 2, p.getZ(), Library.NEG_Y));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerPARFC(id, playerInventory, this);
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

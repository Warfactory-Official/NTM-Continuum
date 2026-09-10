package com.hbm.tileentity.machine.albion;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerPADipole;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.items.machine.ItemPACoil.EnumCoilType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.TileEntityPASource.Particle;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
 * Dipole magnet: the accelerator's junction. It takes the beam from any of the four horizontal
 * neighbours and sends it out along whichever of three configured directions applies — one below the
 * momentum {@link #threshold}, one above it, and one while any of its connection points is powered.
 * Steering the beam resets the straight-line distance the dipole coils care about; passing straight
 * through does not.
 *
 * <p>Ported from 1.12.2's {@code albion/TileEntityPADipole}.
 */
public class TileEntityPADipole extends TileEntityCooledBase
        implements MenuProvider, IControlReceiver, IParticleUser, IRORValueProvider, IRORInteractive {

    public static final long usage = 100_000;

    public int dirLower;
    public int dirUpper;
    public int dirRedstone;
    public int threshold;

    public TileEntityPADipole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DIPOLE.get(), pos, state, 2);
    }

    public static ForgeDirection ditToForgeDir(int dir) {
        if (dir == 1) return ForgeDirection.EAST;
        if (dir == 2) return ForgeDirection.SOUTH;
        if (dir == 3) return ForgeDirection.WEST;
        return ForgeDirection.NORTH;
    }

    @Override
    public long getMaxPower() {
        return 2_500_000;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == ForgeDirection.UP || dir == ForgeDirection.DOWN;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir == ForgeDirection.UP || dir == ForgeDirection.DOWN;
    }

    @Override
    public String getDefaultName() {
        return "container.paDipole";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPADipole te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.getMaxPower());
        te.exchangeCoolant(level);
        te.updateCooling();
        te.networkPackNT();
    }

    @Override
    public boolean canParticleEnter(Particle particle, ForgeDirection dir, BlockPos pos) {
        return worldPosition.getY() == pos.getY()
                && (worldPosition.getX() == pos.getX() || worldPosition.getZ() == pos.getZ());
    }

    @Override
    public void onEnter(Particle particle, ForgeDirection dir) {
        EnumCoilType type = ItemPACoil.typeOf(inventory.getStackInSlot(1));
        boolean isInline = dir.equals(getExitDir(particle));

        int mult = 1;
        if (type != null) {
            if (type.diMin > particle.momentum) mult *= 10;
            if (type.diDistMin > particle.distanceTraveled) mult *= 10;
            if (isInline) mult = 1;
        }

        if (!isCool()) particle.crash(PAState.CRASH_NOCOOL);
        if (this.power < usage * mult) particle.crash(PAState.CRASH_NOPOWER);
        if (type == null) particle.crash(PAState.CRASH_NOCOIL);
        if (type != null && type.diMax < particle.momentum && !isInline) particle.crash(PAState.CRASH_OVERSPEED);

        if (particle.invalid) return;

        if (isInline) {
            particle.addDistance(3);
        } else {
            particle.resetDistance();
        }

        this.power -= usage * mult;
    }

    @Override
    public @Nullable BlockPos getExitPos(Particle particle) {
        particle.dir = getExitDir(particle);
        return worldPosition.offset(particle.dir.offsetX * 2, particle.dir.offsetY * 2, particle.dir.offsetZ * 2);
    }

    public ForgeDirection getExitDir(Particle particle) {
        int dit = particle.momentum < this.threshold
                ? dirLower : checkRedstone()
                ? dirRedstone : dirUpper;
        return ditToForgeDir(dit);
    }

    public boolean checkRedstone() {
        if (level == null) return false;
        for (DirPos pos : getConPos()) {
            if (level.hasNeighborSignal(pos.getPos())) return true;
        }
        return false;
    }

    @Override
    public List<DirPos> getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x + 1, y + 2, z, Library.POS_Y),
                new DirPos(x - 1, y + 2, z, Library.POS_Y),
                new DirPos(x, y + 2, z + 1, Library.POS_Y),
                new DirPos(x, y + 2, z - 1, Library.POS_Y),
                new DirPos(x + 1, y - 2, z, Library.NEG_Y),
                new DirPos(x - 1, y - 2, z, Library.NEG_Y),
                new DirPos(x, y - 2, z + 1, Library.NEG_Y),
                new DirPos(x, y - 2, z - 1, Library.NEG_Y));
    }

    // --- sync + save ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(dirLower);
        buf.writeInt(dirUpper);
        buf.writeInt(dirRedstone);
        buf.writeInt(threshold);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        dirLower = buf.readInt();
        dirUpper = buf.readInt();
        dirRedstone = buf.readInt();
        threshold = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("dirLower", dirLower);
        output.putInt("dirUpper", dirUpper);
        output.putInt("dirRedstone", dirRedstone);
        output.putInt("threshold", threshold);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        dirLower = input.getIntOr("dirLower", 0);
        dirUpper = input.getIntOr("dirUpper", 0);
        dirRedstone = input.getIntOr("dirRedstone", 0);
        threshold = input.getIntOr("threshold", 0);
    }

    // --- GUI ---

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
        return new ContainerPADipole(id, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.getBooleanOr(data, "lower", false)) this.dirLower++;
        if (Tags.getBooleanOr(data, "upper", false)) this.dirUpper++;
        if (Tags.getBooleanOr(data, "redstone", false)) this.dirRedstone++;
        if (Tags.hasInt(data, "threshold")) this.threshold = Tags.getIntOr(data, "threshold", 0);

        if (this.dirLower > 3) this.dirLower -= 4;
        if (this.dirUpper > 3) this.dirUpper -= 4;
        if (this.dirRedstone > 3) this.dirRedstone -= 4;

        this.threshold = Mth.clamp(threshold, 0, 999_999_999);
        this.markChanged();
    }

    public static String dirToName(int dir) {
        if (dir == 1) return "east";
        if (dir == 2) return "south";
        if (dir == 3) return "west";
        return "north";
    }

    public static int nameToDir(String name) {
        if (name.equals("north")) return 0;
        if (name.equals("east")) return 1;
        if (name.equals("south")) return 2;
        if (name.equals("west")) return 3;
        return -1;
    }

    // --- redstone over radio ---

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "temperature",
                PREFIX_VALUE + "pfmcold",
                PREFIX_VALUE + "pfm",
                PREFIX_FUNCTION + "setthreshold" + NAME_SEPARATOR + "threshold"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "temperature").equals(name)) return "" + (int) this.temperature;
        if ((PREFIX_VALUE + "pfmcold").equals(name)) return "" + coolantTanks[0].getFill();
        if ((PREFIX_VALUE + "pfm").equals(name)) return "" + coolantTanks[1].getFill();
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setthreshold").equals(name) && params.length > 0) {
            this.threshold = IRORInteractive.parseInt(params[0], 0, 999_999_999);
            this.markChanged();
        }
        return null;
    }
}

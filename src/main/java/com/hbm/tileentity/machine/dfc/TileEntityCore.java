package com.hbm.tileentity.machine.dfc;

import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.container.ContainerCore;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemCatalyst;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.lib.ModDamageSources;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Raycasts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dark fusion core itself: a single block that the emitters aim at. Beam energy arriving through
 * {@link #burn(long)} is multiplied by the exotic core in its middle slot and by both fuels, comes back
 * out as more beam energy, and leaves heat behind. The stabilisers' field is the only thing keeping that
 * heat from becoming a crater — the core spends one point of field per tick, so a reaction that outruns
 * its stabilisers detonates on the tick the field runs out.
 *
 * <p>Deviations from CE: a running core does not grant the dark-fusion advancement, which is not ported,
 * and the detonation uses {@link EntityNukeExplosionMK5}, this port's nuke entity, in place of upstream's
 * MK3.
 */
public class TileEntityCore extends TileEntityMachineBase implements MenuProvider {

    public int field;
    public int heat;
    public int prevHeat;
    public int color;
    public FluidTankNTM[] tanks;
    public boolean meltdownTick;
    protected int consumption;
    protected int prevConsumption;
    private boolean lastTickValid;

    public TileEntityCore(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_CORE.get(), pos, state, 3);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.DEUTERIUM, 128_000);
        tanks[1] = new FluidTankNTM(Fluids.TRITIUM, 128_000);
    }

    @Override
    public String getDefaultName() {
        return "container.dfcCore";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCore core) {
        core.tickServer(level);
    }

    protected void tickServer(Level level) {
        this.prevConsumption = this.consumption;
        this.consumption = 0;
        this.meltdownTick = false;

        BlockPos pos = getBlockPos();
        this.lastTickValid = level.hasChunkAt(pos)
                && level.hasChunkAt(pos.offset(16, 0, 16)) && level.hasChunkAt(pos.offset(16, 0, -16))
                && level.hasChunkAt(pos.offset(-16, 0, 16)) && level.hasChunkAt(pos.offset(-16, 0, -16));

        if (shouldDetonate()) {
            if (blastSuppressed(level)) {
                this.meltdownTick = true;
                ChunkRadiationManager.proxy.incrementRad(level, pos, 100D);
            } else {
                detonate(level);
            }
        }

        ItemStack left = inventory.getStackInSlot(0);
        ItemStack right = inventory.getStackInSlot(2);
        if (left.getItem() instanceof ItemCatalyst first && right.getItem() instanceof ItemCatalyst second) {
            color = averageHex(first.getColor(), second.getColor());
        } else {
            color = 0;
        }

        if (heat > 0) {
            radiation(level);
            if (level.getGameTime() % 100 == 0) {
                SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                        SatelliteRayScan.RayEvent.INFO_PARTICLE, 200);
            }
        }

        prevHeat = heat;
        networkPackNT();
        heat = 0;

        if (lastTickValid && field > 0) field -= 1;

        setChanged();
    }

    /** Whether the reaction has outrun the field the stabilisers are holding around it. */
    public boolean shouldDetonate() {
        return lastTickValid && heat > 0 && heat >= field;
    }

    /** How wide the blast would be: the fuel left in it, scaled by how hot it had run, 50 to 1000. */
    public int blastSize() {
        int fill = tanks[0].getFill() + tanks[1].getFill();
        int max = tanks[0].getMaxFill() + tanks[1].getMaxFill();
        if (max <= 0) return 50;
        return Math.max(Math.min(fill * (heat * 10) / max, 1000), 50);
    }

    /**
     * Whether another blast is already tearing through the neighbourhood. Upstream reads this off a static
     * table its own explosion entity keeps; this port's nuke entity keeps no such table, so the same
     * question is asked of the world instead — which is only ever asked on the tick a core is about to go,
     * and gives the same answer: a core inside someone else's crater melts down rather than nesting a
     * second blast inside the first.
     */
    private boolean blastSuppressed(Level level) {
        AABB nearby = box(getBlockPos(), 300);
        return !level.getEntitiesOfClass(EntityNukeExplosionMK5.class, nearby).isEmpty();
    }

    /** The whole reaction volume goes up, scaled by how much fuel was in it and how hot it had run. */
    private void detonate(Level level) {
        BlockPos pos = getBlockPos();
        int size = blastSize();

        EntityNukeExplosionMK5 explosion = EntityNukeExplosionMK5.statFacNoRad(level, size,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (level.addFreshEntity(explosion)) explosion.startChunkLoading();

        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 100_000.0F, 1.0F);

        EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(level);
        cloud.setMaxAge(size);
        cloud.snapTo(pos.getX(), pos.getY(), pos.getZ(), 0F, 0F);
        level.addFreshEntity(cloud);
    }

    /**
     * What it is like to stand near a running core: lethal if it can see you within ten blocks, and
     * unsurvivable within three. A meltdown tick widens both and poisons the chunk instead.
     */
    private void radiation(Level level) {
        double scale = meltdownTick ? 5 : 3;
        double range = meltdownTick ? 50 : 10;
        BlockPos pos = getBlockPos();
        Vec3 eye = new Vec3(pos.getX() + 0.5, pos.getY() + 6.5, pos.getZ() + 0.5);

        for (Entity e : level.getEntities(null, box(pos, range))) {
            if (e instanceof Player player && ArmorUtil.checkForHazmat(player)) continue;
            if (Raycasts.isObstructed(level, eye,
                    new Vec3(e.getX(), e.getY() + e.getEyeHeight(), e.getZ()), null)) continue;
            EntityDamageUtil.attackEntityFromIgnoreIFrame(e, ModDamageSources.ams(level), 1000F);
            Effects.ignite(e, 3);
        }

        for (Entity e : level.getEntities(null, box(pos, scale))) {
            if (e instanceof Player player && ArmorUtil.checkForHaz2(player)) continue;
            EntityDamageUtil.attackEntityFromIgnoreIFrame(e, ModDamageSources.amsCore(level), 10000F);
        }
    }

    private static AABB box(BlockPos pos, double range) {
        return new AABB(pos.getX() - range + 0.5, pos.getY() - range + 0.5, pos.getZ() - range + 0.5,
                pos.getX() + range + 0.5, pos.getY() + range + 0.5, pos.getZ() + range + 0.5);
    }

    public int getFieldScaled(int i) {
        return (field * i) / 100;
    }

    public int getHeatScaled(int i) {
        return (heat * i) / 100;
    }

    /** Whether a reaction can take place at all: loaded, cored, catalysed, and fuelled with real fuel. */
    public boolean isReady() {
        if (!lastTickValid) return false;
        if (getCore() == 0) return false;
        if (color == 0) return false;
        return getFuelEfficiency(tanks[0].getTankType()) > 0
                && getFuelEfficiency(tanks[1].getTankType()) > 0;
    }

    /**
     * Takes a beam's energy and gives back what the reaction makes of it. One thousand joules burns a
     * millibucket of each fuel and ten thousand leaves one point of heat.
     */
    public long burn(long joules) {
        if (!isReady()) return joules;

        int demand = (int) Math.ceil((double) joules / 1000D);
        if (tanks[0].getFill() < demand || tanks[1].getFill() < demand) return joules;

        this.consumption += demand;
        this.heat += (int) Math.ceil((double) joules / 10000D);

        tanks[0].setFill(tanks[0].getFill() - demand);
        tanks[1].setFill(tanks[1].getFill() - demand);

        return (long) (joules * getCore()
                * getFuelEfficiency(tanks[0].getTankType())
                * getFuelEfficiency(tanks[1].getTankType()));
    }

    public float getFuelEfficiency(FluidType type) {
        if (type == Fluids.HYDROGEN) return 1.0F;
        if (type == Fluids.DEUTERIUM) return 1.5F;
        if (type == Fluids.TRITIUM) return 1.7F;
        if (type == Fluids.OXYGEN) return 1.2F;
        if (type == Fluids.PEROXIDE) return 1.4F;
        if (type == Fluids.XENON) return 1.5F;
        if (type == Fluids.SAS3) return 2.0F;
        if (type == Fluids.BALEFIRE) return 2.5F;
        if (type == Fluids.AMAT) return 2.2F;
        if (type == Fluids.ASCHRAB) return 2.7F;
        return 0F;
    }

    public boolean hasCore() {
        return getCore() != 0;
    }

    public int getCore() {
        return ItemAMSCore.getPowerBase(inventory.getStackInSlot(1));
    }

    public int getCorePower() {
        return getCore();
    }

    private static int averageHex(int first, int second) {
        int r = (((first >> 16 & 0xFF) + (second >> 16 & 0xFF)) / 2) << 16;
        int g = (((first >> 8 & 0xFF) + (second >> 8 & 0xFF)) / 2) << 8;
        int b = ((first & 0xFF) + (second & 0xFF)) / 2;
        return r | g | b;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 1) return stack.getItem() instanceof ItemAMSCore;
        return stack.getItem() instanceof ItemCatalyst;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeInt(field);
        buf.writeInt(prevHeat);
        buf.writeInt(color);
        buf.writeBoolean(meltdownTick);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        this.field = buf.readInt();
        this.heat = buf.readInt();
        this.color = buf.readInt();
        this.meltdownTick = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "fuel1");
        tanks[1].writeToNBT(output, "fuel2");
        output.putInt("field", field);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "fuel1");
        tanks[1].readFromNBT(input, "fuel2");
        field = input.getIntOr("field", 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerCore(id, playerInventory, this);
    }
}

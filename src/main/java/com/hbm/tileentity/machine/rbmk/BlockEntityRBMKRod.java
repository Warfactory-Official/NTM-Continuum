package com.hbm.tileentity.machine.rbmk;

import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.data.Tags;

import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.rbmk.RBMKRod;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.MenuRBMKRod;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKRod;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Fuel channel tick order is burn, fuel heat, column tick, fail-safe, then stream emission. */
public class BlockEntityRBMKRod extends BlockEntityRBMKBase implements IRBMKFluxReceiver, IRBMKLoadable, MenuProvider, IRORValueProvider {
    public static final Vec3[] fluxDirs = {new Vec3(0,0,-1), new Vec3(1,0,0), new Vec3(0,0,1), new Vec3(-1,0,0)};
    public double fluxFastRatio, fluxQuantity, lastFluxQuantity, lastFluxRatio, lastFluxOut;
    public boolean hasRod;
    public int rodColor;
    public BlockEntityRBMKRod(BlockPos pos, BlockState state) { this(ModRBMKBlockEntities.RBMK_ROD.get(), pos, state); }
    protected BlockEntityRBMKRod(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state, 1); }
    @Override public boolean isModerated() { return getBlockState().getBlock() instanceof RBMKRod rod && rod.moderated; }
    @Override public int trackingRange() { return 25; }
    @Override public String getDefaultName() { return "container.rbmk_rod"; }
    @Override public void receiveFlux(NeutronStream stream) { double oldFast = fluxQuantity * fluxFastRatio; fluxQuantity += stream.fluxQuantity; fluxFastRatio = (oldFast + stream.fluxQuantity * stream.fluxRatio) / fluxQuantity; }
    @Override public void tickServer() {
        ItemStack fuel = inventory.getStackInSlot(0);
        if (fuel.getItem() instanceof ItemRBMKRod rod) {
            rodColor = rod.stats().tint();
            if (fluxQuantity > 0 && level.getGameTime() % 200 == 0)
                SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        SatelliteRayScan.RayEvent.INFO_NUCLEAR, 300);
            double outputRatio = rod.stats().neutronOut() == ItemRBMKRod.NType.SLOW ? 0D : 1D;
            double output = rod.burn(level, fuel, fluxFor(rod.stats().neutronIn()));
            rod.updateHeat(level, fuel, 1D); heat += rod.provideHeat(level, fuel, heat, 1D);
            if (!hasLid()) RadiationSystemNT.incrementRad((ServerLevel) level, worldPosition, fluxQuantity * .05D, fluxQuantity * .05D * 1024D);
            super.tickServer();
            if (heat > maxHeat()) { if (!RBMKDials.getMeltdownsDisabled(level)) meltdown(); lastFluxRatio = lastFluxQuantity = lastFluxOut = fluxQuantity = fluxFastRatio = 0D; return; }
            if (heat > 10000D) heat = 10000D;
            lastFluxQuantity = fluxQuantity; lastFluxRatio = fluxFastRatio; lastFluxOut = output; fluxQuantity = fluxFastRatio = 0D;
            spreadFlux(output, outputRatio); hasRod = true;
        } else { lastFluxRatio = lastFluxQuantity = lastFluxOut = fluxQuantity = fluxFastRatio = 0D; hasRod = false; super.tickServer(); }
    }
    private double fluxFor(ItemRBMKRod.NType type) { double fast = fluxQuantity * fluxFastRatio, slow = fluxQuantity * (1D - fluxFastRatio); return switch (type) { case SLOW -> slow + fast * .5D; case FAST -> fast + slow * .3D; case ANY -> fluxQuantity; }; }
    public void spreadFlux(double flux, double ratio) {
        if (flux == 0D) { NeutronNodeWorld.removeNode(level, worldPosition); return; }
        StreamWorld world = NeutronNodeWorld.getOrAddWorld(level); RBMKNeutronNode node = (RBMKNeutronNode) world.getNode(worldPosition);
        if (node == null) { node = RBMKNeutronHandler.makeNode(world, this); world.addNode(node); }
        for (Vec3 direction : fluxDirs) new RBMKNeutronHandler.RBMKNeutronStream(node, direction, flux, ratio);
    }
    @Override public void onMelt(int reduce) {
        boolean moderated = isModerated(), fuelled = inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod;
        boolean drx = inventory.getStackInSlot(0).is(ModRBMKItems.rbmk_fuel_drx.get()); inventory.setStackInSlot(0, ItemStack.EMPTY); if (drx) RBMKBase.digamma = true;
        if (fuelled) { int h = RBMKDials.getColumnHeight(level); reduce = Mth.clamp(reduce, 1, h); if (level.getRandom().nextInt(3) == 0) reduce++; BlockState melt = (drx ? ModRBMKBlocks.rbmk_debris_digamma : ModHazardBlocks.block_corium).get().defaultBlockState(); for (int i = h; i >= 0; i--) level.setBlock(worldPosition.above(i), i <= h + 1 - reduce ? melt : Blocks.AIR.defaultBlockState(), 3); for (int i = 0, n = 1 + level.getRandom().nextInt(h); i < n; i++) spawnDebris(EntityRBMKDebris.DebrisType.FUEL); } else standardMelt(reduce);
        if (moderated) for (int i = 0, n = 2 + level.getRandom().nextInt(2); i < n; i++) spawnDebris(EntityRBMKDebris.DebrisType.GRAPHITE);
        spawnDebris(EntityRBMKDebris.DebrisType.ELEMENT); if (RBMKBase.lidOf(getBlockState()) == RBMKBase.Lid.CONCRETE) spawnDebris(EntityRBMKDebris.DebrisType.LID);
    }
    @Override public RBMKType getRBMKType() { return RBMKType.ROD; }
    @Override public RBMKColumnType getConsoleType() { return RBMKColumnType.FUEL; }
    @Override public RBMKColumn getConsoleData() { RBMKColumn.FuelColumn data = (RBMKColumn.FuelColumn) super.getConsoleData(); ItemStack fuel = inventory.getStackInSlot(0); if (fuel.getItem() instanceof ItemRBMKRod rod) { data.enrichment = ItemRBMKRod.getEnrichment(fuel); data.xenon = ItemRBMKRod.getPoison(fuel); data.c_heat = ItemRBMKRod.getHullHeat(fuel); data.c_coreHeat = ItemRBMKRod.getCoreHeat(fuel); data.c_maxHeat = rod.stats().meltingPoint(); } return data; }
    @Override public boolean canLoad(ItemStack stack) { return !stack.isEmpty() && inventory.getStackInSlot(0).isEmpty(); }
    @Override public void load(ItemStack stack) { inventory.setStackInSlot(0, stack.copy()); markChanged(); }
    @Override public boolean canUnload() { return !inventory.getStackInSlot(0).isEmpty(); }
    @Override public ItemStack provideNext() { return inventory.getStackInSlot(0); }
    @Override public void unload() { inventory.setStackInSlot(0, ItemStack.EMPTY); markChanged(); }
    @Override public Component getDisplayName() { return Component.translatable("container.rbmkRod"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MenuRBMKRod(id, inventory, this); }
    @Override protected void loadData(DataIn input) { super.loadData(input); fluxQuantity = input.getDoubleOr("fluxQuantity", fluxQuantity); fluxFastRatio = input.getDoubleOr("fluxMod", fluxFastRatio); hasRod = input.getBooleanOr("hasRod", hasRod); }
    @Override protected void saveData(DataOut output) { super.saveData(output); output.putDouble("fluxQuantity", lastFluxQuantity); output.putDouble("fluxMod", lastFluxRatio); output.putBoolean("hasRod", hasRod); }
    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); buf.writeDouble(lastFluxQuantity); buf.writeDouble(lastFluxRatio); buf.writeBoolean(hasRod); buf.writeInt(rodColor); buf.writeDouble(lastFluxOut); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); fluxQuantity = buf.readDouble(); fluxFastRatio = buf.readDouble(); hasRod = buf.readBoolean(); rodColor = buf.readInt(); lastFluxOut = buf.readDouble(); }
    @Override public String[] getFunctionInfo() { return new String[]{PREFIX_VALUE + "columnheat", PREFIX_VALUE + "rodheat", PREFIX_VALUE + "depletion", PREFIX_VALUE + "xenon", PREFIX_VALUE + "fastflux", PREFIX_VALUE + "slowflux", PREFIX_VALUE + "flux"}; }
    @Override public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "columnheat").equals(name)) return "" + (int) heat;
        ItemStack fuel = inventory.getStackInSlot(0);
        if (fuel.getItem() instanceof ItemRBMKRod) {
            if ((PREFIX_VALUE + "rodheat").equals(name)) return "" + (int) ItemRBMKRod.getHullHeat(fuel);
            if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (100 - ItemRBMKRod.getEnrichment(fuel) * 100);
            if ((PREFIX_VALUE + "xenon").equals(name)) return "" + (int) ItemRBMKRod.getPoison(fuel);
        }
        if ((PREFIX_VALUE + "fastflux").equals(name)) return "" + (int) (lastFluxQuantity * lastFluxRatio);
        if ((PREFIX_VALUE + "slowflux").equals(name)) return "" + (int) (lastFluxQuantity * (1 - lastFluxRatio));
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + ((int) (lastFluxQuantity * lastFluxRatio) + (int) (lastFluxQuantity * (1 - lastFluxRatio)));
        return null;
    }
}

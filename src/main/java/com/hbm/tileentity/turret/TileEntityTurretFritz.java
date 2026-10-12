package com.hbm.tileentity.turret;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The heavy flamethrower. It is the only turret that burns a fluid rather than an item: its ammo slots
 * hold canisters it empties into a sixteen-bucket tank, and any flammable liquid piped in works just as
 * well. The round it sprays is picked from the tank, and its damage scales with the fuel's heat energy.
 */
public class TileEntityTurretFritz extends TileEntityTurretBaseNT implements IFluidStandardReceiverMK2 {

    private static final Identifier GUI = turretGui("fritz");
    private static final int[] FRITZ_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.DIESEL, 16000);

    public TileEntityTurretFritz(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_FRITZ.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return null;
    }

    /** 1.7 ammo display: {@code FT_Combustible && FT_Liquid} (so crude oil, flammable but not combustible, is not listed). */
    public static boolean isListedAmmo(FluidType type) {
        return type != null && FluidCombustion.getCombustible(type) != null
                && type.hasTrait(FluidTraitSimple.FT_Liquid.class);
    }

    /** 1.7 firing gate: {@code FT_Flammable && FT_Liquid} (wider than the display list; oil fires but is not listed). */
    public static boolean canFire(FluidType type) {
        return type != null && FluidCombustion.isFlammable(type) && type.hasTrait(FluidTraitSimple.FT_Liquid.class);
    }

    /** The canister first, then every combustible liquid (1.7's list, in 1.7's Fluids.getInNiceOrder()). */
    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ModAmmoItems.flame_diesel.get()));
        for (FluidType type : Fluids.getInNiceOrder()) {
            if (type == null || type == Fluids.NONE) continue;
            if (isListedAmmo(type)) {
                stacks.add(ItemFluidIcon.make(type));
            }
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    @Override
    public String getDefaultName() {
        return "container.turretFritz";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public @Nullable FluidTankNTM turretTank() {
        return tank;
    }

    /** 1.7's 48 blocks (1.12.2 cut it to 32). */
    @Override
    public double getDetectorRange() {
        return 48D;
    }

    @Override
    public double getDetectorGrace() {
        return 2D;
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getBarrelLength() {
        return 2.25D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    @Override
    public void updateFiringTick() {
        FluidType type = tank.getTankType();
        if (!canFire(type) || tank.getFill() < 2) {
            return;
        }

        tank.setFill(tank.getFill() - 2);

        Vec3 tip = barrelTip();
        float damage = Math.min(FluidCombustion.getFlammableEnergy(type) / 500_000F, 20F);
        BulletConfig conf = type == Fluids.BALEFIRE ? XFactoryFlamer.flame_nograv_bf : XFactoryFlamer.flame_nograv;

        EntityBulletBaseMK4 flame = new EntityBulletBaseMK4(level, conf, damage, 0.05F,
                worldYaw(), worldPitch());
        flame.setPos(tip.x, tip.y, tip.z);
        level.addFreshEntity(flame);

        level.playSound(null, worldPosition, ModSounds.FLAMETHROWER_SHOOT.get(), SoundSource.BLOCKS, 2F,
                1F + level.getRandom().nextFloat() * 0.5F);
    }

    @Override
    protected void tickServer() {
        super.tickServer();


        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            if (!getItem(i).is(ModAmmoItems.flame_diesel.get())) continue;
            if (tank.getTankType() != Fluids.DIESEL) continue;
            if (tank.getFill() + 1000 > tank.getMaxFill()) continue;

            tank.setFill(tank.getFill() + 1000);
            removeItem(i, 1);
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return FRITZ_SLOTS;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "diesel");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "diesel");
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }
}

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.Charger;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7 {@code TileEntityCharger}. Every tick it looks for players standing in its own column (the half-block
 * box above its floor, widened to the full cell) and adds up how much their held item and four armour pieces could
 * take this tick; that sum is its demand. Power offered to it is split over those items, a fifth of the offer (at
 * least 1 HE) per item. The clamps slide out over {@link #DELAY} ticks while anything could be charged and back in
 * when nothing can, and the charger only takes power while fully extended.
 *
 * <p>1.7 subscribed only to the conductor behind it (the cell opposite its front); the port's graph finds
 * receivers from the cable side, so {@link #canConnect} admits that one face and no other.
 *
 * <p>Odd but ported: {@link #getPower()} is always 0 and {@link #getMaxPower()} is the demand, so the network sees
 * an empty buffer exactly as large as what the players can absorb.
 */
public class TileEntityCharger extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    /** 1.7 {@code delay}: ticks for the clamps to slide out. */
    public static final int DELAY = 20;

    private List<Player> players = new ArrayList<>();
    private long charge = 0;
    private int lastOp = 0;
    boolean particles = false;

    public int usingTicks;
    public int lastUsingTicks;

    public TileEntityCharger(BlockPos pos, BlockState state) {
        super(ModEnergyBedrockBlockEntities.CHARGER.get(), pos, state);
    }

    /** 1.7 {@code ForgeDirection.getOrientation(meta).getOpposite()}: from the charger towards its mount. */
    private ForgeDirection back() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(Charger.FACING) ? state.getValue(Charger.FACING) : Direction.NORTH;
        return ForgeDirection.getOrientation(facing.getOpposite());
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == back();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCharger charger) {
        charger.players = level.getEntitiesOfClass(Player.class,
                new AABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                        .inflate(0.5, 0.0, 0.5));

        charger.charge = 0;
        for (Player player : charger.players) {
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof IBatteryItem battery) {
                    charger.charge += Math.min(battery.getMaxCharge(stack) - battery.getCharge(stack), battery.getChargeRate(stack));
                }
            }
        }

        charger.particles = charger.lastOp > 0;

        if (charger.particles) {
            charger.lastOp--;
            if (level.getGameTime() % 20 == 0) {
                // 1.7 NTMSounds.VANILLA_HISS ("random.fizz")
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
        }

        charger.networkPackNT();
        charger.animate(level, pos);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCharger charger) {
        charger.animate(level, pos);

        if (charger.particles) {
            ForgeDirection dir = charger.back();
            RandomSource rand = level.getRandom();
            level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    pos.getX() + 0.5 + rand.nextDouble() * 0.0625 + dir.offsetX * 0.75,
                    pos.getY() + 0.1,
                    pos.getZ() + 0.5 + rand.nextDouble() * 0.0625 + dir.offsetZ * 0.75,
                    -dir.offsetX + rand.nextGaussian() * 0.1,
                    0,
                    -dir.offsetZ + rand.nextGaussian() * 0.1);
        }
    }

    /**
     * The clamp animation, run on both sides as 1.7's {@code updateEntity} was. 1.7 played the piston sounds
     * through {@code worldObj.playSoundEffect}, which only reaches players from the server; so here.
     */
    private void animate(Level level, BlockPos pos) {
        lastUsingTicks = usingTicks;

        if ((charge > 0 || particles) && usingTicks < DELAY) {
            usingTicks++;
            if (usingTicks == 2 && !level.isClientSide()) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
        if ((charge <= 0 && !particles) && usingTicks > 0) {
            usingTicks--;
            if (usingTicks == 4 && !level.isClientSide()) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
    }

    /** 1.7 {@code getEquipmentInSlot(0..4)}: the held item, then boots, leggings, chestplate, helmet. */
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeLong(charge);
        buf.writeBoolean(particles);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        charge = buf.readLong();
        particles = buf.readBoolean();
    }

    @Override public long getPower() { return 0; }
    @Override public long getMaxPower() { return charge; }
    @Override public void setPower(long power) { }

    /** 1.7 {@code transferPower}: nothing until the clamps are out; then a fifth of the offer (at least 1) per item. */
    @Override
    public long transferPower(long power, boolean simulate) {
        if (usingTicks < DELAY || power == 0) return power;

        for (Player player : players) {
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof IBatteryItem battery) {
                    long toCharge = Math.min(battery.getMaxCharge(stack) - battery.getCharge(stack), battery.getChargeRate(stack));
                    toCharge = Math.min(toCharge, Math.max(power / 5, 1));
                    if (!simulate) {
                        battery.chargeBattery(stack, toCharge);
                        lastOp = 4;
                    }
                    power -= toCharge;
                }
            }
        }

        // 1.7 could hand back a negative remainder (several items each taking the minimum of 1 HE from an offer
        // smaller than their count); the graph here books "offered - returned" as delivered, so the overdraw is
        // clamped rather than minted.
        return Math.max(power, 0);
    }
}

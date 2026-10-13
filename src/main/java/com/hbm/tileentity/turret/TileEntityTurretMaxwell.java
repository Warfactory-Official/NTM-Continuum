package com.hbm.tileentity.turret;

import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.world.Effects;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The MASER. It takes no ammunition at all: its nine slots hold machine upgrades, and the only thing
 * it consumes is power. Damage, range, draw and afterburn all read off those upgrades.
 *
 * <p>The gore 1.12.2 throws when the beam finishes something off is absent: this port has no giblet
 * particles. The kill sound stays.
 */
public class TileEntityTurretMaxwell extends TileEntityTurretBaseNT implements IUpgradeInfoProvider {

    private static final Identifier GUI = turretGui("maxwell");

    private static @Nullable List<ItemStack> upgradeDisplay;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public int beam;
    public double lastDist;

    private boolean shot;

    public TileEntityTurretMaxwell(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_MAXWELL.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return null;
    }

    /** The slot panel lists what the turret actually takes, which for this one is every upgrade tier. */
    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (upgradeDisplay == null) {
            upgradeDisplay = List.of(
                    new ItemStack(ModUpgradeItems.upgrade_speed_1.get()),
                    new ItemStack(ModUpgradeItems.upgrade_speed_2.get()),
                    new ItemStack(ModUpgradeItems.upgrade_speed_3.get()),
                    new ItemStack(ModUpgradeItems.upgrade_effect_1.get()),
                    new ItemStack(ModUpgradeItems.upgrade_effect_2.get()),
                    new ItemStack(ModUpgradeItems.upgrade_effect_3.get()),
                    new ItemStack(ModUpgradeItems.upgrade_power_1.get()),
                    new ItemStack(ModUpgradeItems.upgrade_power_2.get()),
                    new ItemStack(ModUpgradeItems.upgrade_power_3.get()),
                    new ItemStack(ModUpgradeItems.upgrade_afterburn_1.get()),
                    new ItemStack(ModUpgradeItems.upgrade_afterburn_2.get()),
                    new ItemStack(ModUpgradeItems.upgrade_afterburn_3.get()),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_1.get()),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_2.get()),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_3.get()),
                    new ItemStack(ModFinalConsumableItems.upgrade_5g.get()),
                    new ItemStack(ModUpgradeItems.upgrade_screm.get()));
        }
        return upgradeDisplay;
    }

    @Override
    public String getDefaultName() {
        return "container.turretMaxwell";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public int guiFontColor() {
        return 0x0C0C0C;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 2;
    }

    @Override
    public double getDetectorGrace() {
        return 5D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 9D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 6D;
    }

    @Override
    public double getTurretElevation() {
        return 40D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDetectorRange() {
        return 64D + upgradeManager.getLevel(UpgradeType.EFFECT) * 3;
    }

    @Override
    public long getMaxPower() {
        return 10000000;
    }

    @Override
    public long getConsumption() {
        return 10000 - upgradeManager.getLevel(UpgradeType.POWER) * 300L;
    }

    @Override
    public double getBarrelLength() {
        return 2.125D;
    }

    @Override
    public double getHeightOffset() {
        return 2D;
    }

    /** 1.7 flags recounted every 20 ticks: the 5G emitter and the screaming-scientist kill sound. */
    private boolean fiveG;
    private boolean screm;
    private int checkDelay;

    @Override
    protected void tickServer() {
        upgradeManager.checkSlots(inventory, SLOT_AMMO_FIRST, SLOT_AMMO_LAST);
        if (checkDelay <= 0) {
            checkDelay = 20;
            fiveG = false;
            screm = false;
            for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.is(ModFinalConsumableItems.upgrade_5g.get())) fiveG = true;
                if (stack.is(ModUpgradeItems.upgrade_screm.get())) screm = true;
            }
        }
        checkDelay--;
        super.tickServer();
    }

    @Override
    protected void tickClient() {
        if (tPos != null) lastDist = tPos.subtract(getTurretPos()).length();
        if (beam > 0) beam--;

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        long demand = getConsumption() * 10;
        if (target == null || getPower() < demand || !(level instanceof ServerLevel server)) return;

        int overdrive = upgradeManager.getLevel(UpgradeType.OVERDRIVE);
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int afterburn = upgradeManager.getLevel(UpgradeType.AFTERBURN);

        if (fiveG && target instanceof Player player) {
            // 1.7: a player in the beam of a 5G-equipped MASER gets thirty minutes of Death instead of damage
            player.addEffect(Effects.instanceRegistered(
                    ModEffects.DEATH.get(), 30 * 60 * 20, 0, true, true));
        } else {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(target, ModDamageSources.microwave(level),
                    (overdrive * 10 + speed + 1F) * 0.25F);
        }

        if (afterburn > 0) Effects.ignite(target, afterburn * 3);

        if (!target.isAlive() && target instanceof LivingEntity living) {
            if (screm) {
                server.playSound(null, living.getX(), living.getY(), living.getZ(),
                        ModSounds.SCREM.get(), SoundSource.HOSTILE, 20.0F, 1.0F);
            } else {
                server.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
            }
        }

        setPower(getPower() - demand);

        shot = true;
        networkPackNT();
        shot = false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(shot);
        if (!shot) super.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) beam = 5;
        else super.deserialize(buf);
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.EFFECT
                || type == UpgradeType.AFTERBURN || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(
                ModTurretBlocks.turret_maxwell.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.literal("Damage +0." + (level * 25) + "/t").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(IUpgradeInfoProvider.KEY_CONSUMPTION, "-" + (level * 3) + "%")
                              .withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(IUpgradeInfoProvider.KEY_RANGE, "+" + (level * 3) + "m")
                              .withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.AFTERBURN) {
            info.add(Component.literal("Afterburn +3s").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(
                    IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 27);
        upgrades.put(UpgradeType.POWER, 27);
        upgrades.put(UpgradeType.EFFECT, 27);
        upgrades.put(UpgradeType.AFTERBURN, 27);
        upgrades.put(UpgradeType.OVERDRIVE, 27);
        return upgrades;
    }
}

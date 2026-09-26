package com.hbm.util;

import dev.golem.core.world.Compat;
import com.hbm.api.hazard.HazardClass;
import java.util.List;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import com.hbm.items.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The contamination API, ported from 1.12.2's {@code ContaminationUtil}. All radiation/digamma/lung
 * exposure funnels through {@link #contaminate}, which applies the armor/creative/immunity gates and
 * the exponential hazmat reduction ({@code 10^-resistance}).
 *
 * <p>Deferred vs the original: the mutation/stability/rad-x potion interactions (no potions yet), the
 * per-entity compat config resistances, the marshmallow gag, and the euphemium-armor blast exemption.
 */
public class ContaminationUtil {

    public static final String RAD_MULT_KEY = "hbmradmultiplier";

    /**
     * Calculates how much radiation can be applied to this entity by calculating resistance.
     */
    public static double calculateRadiationMod(LivingEntity entity) {
        if (dev.golem.core.world.Effects.hasRegistered(entity, ModEffects.MUTATION.get())) return 0D;
        double mult = 1D;
        // TODO(compat): per-entity RAD_MULT_KEY persistent-data multiplier

        double koeff = 10.0D;
        return Math.pow(koeff, -(HazmatRegistry.getResistance(entity))) * mult;
    }

    private static String fmt3(double v) {
        double ar = Math.abs(v);
        return (ar >= 1.0e6 || (ar > 0.0 && ar < 1.0e-3)) ? String.format("%.3e", v) : String.format("%.3f", v);
    }

    public static void printGeigerData(Player player) {
        double rawRadMod = calculateRadiationMod(player);
        double eRad = HbmLivingProps.getRadiation(player);
        double rads = ChunkRadiationManager.proxy.getRadiation(player.level(), player.blockPosition());
        double env = getPlayerRads(player);
        double res = (1.0 - rawRadMod) * 100.0;
        double resKoeff = HazmatRegistry.getResistance(player) * 100.0;
        double rec = env * rawRadMod;

        String resS = (Math.abs(res) >= 1.0e6 || (Math.abs(res) > 0.0 && Math.abs(res) < 1.0e-6))
                ? String.format("%.6e", res) : String.format("%.6f", res);
        String resKoeffS = (Math.abs(resKoeff) >= 1.0e6 || (Math.abs(resKoeff) > 0.0 && Math.abs(resKoeff) < 1.0e-2))
                ? String.format("%.2e", resKoeff) : String.format("%.2f", resKoeff);

        String radPrefix;
        if (eRad < 200) radPrefix = "" + ChatFormatting.GREEN;
        else if (eRad < 400) radPrefix = "" + ChatFormatting.YELLOW;
        else if (eRad < 600) radPrefix = "" + ChatFormatting.GOLD;
        else if (eRad < 800) radPrefix = "" + ChatFormatting.RED;
        else if (eRad < 1000) radPrefix = "" + ChatFormatting.DARK_RED;
        else radPrefix = "" + ChatFormatting.DARK_GRAY;
        String resPrefix = "" + ChatFormatting.WHITE + (resKoeff > 0 ? ChatFormatting.GREEN : "");

        send(player, line(ChatFormatting.GOLD, Component.literal("===== ☢ ")
                .append(Component.translatable("geiger.title")).append(Component.literal(" ☢ ====="))));
        send(player, line(ChatFormatting.YELLOW, Component.translatable("geiger.chunkRad")
                .append(Component.literal(" " + getPreffixFromRad(rads) + fmt3(rads) + " RAD/s"))));
        send(player, line(ChatFormatting.YELLOW, Component.translatable("geiger.envRad")
                .append(Component.literal(" " + getPreffixFromRad(env) + fmt3(env) + " RAD/s"))));
        send(player, line(ChatFormatting.YELLOW, Component.translatable("geiger.recievedRad")
                .append(Component.literal(" " + getPreffixFromRad(rec) + fmt3(rec) + " RAD/s"))));
        send(player, line(ChatFormatting.YELLOW, Component.translatable("geiger.playerRad")
                .append(Component.literal(" " + radPrefix + fmt3(eRad) + " RAD"))));
        send(player, line(ChatFormatting.YELLOW, Component.translatable("geiger.playerRes")
                .append(Component.literal(" " + resPrefix + resS + "% (" + resKoeffS + ")"))));
    }

    public static void printDosimeterData(Player player) {
        double rads = getActualPlayerRads(player);
        boolean limit = false;

        if (rads > 3.6D) {
            rads = 3.6D;
            limit = true;
        }
        rads = ((int) (1000D * rads)) / 1000D;

        send(player, line(ChatFormatting.GOLD, Component.literal("===== ☢ ")
                .append(Component.translatable("dosimeter.title")).append(Component.literal(" ☢ ====="))));
        send(player, line(ChatFormatting.YELLOW, Component.translatable("geiger.recievedRad")
                .append(Component.literal(" " + getPreffixFromRad(rads) + (limit ? ">" : "") + rads + " RAD/s"))));
    }

    public static void printDiagnosticData(Player player) {
        double digamma = ((int) (HbmLivingProps.getDigamma(player) * 1000)) / 1000D;
        double halflife = ((int) ((1D - Math.pow(0.5, digamma)) * 10000)) / 100D;

        send(player, line(ChatFormatting.DARK_PURPLE, Component.literal("===== Ϝ ")
                .append(Component.translatable("digamma.title")).append(Component.literal(" Ϝ ====="))));
        send(player, line(ChatFormatting.LIGHT_PURPLE, Component.translatable("digamma.playerDigamma")
                .append(Component.literal(ChatFormatting.RED + " " + digamma + " DRX"))));
        send(player, line(ChatFormatting.LIGHT_PURPLE, Component.translatable("digamma.playerHealth")
                .append(Component.literal(getTextColorFromPercent(halflife / 100D) + String.format(" %6.2f", halflife) + "%"))));
    }

    public static void printLungDiagnosticData(Player player) {
        float playerAsbestos = 100F - ((int) (10000F * HbmLivingProps.getAsbestos(player) / EntityHbmProps.MAX_ASBESTOS)) / 100F;
        float playerBlacklung = 100F - ((int) (10000F * HbmLivingProps.getBlackLung(player) / EntityHbmProps.MAX_BLACKLUNG)) / 100F;
        float playerTotal = (playerAsbestos * playerBlacklung / 100F);

        send(player, line(ChatFormatting.WHITE, Component.literal("===== L ")
                .append(Component.translatable("lung_scanner.title")).append(Component.literal(" L ====="))));
        send(player, line(ChatFormatting.WHITE, Component.translatable("lung_scanner.player_asbestos_health")
                .append(Component.literal(String.format(getTextColorLung(playerAsbestos / 100D) + " %6.2f", playerAsbestos) + " %"))));
        send(player, line(ChatFormatting.DARK_GRAY, Component.translatable("lung_scanner.player_coal_health")
                .append(Component.literal(String.format(getTextColorLung(playerBlacklung / 100D) + " %6.2f", playerBlacklung) + " %"))));
        send(player, line(ChatFormatting.GRAY, Component.translatable("lung_scanner.player_total_health")
                .append(Component.literal(String.format(getTextColorLung(playerTotal / 100D) + " %6.2f", playerTotal) + " %"))));
    }

    private static MutableComponent line(ChatFormatting color, MutableComponent body) {
        return body.withStyle(color);
    }

    private static void send(Player player, Component component) {
        player.sendSystemMessage(component);
    }

    public static String getTextColorFromPercent(double percent) {
        if (percent < 0.5) return "" + ChatFormatting.GREEN;
        else if (percent < 0.6) return "" + ChatFormatting.YELLOW;
        else if (percent < 0.7) return "" + ChatFormatting.GOLD;
        else if (percent < 0.8) return "" + ChatFormatting.RED;
        else if (percent < 0.9) return "" + ChatFormatting.DARK_RED;
        else return "" + ChatFormatting.DARK_GRAY;
    }

    public static String getTextColorLung(double percent) {
        if (percent > 0.9) return "" + ChatFormatting.GREEN;
        else if (percent > 0.75) return "" + ChatFormatting.YELLOW;
        else if (percent > 0.5) return "" + ChatFormatting.GOLD;
        else if (percent > 0.25) return "" + ChatFormatting.RED;
        else if (percent > 0.1) return "" + ChatFormatting.DARK_RED;
        else return "" + ChatFormatting.DARK_GRAY;
    }

    public static String getPreffixFromRad(double rads) {
        if (rads == 0) return "" + ChatFormatting.GREEN;
        else if (rads < 1) return "" + ChatFormatting.YELLOW;
        else if (rads < 10) return "" + ChatFormatting.GOLD;
        else if (rads < 100) return "" + ChatFormatting.RED;
        else if (rads < 1000) return "" + ChatFormatting.DARK_RED;
        else return "" + ChatFormatting.DARK_GRAY;
    }

    public static double getActualPlayerRads(LivingEntity entity) {
        return getPlayerRads(entity) * calculateRadiationMod(entity);
    }

    public static double getPlayerRads(LivingEntity entity) {
        double rads = HbmLivingProps.getRadBuf(entity);
        if (entity instanceof Player)
            rads = rads + HbmLivingProps.getNeutron(entity) * 20;
        return rads;
    }

    public static double getNoNeutronPlayerRads(LivingEntity entity) {
        return HbmLivingProps.getRadBuf(entity) * calculateRadiationMod(entity);
    }

    public static boolean isRadItem(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return false;
        return HazardSystem.getRawRadsFromStack(stack) > 0;
    }

    /** Neutron activation reads — data-component based; system is config-off, kept minimal. */
    public static float getNeutronRads(ItemStack stack) {
        if (stack.isEmpty() || isRadItem(stack)) return 0F;
        return ModDataComponents.NEUTRON_ACTIVATION.getOr(stack, 0F) * stack.getCount();
    }

    /** Whether an otherwise non-radioactive stack carries the old neutron-activation contamination. */
    public static boolean isContaminated(ItemStack stack) {
        return !stack.isEmpty() && ModDataComponents.NEUTRON_ACTIVATION.has(stack);
    }

    /** The old drum-side neutron activation update, expressed with the modern item component. */
    public static boolean neutronActivateItem(ItemStack stack, float rad, float decay) {
        if (stack.isEmpty() || stack.getCount() != 1 || isRadItem(stack)) return false;
        float previous = ModDataComponents.NEUTRON_ACTIVATION.getOr(stack, 0F);
        float next = previous * decay + rad / stack.getCount();
        if (next < 0.0001F) {
            if (previous > 0F) {
                ModDataComponents.NEUTRON_ACTIVATION.remove(stack);
                return true;
            }
        } else if (Math.abs(next - previous) > 1.0e-6F) {
            ModDataComponents.NEUTRON_ACTIVATION.set(stack, next);
            return true;
        }
        return false;
    }

    /** Adds the old neutron-activation radioactive tooltip lines to the shared item tooltip path. */
    public static void addNeutronRadInfo(ItemStack stack, List<Component> list) {
        float activation = getNeutronRads(stack);
        if (activation <= 0F) return;

        double perItem = activation / stack.getCount();
        list.add(Component.literal("[").append(Component.translatable("trait.radioactive"))
                .append(Component.literal("]")).withStyle(ChatFormatting.GREEN));
        list.add(Component.literal(" " + Math.round(HazardTypeRadiation.getNewValue(perItem) * 1000D) / 1000D)
                .append(neutronSuffix(perItem)).append(Component.literal(" "))
                .append(Component.translatable("desc.rads")).withStyle(ChatFormatting.YELLOW));
        if (stack.getCount() > 1) {
            list.add(Component.literal(" ").append(Component.translatable("desc.stack"))
                    .append(Component.literal(" " + Math.round(HazardTypeRadiation.getNewValue(activation) * 1000D) / 1000D))
                    .append(neutronSuffix(activation)).append(Component.literal(" "))
                    .append(Component.translatable("desc.rads")).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static Component neutronSuffix(double radiation) {
        if (radiation < 1_000_000D) return Component.empty();
        return Component.translatable(radiation < 1_000_000_000D ? "desc.mil" : "desc.bil");
    }

    public static double getRads(Entity e) {
        if (e instanceof IRadiationImmune)
            return 0.0D;
        if (e instanceof LivingEntity living)
            return HbmLivingProps.getRadiation(living);
        return 0.0D;
    }

    public static boolean isRadImmune(Entity e) {
        if (e instanceof LivingEntity living &&
                dev.golem.core.world.Effects.hasRegistered(living, ModEffects.MUTATION.get())) return true;
        // Old list minus un-ported HBM mobs (nuclear creeper, quackos)
        return e instanceof MushroomCow
                || e instanceof Zombie
                || e instanceof AbstractSkeleton
                || e instanceof Ocelot
                || e instanceof Cat
                || e instanceof ZombieHorse
                || e instanceof SkeletonHorse
                || e instanceof ArmorStand
                || e instanceof IRadiationImmune;
    }

    /// ASBESTOS ///

    public static void applyAsbestos(Entity e, int i, int dmg) {
        applyAsbestos(e, i, dmg, 1);
    }

    public static void applyAsbestos(Entity e, int i, int dmg, int chance) {

        if (!(e instanceof LivingEntity entity))
            return;

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        if (e instanceof Player && e.tickCount < 200)
            return;

        if (ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) {
            if (chance > 1) {
                if (entity.getRandom().nextInt(chance) == 0) {
                    ArmorUtil.damageGasMaskFilter(entity, 1);
                }
            } else {
                ArmorUtil.damageGasMaskFilter(entity, dmg);
            }
        } else {
            HbmLivingProps.incrementAsbestos(entity, i);
        }
    }

    /// COAL ///
    public static void applyCoal(Entity e, int i, int dmg, int chance) {

        if (!(e instanceof LivingEntity entity))
            return;

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        if (e instanceof Player && e.tickCount < 200)
            return;

        if (ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_COARSE)) {
            if (chance > 1) {
                if (entity.getRandom().nextInt(chance) == 0) {
                    ArmorUtil.damageGasMaskFilter(entity, 1);
                }
            } else {
                ArmorUtil.damageGasMaskFilter(entity, dmg);
            }
        } else {
            HbmLivingProps.incrementBlackLung(entity, i);
        }
    }

    /// DIGAMMA ///
    public static void applyDigammaData(Entity e, double f) {

        if (!(e instanceof LivingEntity entity))
            return;

        if (e instanceof Ocelot || e instanceof Cat)
            return;

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        if (e instanceof Player && e.tickCount < 200)
            return;

        if (dev.golem.core.world.Effects.hasRegistered(entity, ModEffects.STABILITY.get())) return;

        if (!(entity instanceof Player player && ArmorUtil.checkForDigamma(player)))
            HbmLivingProps.incrementDigamma(entity, f);
    }

    public static double getDigamma(Entity e) {
        if (!(e instanceof LivingEntity entity))
            return 0.0D;
        return HbmLivingProps.getDigamma(entity);
    }

    public static void radiate(Level level, double x, double y, double z, double range, float rad3d) {
        radiate(level, x, y, z, range, rad3d, 0, 0, 0, 0);
    }

    public static void radiate(Level level, double x, double y, double z, double range, float rad3d, float dig3d, float fire3d) {
        radiate(level, x, y, z, range, rad3d, dig3d, fire3d, 0, 0);
    }

    public static void radiate(Level level, double x, double y, double z, double range, float rad3d, float dig3d, float fire3d, float blast3d) {
        radiate(level, x, y, z, range, rad3d, dig3d, fire3d, blast3d, range);
    }

    /**
     * Area-of-effect contamination (used by explosions): distance-falloff radiation/digamma plus
     * optional fire and blast damage, attenuated by the summed explosion resistance of blocks along
     * the ray to each entity.
     */
    public static void radiate(Level level, double x, double y, double z, double range, float rad3d, float dig3d, float fire3d, float blast3d, double blastRange) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(x - range, y - range, z - range, x + range, y + range, z + range));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Entity e : entities) {
            if (isExplosionExempt(e)) continue;

            Vec3 vec = new Vec3(e.getX() - x, (e.getY() + e.getEyeHeight()) - y, e.getZ() - z);
            double len = vec.length();

            if (len > range) continue;
            vec = vec.normalize();
            double dmgLen = Math.max(len, range * 0.05D);

            float res = 0;

            for (int i = 1; i < len; i++) {
                int ix = (int) Math.floor(x + vec.x * i);
                int iy = (int) Math.floor(y + vec.y * i);
                int iz = (int) Math.floor(z + vec.z * i);
                res += level.getBlockState(pos.set(ix, iy, iz)).getBlock().getExplosionResistance();
            }
            boolean isLiving = e instanceof LivingEntity;

            if (res < 1)
                res = 1;
            if (isLiving && rad3d > 0) {
                float eRads = rad3d;
                eRads /= (float) (dmgLen * dmgLen * Math.sqrt(res));
                contaminate((LivingEntity) e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);
            }
            if (isLiving && dig3d > 0) {
                float eDig = dig3d;
                eDig /= (float) (dmgLen * dmgLen * dmgLen);
                contaminate((LivingEntity) e, HazardType.DIGAMMA, ContaminationType.DIGAMMA, eDig);
            }

            if (fire3d > 0.025 && level instanceof ServerLevel serverLevel) {
                float fireDmg = fire3d;
                fireDmg /= (float) (dmgLen * dmgLen * res * res);
                if (fireDmg > 0.025) {
                    Compat.hurt(e, serverLevel, serverLevel.damageSources().inFire(), fireDmg);
                    dev.golem.core.world.Effects.ignite(e, 5);
                }
            }

            if (len < blastRange && blast3d > 0.025 && level instanceof ServerLevel serverLevel) {
                float blastDmg = blast3d;
                blastDmg /= (float) (dmgLen * dmgLen * res);
                if (blastDmg > 0.025) {
                    // TODO(damage): dedicated nuclearBlast/blast damage types
                    Compat.hurt(e, serverLevel, serverLevel.damageSources().explosion(null, null), blastDmg);
                }
                e.push(vec.x * 0.005D * blastDmg, vec.y * 0.005D * blastDmg, vec.z * 0.005D * blastDmg);
            }
        }
    }

    private static boolean isExplosionExempt(Entity e) {
        if (e instanceof Ocelot || e instanceof Cat || e instanceof EntityNukeExplosionMK5) {
            return true;
        }
        // TODO(armor): euphemium full-set exemption; TODO(entities): torex/mirv/mini-nuke/bullet exemptions
        return e instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public enum HazardType {
        MONOXIDE,
        RADIATION,
        NEUTRON,
        DIGAMMA
    }

    public enum ContaminationType {
        FARADAY,     // preventable by metal armor
        HAZMAT,      // preventable by hazmat
        HAZMAT2,     // preventable by heavy hazmat
        DIGAMMA,     // preventable by fau armor or stability
        DIGAMMA2,    // preventable by robes
        CREATIVE,    // preventable by creative mode, for rad calculation armor piece bonuses still apply
        RAD_BYPASS,  // same as creative but will not apply radiation resistance calculation
        NONE         // not preventable
    }

    public static boolean contaminate(LivingEntity entity, HazardType hazard, ContaminationType cont, double amount) {

        if (hazard == HazardType.RADIATION) {
            double radEnv = HbmLivingProps.getRadEnv(entity);
            HbmLivingProps.setRadEnv(entity, radEnv + amount);
        }

        if (entity instanceof Player player) {
            if (player.isSpectator()) return false;
            switch (cont) {
                case FARADAY: if (ArmorUtil.checkForFaraday(player)) return false; break;
                case HAZMAT: if (ArmorUtil.checkForHazmat(player)) return false; break;
                case HAZMAT2: if (ArmorUtil.checkForHaz2(player)) return false; break;
                case DIGAMMA: if (ArmorUtil.checkForDigamma(player)) return false; break;
                default: break;
            }

            if (player.isCreative() && cont != ContaminationType.NONE) {
                if (hazard == HazardType.NEUTRON)
                    HbmLivingProps.setNeutron(entity, amount);
                return false;
            }

            if (player.tickCount < 200)
                return false;
        }

        if ((hazard == HazardType.RADIATION || hazard == HazardType.NEUTRON) && isRadImmune(entity)) {
            return false;
        }

        if (hazard == HazardType.RADIATION || hazard == HazardType.NEUTRON) {
            amount = com.hbm.scripting.NTMServerEvents.postRadiation(entity, amount, hazard == HazardType.NEUTRON);
            if (Double.isNaN(amount)) return false;
        }

        switch (hazard) {
            case MONOXIDE -> {
                if (entity.level() instanceof ServerLevel serverLevel)
                    Compat.hurt(entity, serverLevel, ModDamageSources.monoxide(serverLevel), (float) amount);
            }
            case RADIATION ->
                    HbmLivingProps.incrementRadiation(entity, amount * (cont == ContaminationType.RAD_BYPASS ? 1D : calculateRadiationMod(entity)));
            case NEUTRON -> {
                HbmLivingProps.incrementRadiation(entity, amount * (cont == ContaminationType.RAD_BYPASS ? 1D : calculateRadiationMod(entity)));
                HbmLivingProps.setNeutron(entity, amount);
            }
            case DIGAMMA -> applyDigammaData(entity, amount);
        }

        return true;
    }
}

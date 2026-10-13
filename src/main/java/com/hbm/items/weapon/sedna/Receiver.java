package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A gun's moving parts: barrel, chamber and magazine. Base damage, spread, fire rate, reload timings
 * and the fire sound all live here, so an underbarrel grenade launcher is a second receiver on
 * the same {@link GunConfig}.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.Receiver}. Upstream routes every getter
 * through {@code XWeaponModManager.eval(field, stack, KEY, this, index)} so weapon mods can override
 * any stat per stack; every getter here does the same, so installed weapon mods apply (verified by the
 * {@code fix5_weapon_mods_and_names} GameTest).
 */
public class Receiver {

    public static final String F_BASEDAMAGE = "F_BASEDAMAGE";
    public static final String I_DELAYAFTERFIRE = "I_DELAYAFTERFIRE";
    public static final String I_DELAYAFTERDRYFIRE = "I_DELAYAFTERDRYFIRE";
    public static final String I_ROUNDSPERCYCLE = "I_ROUNDSPERCYCLE";
    public static final String F_SPLITPROJECTILES = "F_SPLITPROJECTILES";
    public static final String F_SPREADINNATE = "F_SPREADINNATE";
    public static final String F_SPREADAMMO = "F_SPREADAMMO";
    public static final String F_SPREADHIPFIRE = "F_SPREADHIPFIRE";
    public static final String F_SPREADDURABILITY = "F_SPREADDURABILITY";
    public static final String B_REFIREONHOLD = "B_REFIREONHOLD";
    public static final String B_REFIREAFTERDRY = "B_REFIREAFTERDRY";
    public static final String B_DOESDRYFIRE = "B_DOESDRYFIRE";
    public static final String B_DOESDRYFIREAFTERAUTO = "B_DOESDRYFIREAFTERAUTO";
    public static final String B_EJECTONFIRE = "B_EJECTONFIRE";
    public static final String B_RELOADONEMPTY = "B_RELOADONEMPTY";
    public static final String I_RELOADBEGINDURATION = "I_RELOADBEGINDURATION";
    public static final String I_RELOADCYCLEDURATION = "I_RELOADCYCLEDURATION";
    public static final String I_RELOADENDDURATION = "I_RELOADENDDURATION";
    public static final String I_RELOADCOCKONEMPTYPRE = "I_RELOADCOCKONEMPTYPRE";
    public static final String I_RELOADCOCKONEMPTYPOST = "I_RELOADCOCKONEMPTYPOST";
    public static final String I_JAMDURATION = "I_JAMDURATION";
    public static final String S_FIRESOUND = "S_FIRESOUND";
    public static final String F_FIREVOLUME = "F_FIREVOLUME";
    public static final String F_FIREPITCH = "F_FIREPITCH";
    public static final String O_MAGAZINE = "O_MAGAZINE";
    public static final String O_PROJECTILEOFFSET = "O_PROJECTILEOFFSET";
    public static final String O_PROJECTILEOFFSETSCOPED = "O_PROJECTILEOFFSETSCOPED";
    public static final String FUN_CANFIRE = "FUN_CANFIRE";
    public static final String CON_ONFIRE = "CON_ONFIRE";
    public static final String CON_ONRECOIL = "CON_ONRECOIL";

    public Receiver(int index) {
        this.index = index;
    }

    protected int index;
    protected GunConfig parent;
    protected float baseDamage_DNA;
    protected int delayAfterFire_DNA;
    protected int delayAfterDryFire_DNA;
    protected int roundsPerCycle_DNA = 1;
    protected float splitProjectiles_DNA = 1;
    protected float spreadInnate_DNA = 0F;
    protected float spreadMultAmmo_DNA = 1F;
    protected float spreadPenaltyHipfire_DNA = 0.025F;
    protected float spreadDurability_DNA = 0.125F;
    protected boolean refireOnHold_DNA = false;
    protected boolean refireAfterDry_DNA = false;
    protected boolean doesDryFire_DNA = true;
    protected boolean doesDryFireAfterAuto_DNA = false;
    protected boolean ejectOnFire_DNA = true;
    protected boolean reloadOnEmpty_DNA = false;
    protected int reloadBeginDuration_DNA;
    protected int reloadCycleDuration_DNA;
    protected int reloadEndDuration_DNA;
    protected int reloadCockOnEmptyPre_DNA;
    protected int reloadCockOnEmptyPost_DNA;
    protected int jamDuration_DNA = 0;
    /**
     * The fire sound, held as a supplier.
     *
     * <p>Lazy for the same reason the ammo item is: receivers are built while the item registry is being
     * flushed, and the sound registry may not have been flushed yet. NeoForge survives an eager lookup
     * because its {@code RegisterEvent} fires in vanilla registry order (sounds before items); Fabric
     * flushes in declaration order and fails with "Missing key … hbm:weapon.fire.pistol".
     */
    protected Supplier<SoundEvent> fireSound_DNA;
    protected float fireVolume_DNA = 1.0F;
    protected float firePitch_DNA = 1.0F;
    /** Raw magazine currency is heterogeneous: bullets, fluids, and energy all share the receiver slot. */
    protected IMagazine<?> magazine_DNA;
    protected Vec3 projectileOffset_DNA = Vec3.ZERO;
    protected Vec3 projectileOffsetScoped_DNA = Vec3.ZERO;
    protected BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> canFire_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onFire_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onRecoil_DNA;

    public float getBaseDamage(ItemStack s) { return XWeaponModManager.eval(baseDamage_DNA, s, F_BASEDAMAGE, this, parent.index); }
    public int getDelayAfterFire(ItemStack s) { return XWeaponModManager.eval(delayAfterFire_DNA, s, I_DELAYAFTERFIRE, this, parent.index); }
    public int getDelayAfterDryFire(ItemStack s) { return XWeaponModManager.eval(delayAfterDryFire_DNA, s, I_DELAYAFTERDRYFIRE, this, parent.index); }
    public int getRoundsPerCycle(ItemStack s) { return XWeaponModManager.eval(roundsPerCycle_DNA, s, I_ROUNDSPERCYCLE, this, parent.index); }
    public float getSplitProjectiles(ItemStack s) { return XWeaponModManager.eval(splitProjectiles_DNA, s, F_SPLITPROJECTILES, this, parent.index); }
    public float getInnateSpread(ItemStack s) { return XWeaponModManager.eval(spreadInnate_DNA, s, F_SPREADINNATE, this, parent.index); }
    public float getAmmoSpread(ItemStack s) { return XWeaponModManager.eval(spreadMultAmmo_DNA, s, F_SPREADAMMO, this, parent.index); }
    public float getHipfireSpread(ItemStack s) { return XWeaponModManager.eval(spreadPenaltyHipfire_DNA, s, F_SPREADHIPFIRE, this, parent.index); }
    public float getDurabilitySpread(ItemStack s) { return XWeaponModManager.eval(spreadDurability_DNA, s, F_SPREADDURABILITY, this, parent.index); }
    public boolean getRefireOnHold(ItemStack s) { return XWeaponModManager.eval(refireOnHold_DNA, s, B_REFIREONHOLD, this, parent.index); }
    public boolean getRefireAfterDry(ItemStack s) { return XWeaponModManager.eval(refireAfterDry_DNA, s, B_REFIREAFTERDRY, this, parent.index); }
    public boolean getDoesDryFire(ItemStack s) { return XWeaponModManager.eval(doesDryFire_DNA, s, B_DOESDRYFIRE, this, parent.index); }
    public boolean getDoesDryFireAfterAuto(ItemStack s) { return XWeaponModManager.eval(doesDryFireAfterAuto_DNA, s, B_DOESDRYFIREAFTERAUTO, this, parent.index); }
    public boolean getEjectOnFire(ItemStack s) { return XWeaponModManager.eval(ejectOnFire_DNA, s, B_EJECTONFIRE, this, parent.index); }
    public boolean getReloadOnEmpty(ItemStack s) { return XWeaponModManager.eval(reloadOnEmpty_DNA, s, B_RELOADONEMPTY, this, parent.index); }
    public int getReloadBeginDuration(ItemStack s) { return XWeaponModManager.eval(reloadBeginDuration_DNA, s, I_RELOADBEGINDURATION, this, parent.index); }
    public int getReloadCycleDuration(ItemStack s) { return XWeaponModManager.eval(reloadCycleDuration_DNA, s, I_RELOADCYCLEDURATION, this, parent.index); }
    public int getReloadEndDuration(ItemStack s) { return XWeaponModManager.eval(reloadEndDuration_DNA, s, I_RELOADENDDURATION, this, parent.index); }
    public int getReloadCockOnEmptyPre(ItemStack s) { return XWeaponModManager.eval(reloadCockOnEmptyPre_DNA, s, I_RELOADCOCKONEMPTYPRE, this, parent.index); }
    public int getReloadCockOnEmptyPost(ItemStack s) { return XWeaponModManager.eval(reloadCockOnEmptyPost_DNA, s, I_RELOADCOCKONEMPTYPOST, this, parent.index); }
    public int getJamDuration(ItemStack s) { return XWeaponModManager.eval(jamDuration_DNA, s, I_JAMDURATION, this, parent.index); }
    public SoundEvent getFireSound(ItemStack s) { return XWeaponModManager.eval(fireSound_DNA == null ? null : fireSound_DNA.get(), s, S_FIRESOUND, this, parent.index); }
    public float getFireVolume(ItemStack s) { return XWeaponModManager.eval(fireVolume_DNA, s, F_FIREVOLUME, this, parent.index); }
    public float getFirePitch(ItemStack s) { return XWeaponModManager.eval(firePitch_DNA, s, F_FIREPITCH, this, parent.index); }
    @SuppressWarnings("unchecked")
    public IMagazine<BulletConfig> getMagazine(ItemStack s) {
        return (IMagazine<BulletConfig>) XWeaponModManager.eval(magazine_DNA, s, O_MAGAZINE, this, parent.index);
    }
    public Vec3 getProjectileOffset(ItemStack s) { return XWeaponModManager.eval(projectileOffset_DNA, s, O_PROJECTILEOFFSET, this, parent.index); }
    public Vec3 getProjectileOffsetScoped(ItemStack s) { return XWeaponModManager.eval(projectileOffsetScoped_DNA, s, O_PROJECTILEOFFSETSCOPED, this, parent.index); }
    public BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> getCanFire(ItemStack s) { return XWeaponModManager.eval(canFire_DNA, s, FUN_CANFIRE, this, parent.index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getOnFire(ItemStack s) { return XWeaponModManager.eval(onFire_DNA, s, CON_ONFIRE, this, parent.index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getRecoil(ItemStack s) { return XWeaponModManager.eval(onRecoil_DNA, s, CON_ONRECOIL, this, parent.index); }

    public Receiver dmg(float dmg) { this.baseDamage_DNA = dmg; return this; }
    public Receiver delay(int delay) { this.delayAfterFire_DNA = this.delayAfterDryFire_DNA = delay; return this; }
    public Receiver dry(int delay) { this.delayAfterDryFire_DNA = delay; return this; }
    public Receiver rounds(int rounds) { this.roundsPerCycle_DNA = rounds; return this; }
    public Receiver split(float rounds) { this.splitProjectiles_DNA = rounds; return this; }
    public Receiver spread(float spread) { this.spreadInnate_DNA = spread; return this; }
    public Receiver spreadAmmo(float spread) { this.spreadMultAmmo_DNA = spread; return this; }
    public Receiver spreadHipfire(float spread) { this.spreadPenaltyHipfire_DNA = spread; return this; }
    public Receiver spreadDurability(float spread) { this.spreadDurability_DNA = spread; return this; }
    public Receiver auto(boolean auto) { this.refireOnHold_DNA = auto; return this; }
    public Receiver autoAfterDry(boolean auto) { this.refireAfterDry_DNA = auto; return this; }
    public Receiver dryfire(boolean dryfire) { this.doesDryFire_DNA = dryfire; return this; }
    public Receiver dryfireAfterAuto(boolean dryfire) { this.doesDryFireAfterAuto_DNA = dryfire; return this; }
    public Receiver ejectOnFire(boolean eject) { this.ejectOnFire_DNA = eject; return this; }
    public Receiver reloadOnEmpty(boolean reload) { this.reloadOnEmpty_DNA = reload; return this; }
    public Receiver mag(IMagazine<?> magazine) { this.magazine_DNA = magazine; return this; }
    public Receiver jam(int jam) { this.jamDuration_DNA = jam; return this; }

    /**
     * The muzzle position relative to the shooter, as (forward, up, side). Setting the hipfire offset
     * also sets the scoped one to the same forward/up with no sideways displacement, down the sights,
     * the barrel is centred.
     */
    public Receiver offset(double f, double u, double s) {
        this.projectileOffset_DNA = new Vec3(f, u, s);
        this.projectileOffsetScoped_DNA = new Vec3(f, u, 0);
        return this;
    }

    public Receiver offsetScoped(double f, double u, double s) {
        this.projectileOffsetScoped_DNA = new Vec3(f, u, s);
        return this;
    }

    public Receiver reload(int delay) { return reload(0, delay, delay, 0, 0); }

    public Receiver reload(int begin, int cycle, int end, int cock) { return reload(0, begin, cycle, end, cock); }

    public Receiver reload(int pre, int begin, int cycle, int end, int post) {
        this.reloadBeginDuration_DNA = begin;
        this.reloadCycleDuration_DNA = cycle;
        this.reloadEndDuration_DNA = end;
        this.reloadCockOnEmptyPre_DNA = pre;
        this.reloadCockOnEmptyPost_DNA = post;
        return this;
    }

    public Receiver canFire(BiFunction<ItemStack, ItemGunBaseNT.LambdaContext, Boolean> lambda) { this.canFire_DNA = lambda; return this; }
    public Receiver fire(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onFire_DNA = lambda; return this; }
    public Receiver recoil(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onRecoil_DNA = lambda; return this; }

    public Receiver sound(Supplier<SoundEvent> sound, float volume, float pitch) {
        this.fireSound_DNA = sound;
        this.fireVolume_DNA = volume;
        this.firePitch_DNA = pitch;
        return this;
    }

    /** Fires when the magazine has a round, and spawns a standard bullet. */
    public Receiver setupStandardFire() {
        return this.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE);
    }

    /** Standard fire gated by the receiver's lock-on flag. */
    public Receiver setupLockonFire() {
        return this.canFire(Lego.LAMBDA_LOCKON_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE);
    }
}

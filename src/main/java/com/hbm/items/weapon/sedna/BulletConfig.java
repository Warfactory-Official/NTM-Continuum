package com.hbm.items.weapon.sedna;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModCasingItems;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.particle.SpentCasing;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

/**
 * Everything about one <em>round of ammunition</em>: how fast it flies, how it spreads, what it does on
 * impact, and which item feeds it into a magazine. The gun contributes base damage (on its
 * {@link Receiver}) and the round multiplies it — so the same receiver firing AP instead of FMJ is the
 * whole difference between those two rounds.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.BulletConfig}. Two deliberate changes:
 *
 * <ul>
 *   <li><b>Identity.</b> Upstream keeps a mutable {@code static List<BulletConfig> configs} and stores
 *       the <i>list index</i> in both the gun's NBT and the bullet entity's synched data — the source
 *       itself carries a {@code //TODO: replace with named tags to avoid ID shifting}. Here every config
 *       is registered under an {@link Identifier} in {@link BulletConfigs}, so save data and packets
 *       survive any change in declaration order.
 *   <li><b>Ammo matching.</b> Upstream's {@code ComparableStack(ammo_standard, 1, meta)} relies on item
 *       metadata, which no longer exists; each ammo variant is its own item, so the binding is a plain
 *       {@link net.minecraft.world.item.Item}.
 * </ul>
 *
 * <p>Casing items and casing-bag accumulation are supported through the shared item path. Per-ammo
 * tracer renderers remain client-owned; projectile types are dispatched by {@code Lego} when their
 * corresponding entity is registered.
 */
public class BulletConfig {

    /** The standard "bounce off shallow angles, break glass, otherwise stop" block-impact behaviour. */
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_RICOCHET =
            BulletImpact::standardRicochet;

    /** The standard "hurt it, maybe headshot, maybe keep going" entity-impact behaviour. */
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_ENTITY_HIT =
            BulletImpact::standardEntityHit;

    /** This config's registry id. Assigned by {@link BulletConfigs#register}. */
    public Identifier id;

    /**
     * The item one round is loaded from, held as a supplier.
     *
     * <p>Lazy on purpose: bullet configs are declared during mod bootstrap so that a gun's magazine can
     * name them while the gun item is being built, but items themselves are not registered until later.
     * Resolving the item eagerly here throws "Missing key in ResourceKey[minecraft:item …]" before the
     * game finishes loading.
     */
    public java.util.function.Supplier<net.minecraft.world.item.Item> ammo;
    /** Spent casing item and the number of shots represented by one casing drop. */
    public ItemStack casingItem;
    private java.util.function.Supplier<ItemStack> casingSupplier;
    public int casingAmount;
    /** Original-hbm's shell casing renderer data; retained even while its world entity is deferred. */
    public CasingProfile casingProfile;
    /** The source casing definition selected by this ammunition config. */
    public SpentCasing casing;

    /** How much ammo is added to a magazine per consumed item. */
    public int ammoReloadCount = 1;
    public float velocity = 10F;
    public float spread = 0F;
    public float wear = 1F;
    public int projectilesMin = 1;
    public int projectilesMax = 1;
    public ProjectileType pType = ProjectileType.BULLET;
    public float damageMult = 1.0F;
    /**
     * Damage-threshold negation, passed to {@code DamageResistanceHandler} with each impact.
     */
    public float armorThresholdNegation = 0.0F;
    /** Fraction of this round's damage that ignores armour entirely. */
    public float armorPiercingPercent = 0.0F;
    public float knockbackMult = 0.1F;
    public float headshotMult = 1.25F;
    public DamageClass dmgClass = DamageClass.PHYSICAL;
    public float ricochetAngle = 5F;
    public int maxRicochetCount = 2;
    /** Whether damage dealt to an entity is subtracted from the projectile's remaining damage. */
    public boolean damageFalloffByPen = true;
    public Consumer<Entity> onUpdate;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onImpact;
    public BiConsumer<EntityBulletBeamBase, HitResult> onImpactBeam;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onRicochet = LAMBDA_STANDARD_RICOCHET;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onEntityHit = LAMBDA_STANDARD_ENTITY_HIT;
    public float gravity = 0;
    public int expires = 30;
    public boolean impactsEntities = true;
    public boolean doesPenetrate = false;
    /** Whether projectiles ignore blocks entirely. */
    public boolean isSpectral = false;
    public int selfDamageDelay = 2;
    public boolean blackPowder = false;
    public boolean renderRotations = true;

    /* SETTERS — one per field, chained in the XFactory declarations. */

    public BulletConfig setItem(java.util.function.Supplier<net.minecraft.world.item.Item> ammo) { this.ammo = ammo; return this; }
    public BulletConfig setBeam() { this.pType = ProjectileType.BEAM; return this; }
    public BulletConfig setChunkloading() { this.pType = ProjectileType.BULLET_CHUNKLOADING; return this; }
    public BulletConfig setCasing(ItemStack item, int amount) { this.casingItem = item; this.casingSupplier = null; this.casingAmount = amount; return this; }
    public BulletConfig setCasing(java.util.function.Supplier<ItemStack> item, int amount) { this.casingItem = null; this.casingSupplier = item; this.casingAmount = amount; return this; }
    public BulletConfig setCasing(ItemEnums.EnumCasingType item, int amount) {
        return setCasing(() -> ModCasingItems.casing(item, 1), amount);
    }
    public BulletConfig setCasing(CasingProfile profile) { this.casingProfile = profile; return this; }
    public BulletConfig setCasing(SpentCasing casing) {
        this.casing = casing;
        int[] colors = casing.getColors();
        this.casingProfile = new CasingProfile(casing.getName(), casing.getType().name(), casing.getScaleX(),
                casing.getBounceYaw(), casing.getBouncePitch(), colors == null || colors.length == 0 ? 0 : colors[0],
                casing.getSmokeChance(), casing.getSmokeLift(), casing.getSmokeDuration(), casing.getSmokeNodeLife());
        return this;
    }
    public ItemStack casingStack() { return casingSupplier == null ? casingItem : casingSupplier.get(); }
    public BulletConfig setReloadCount(int count) { this.ammoReloadCount = count; return this; }
    public BulletConfig setVel(float velocity) { this.velocity = velocity; return this; }
    public BulletConfig setSpread(float spread) { this.spread = spread; return this; }
    public BulletConfig setWear(float wear) { this.wear = wear; return this; }
    public BulletConfig setProjectiles(int amount) { this.projectilesMin = this.projectilesMax = amount; return this; }
    public BulletConfig setProjectiles(int min, int max) { this.projectilesMin = min; this.projectilesMax = max; return this; }
    public BulletConfig setDamage(float damageMult) { this.damageMult = damageMult; return this; }
    public BulletConfig setThresholdNegation(float negation) { this.armorThresholdNegation = negation; return this; }
    public BulletConfig setArmorPiercing(float percent) { this.armorPiercingPercent = percent; return this; }
    public BulletConfig setKnockback(float mult) { this.knockbackMult = mult; return this; }
    public BulletConfig setHeadshot(float mult) { this.headshotMult = mult; return this; }
    public BulletConfig setupDamageClass(DamageClass clazz) { this.dmgClass = clazz; return this; }
    public BulletConfig setRicochetAngle(float angle) { this.ricochetAngle = angle; return this; }
    public BulletConfig setRicochetCount(int count) { this.maxRicochetCount = count; return this; }
    public BulletConfig setDamageFalloffByPen(boolean falloff) { this.damageFalloffByPen = falloff; return this; }
    public BulletConfig setGrav(float gravity) { this.gravity = gravity; return this; }
    public BulletConfig setLife(int expires) { this.expires = expires; return this; }
    public BulletConfig setImpactsEntities(boolean impact) { this.impactsEntities = impact; return this; }
    public BulletConfig setDoesPenetrate(boolean pen) { this.doesPenetrate = pen; return this; }
    public BulletConfig setSpectral(boolean spectral) { this.isSpectral = spectral; return this; }
    public BulletConfig setSelfDamageDelay(int delay) { this.selfDamageDelay = delay; return this; }
    public BulletConfig setBlackPowder(boolean bp) { this.blackPowder = bp; return this; }
    public BulletConfig setRenderRotations(boolean rot) { this.renderRotations = rot; return this; }
    public BulletConfig setOnUpdate(Consumer<Entity> lambda) { this.onUpdate = lambda; return this; }
    public BulletConfig setOnRicochet(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) { this.onRicochet = lambda; return this; }
    public BulletConfig setOnImpact(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) { this.onImpact = lambda; return this; }
    public BulletConfig setOnBeamImpact(BiConsumer<EntityBulletBeamBase, HitResult> lambda) { this.onImpactBeam = lambda; return this; }
    public BulletConfig setOnEntityHit(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) { this.onEntityHit = lambda; return this; }

    /** The item one round is loaded from, or null before item registration has run. */
    public net.minecraft.world.item.Item ammoItem() {
        return ammo == null ? null : ammo.get();
    }

    /**
     * Whether {@code slot} holds a round of this type. Upstream compares a metadata'd
     * {@code ComparableStack}; here each variant is its own item, so item identity is the whole test.
     */
    public boolean matchesAmmo(ItemStack slot) {
        return !slot.isEmpty() && slot.getItem() == ammoItem();
    }

    /** One round of this type, for tooltips and the ammo HUD. */
    public ItemStack toStack() {
        net.minecraft.world.item.Item item = ammoItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public enum ProjectileType {
        BULLET,
        BULLET_CHUNKLOADING,
        BEAM
    }

    /** The immutable subset of original {@code SpentCasing} configuration used by the 240 mm shells. */
    public record CasingProfile(String id, String type, float scale, float bounceYaw, float bouncePitch,
            int color, float smokeChance, double smokeLift, int smokeDuration, int smokeNodeLife) {}
}

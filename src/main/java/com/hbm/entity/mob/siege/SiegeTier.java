package com.hbm.entity.mob.siege;

import com.hbm.items.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The siege mobs' tiers. {@link #id} is the runtime index the entities sync; saves use {@link #name}. */
public class SiegeTier {

    public static final SiegeTier[] tiers = new SiegeTier[100];
    private static int nextID = 0;

    public static int getLength() {
        return nextID;
    }

    public static final SiegeTier DEFAULT_BUFF = new SiegeTier(20, "buff").addDrop(SiegeTier::steel).setDR(0.2F).setDMG(2F).setLaser(0F, 0F, false);
    public static final SiegeTier CLAY = new SiegeTier(30, "clay").addDrop(SiegeTier::steel).setDR(0.2F).setDMG(3F).setLaser(0F, 0F, false);
    public static final SiegeTier STONE = new SiegeTier(40, "stone").addDrop(SiegeTier::steel).setDR(0.3F).setDT(1F).setFP().setDMG(5F).setLaser(0F, 0F, true);
    public static final SiegeTier IRON = new SiegeTier(50, "iron").addDrop(SiegeTier::steel).setDR(0.3F).setDT(2F).setFP().setDMG(7.5F).setFF().setLaser(0F, 1F, true);
    public static final SiegeTier SILVER = new SiegeTier(70, "silver").addDrop(SiegeTier::steel).setDR(0.5F).setDT(3F).setNF().setFP().setDMG(10F).setSP(0.5F).setFF().setLaser(0.01F, 1F, true);
    public static final SiegeTier GOLD = new SiegeTier(100, "gold").addDrop(SiegeTier::steel).setDR(0.5F).setDT(5F).setNF().setFP().setDMG(15F).setSP(0.5F).setFF().setLaser(0.02F, 1.5F, true);
    public static final SiegeTier DESH = new SiegeTier(150, "desh").addDrop(SiegeTier::steel).setDR(0.7F).setDT(7F).setNF().setFP().setDMG(25F).setSP(0.5F).setFF().setLaser(0.05F, 1.5F, true);
    public static final SiegeTier SCHRAB = new SiegeTier(250, "schrab").addDrop(SiegeTier::steel).setDR(0.7F).setDT(10F).setNF().setFP().setDMG(50F).setSP(1F).setFF().setLaser(0.1F, 2F, true);
    public static final SiegeTier DNT = new SiegeTier(500, "dnt").addDrop(SiegeTier::steel).setDR(0.9F).setDT(20F).setNF().setFP().setDMG(100F).setSP(1F).setFF().setLaser(0.2F, 2F, true);

    public final int id;
    public float dt = 0F;
    public float dr = 0F;
    public float health = 20F;
    public final String name;
    public float damageMod = 1F;
    public float speedMod = 0F;
    public boolean fireProof = false;
    public boolean noFall = false;
    public boolean noFriendlyFire = false;
    // built on use: the items are registry objects and this class may load before registration
    private final List<Supplier<ItemStack>> dropItem = new ArrayList<>();

    public float laserBreak = 0F;
    public float laserExplosive = 0F;
    public boolean laserIncendiary = false;

    //so this is basically delegates but in java? or like, uh, storing lambdas? i don't know what it is but i feel like playing god. i like it.
    public @Nullable Consumer<LivingEntity> delegate;

    private SiegeTier(float baseHealth, String name) {
        this.id = nextID;
        SiegeTier.tiers[this.id] = this;
        nextID++;

        this.health = baseHealth;
        this.name = name;
    }

    private static ItemStack steel() {
        return new ItemStack(ModItems.ingot_steel.get());
    }

    public static @Nullable SiegeTier byName(String name) {
        for (int i = 0; i < nextID; i++) if (tiers[i].name.equals(name)) return tiers[i];
        return null;
    }

    private SiegeTier setDT(float dt) {
        this.dt = dt;
        return this;
    }

    private SiegeTier setDR(float dr) {
        this.dr = dr;
        return this;
    }

    private SiegeTier setDMG(float dmg) {
        this.damageMod = dmg;
        return this;
    }

    private SiegeTier setSP(float sp) {
        this.speedMod = sp;
        return this;
    }

    private SiegeTier setFP() {
        this.fireProof = true;
        return this;
    }

    private SiegeTier setNF() {
        this.noFall = true;
        return this;
    }

    private SiegeTier setFF() {
        this.noFriendlyFire = true;
        return this;
    }

    private SiegeTier setLaser(float breaking, float explosive, boolean incendiary) {
        this.laserBreak = breaking;
        this.laserExplosive = explosive;
        this.laserIncendiary = incendiary;
        return this;
    }

    private SiegeTier addDrop(Supplier<ItemStack> drop) {
        this.dropItem.add(drop);
        return this;
    }

    public List<ItemStack> dropItems() {
        List<ItemStack> out = new ArrayList<>();
        for (Supplier<ItemStack> drop : this.dropItem) out.add(drop.get());
        return out;
    }

    public void runDelegate(LivingEntity entity) {
        if (this.delegate != null) this.delegate.accept(entity);
    }
}

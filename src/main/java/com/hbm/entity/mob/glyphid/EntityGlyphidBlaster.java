package com.hbm.entity.mob.glyphid;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.registry.LivingAttributes;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Entities;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.main.ResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class EntityGlyphidBlaster extends EntityGlyphidBombardier {

    public static final RegistryObject<EntityType<EntityGlyphidBlaster>> TYPE =
            ModNtmEntities.ENTITY_TYPES.register("entity_glyphid_blaster", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityGlyphidBlaster>of(EntityGlyphidBlaster::new, MobCategory.MONSTER)).sized(2F, 1.125F).clientTrackingRange(5).updateInterval(3), key));

    static {
        LivingAttributes.register(TYPE, EntityGlyphidBlaster::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.createAttributes()
                                       .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBlaster().health)
                                       .add(Attributes.MOVEMENT_SPEED, GlyphidStats.getStats().getBlaster().speed)
                                       .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBlaster().damage);
    }

    public EntityGlyphidBlaster(EntityType<? extends EntityGlyphidBlaster> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_blaster_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(GlyphidStats.getStats().getBlaster().health);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(GlyphidStats.getStats().getBlaster().speed);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(GlyphidStats.getStats().getBlaster().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBlaster;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    public float getBombDamage() {
        return 15F;
    }

    @Override
    public int getBombCount() {
        return 10;
    }

    @Override
    public float getSpreadMult() {
        return 0.5F;
    }

    @Override
    public double getV0() {
        return 1.25D;
    }
}

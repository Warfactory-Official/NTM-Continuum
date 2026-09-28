package com.hbm.entity.mob;

import com.hbm.blocks.bomb.ModBombBlocks;
import dev.golem.core.world.Compat;
import com.hbm.blocks.bomb.BlockTaint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.interfaces.IRadiationImmune;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

public class EntityCreeperTainted extends EntityCreeperBase implements IRadiationImmune {

    public EntityCreeperTainted(EntityType<? extends EntityCreeperTainted> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                      .add(Attributes.MAX_HEALTH, 15.0D)
                      .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    public void tick() {
        super.tick();

        if (isAlive() && getHealth() < getMaxHealth() && tickCount % 10 == 0) heal(1.0F);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // 1.7 ModEventHandler.onEntityDeath: bobHidden for every player within 50 of a tainted creeper a boxcar
        // crushed
        if (level() instanceof ServerLevel server && source.is(com.hbm.lib.ModDamageSources.BOXCAR)) {
            String kind = net.minecraft.world.entity.EntityType.getKey(getType()).toString();
            com.hbm.advancement.AwardRegions.nearby(server, getBoundingBox().inflate(50D),
                    player -> com.hbm.advancement.HbmCriteria.CRUSHED.fire(player, kind));
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            looting = dev.golem.core.world.Compat.lootingLevel(level, attacker);
        }

        int amount = random.nextInt(3) + (looting > 0 ? random.nextInt(looting + 1) : 0);
        for (int i = 0; i < amount; i++) spawnAtLocation(level, new ItemStack(Items.TNT));
    }

    @Override
    protected void detonate(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.NONE);

        if (!Compat.canEntityGrief(level, this)) return;

        // TAINT_TRAILS keeps the scatter young so it keeps creeping; without it the taint starts old
        boolean trails = com.hbm.config.ServerConfig.TAINT_TRAILS.get(); // 1.7 read the /ntmserver key

        if (isPowered()) {
            scatter(level, 255, 15, 7, trails ? 0 : 5, 3);
        } else {
            scatter(level, 85, 7, 3, trails ? 4 : 10, trails ? 3 : 6);
        }
    }

    private void scatter(ServerLevel level, int count, int spread, int offset, int ageBase, int ageSpread) {
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(
                    random.nextInt(spread) + (int) getX() - offset,
                    random.nextInt(spread) + (int) getY() - offset,
                    random.nextInt(spread) + (int) getZ() - offset);

            BlockState state = level.getBlockState(pos);
            if (!dev.golem.core.world.Compat.isSolidRender(state) || state.isAir()) continue;

            level.setBlock(pos, ModBombBlocks.taint.get().defaultBlockState()
                    .setValue(BlockTaint.TAINTAGE, random.nextInt(ageSpread) + ageBase), Block.UPDATE_CLIENTS);
        }
    }
}

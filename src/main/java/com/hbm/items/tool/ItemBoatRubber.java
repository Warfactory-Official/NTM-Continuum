package com.hbm.items.tool;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.item.EntityBoatRubber;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import dev.golem.core.world.CompatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
// 26.1: BoatItem requires an entity type before the fixed entity handles bind, so this class follows its use
// flow with a deferred rubber-boat lookup.
public class ItemBoatRubber extends CompatItem {

    public ItemBoatRubber(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

        if (hit.getType() == HitResult.Type.MISS) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Vec3 look = player.getViewVector(1F);
        List<Entity> blocking = level.getEntities(player,
                player.getBoundingBox().expandTowards(look.scale(5D)).inflate(1D), e -> e.isAlive() && e.isPickable() /* 26.1 EntitySelector.CAN_BE_PICKED */);

        if (!blocking.isEmpty()) {
            Vec3 eye = player.getEyePosition();

            for (Entity entity : blocking) {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (box.contains(eye)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
        }

        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.TRY_WITH_EMPTY_HAND;

        EntityBoatRubber boat = Entities.create(ModNtmEntities.BOAT_RUBBER.get(), level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (boat == null) return InteractionResult.FAIL;

        Vec3 at = hit.getLocation();
        Compat.setInitialPos(boat, at.x, at.y, at.z);
        boat.setYRot(player.getYRot());
        if (level instanceof ServerLevel server) {
            EntityType.<EntityBoatRubber>createDefaultStackConfig(server, stack, player).accept(boat);
        }

        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResult.FAIL;

        if (!level.isClientSide()) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            ItemStacks.consume(stack, 1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}

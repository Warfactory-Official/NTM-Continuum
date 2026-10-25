package com.hbm.items.special;

import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

// 1.7: ItemChopper is a spawn egg for one fixed NTM mob; constructor arguments replace item-identity dispatch.
// 26.1: SpawnEggItem requires an entity type before the fixed entity handles bind, so this class follows its
// use flow with a deferred type and omits only 1.7's spawner-block arm.
public class ItemChopper extends CompatItem {

    private final Supplier<EntityType<? extends Mob>> type;
    private final int launch;
    private final Consumer<Mob> spawned;
    private final String[] tooltip;

    public ItemChopper(Properties properties, Supplier<EntityType<? extends Mob>> type) {
        this(properties, type, 0, mob -> {
        });
    }

    public ItemChopper(Properties properties, Supplier<EntityType<? extends Mob>> type, int launch,
                       Consumer<Mob> spawned, String... tooltip) {
        super(properties);
        this.type = type;
        this.launch = launch;
        this.spawned = spawned;
        this.tooltip = tooltip;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        EntityType<? extends Mob> type = this.type.get();
        if (!type.isEnabled(context.getLevel().enabledFeatures())) return InteractionResult.FAIL;

        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(face);

        return spawn(type, level, context.getItemInHand(), context.getPlayer(), pos, true,
                !Objects.equals(clicked, pos) && face == Direction.UP);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.TRY_WITH_EMPTY_HAND;
        EntityType<? extends Mob> type = this.type.get();
        if (!type.isEnabled(level.enabledFeatures())) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos pos = hit.getBlockPos();
        // 1.7 BlockLiquid: vanilla water and lava, not Forge fluid blocks
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof LiquidBlock) || block instanceof FluidBlock) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }

        InteractionResult result = spawn(type, serverLevel, stack, player, pos, false, false);
        if (result == InteractionResult.SUCCESS) player.awardStat(Stats.ITEM_USED.get(this));
        return result;
    }

    private InteractionResult spawn(EntityType<? extends Mob> type, ServerLevel level, ItemStack stack,
                                    Player player, BlockPos pos,
                                    boolean tryMoveDown, boolean movedUp) {

        // 1.7: launched mobs start in open air, so target spawn settling must stay disabled.
        BlockPos spawnPos = pos.above(this.launch);
        Mob mob = type.spawn(level, stack, player, spawnPos,
                EntitySpawnReason.SPAWN_ITEM_USE, tryMoveDown && this.launch == 0, movedUp);
        if (mob == null) return InteractionResult.FAIL;

        this.spawned.accept(mob);
        ItemStacks.consume(stack, 1, player);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        for (String line : this.tooltip) tooltip.add(Component.literal(line));
    }
}

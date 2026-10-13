package com.hbm.items.tool;

import com.hbm.Constants;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.block.IToolable;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ToolAnimation;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.Ids;
import dev.golem.core.world.Attacks;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Boltgun: the {@link IToolable.ToolType#BOLT} tool. Right-clicking a {@link IToolable} block bolts it
 * into its upgraded form, consuming the bolts the conversion asks for. Its job is turning
 * plain Watz casing into the bolted casing the reactor structure requires.
 *
 * <p>It doubles as a weapon, 1.7 {@code onLeftClickEntity}: a swing at a living target spends one bolt from the
 * inventory (spike, steel, tungsten, then dura bolts, first slot first) for 10 armour-piercing damage that ignores
 * hurt cooldown, replacing the ordinary melee hit. A player killed that way earns "Go Fish". Both the bolted hit and a
 * bolted block play the barrel recoil ({@code RECOIL} bus, 1.7 {@code playAnimation(player, SWING)}).
 */
public class ItemBoltgun extends CompatItem implements IAnimatedItem {

    /** 1.7 had no attack cooldown: the recoil animation plays with the hand fully raised. */
    @Override
    public boolean compatHoldsRaised(ItemStack held) {
        return true;
    }

    public ItemBoltgun(Properties properties) {
        super(properties);
    }

    /** 1.7 order of {@code ItemBoltgun.onLeftClickEntity}'s bolt table. */
    private static final String[] BOLTS = {"bolt_spike", "bolt_steel", "bolt_tungsten", "bolt_dura_steel"};

    /** Hooks the melee replacement; called once from {@code ModAdvancementContent}. */
    public static void installAttack() {
        Attacks.interceptPlayerAttack(ItemBoltgun::onLeftClickEntity);
    }

    /** 1.7 {@code onLeftClickEntity}; {@code true} takes the swing over from vanilla's attack. */
    static boolean onLeftClickEntity(Player player, Entity entity) {
        if (!(player.getMainHandItem().getItem() instanceof ItemBoltgun) || !entity.isAlive()) return false;
        Inventory inventory = player.getInventory();
        for (String bolt : BOLTS) {
            Item item = BuiltInRegistries.ITEM.getValue(
                    Ids.of(Constants.MOD_ID, bolt));
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                if (slot.isEmpty() || !slot.is(item)) continue;
                if (player.level() instanceof ServerLevel server) {
                    server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.BOLTGUN.get(),
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    inventory.removeItem(i, 1);
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, ModDamageSources.source(
                            server, ModDamageSources.BOLTGUN, player, player), 10F);
                    // 1.7 achGoFish goes to the victim
                    if (!entity.isAlive() && entity instanceof ServerPlayer victim) {
                        HbmCriteria.BOLTED.fire(victim);
                    }
                    server.sendParticles(ParticleTypes.EXPLOSION, entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2D, entity.getZ(), 1, 0D, 0D, 0D, 0D);
                    ((ItemBoltgun) player.getMainHandItem().getItem()).playAnimation(player,
                            ToolAnimation.SWING, player.getMainHandItem());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof IToolable toolable)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (toolable.onScrew(level, player, pos, context.getClickedFace(), context.getClickLocation(),
                context.getHand(), IToolable.ToolType.BOLT)) {
            level.playSound(null, pos, ModSounds.BOLTGUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            playAnimation(player, ToolAnimation.SWING, context.getItemInHand());
        }
        return InteractionResult.SUCCESS;
    }
}

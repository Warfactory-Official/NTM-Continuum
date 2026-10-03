package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Boltgun — the {@link IToolable.ToolType#BOLT} tool. Right-clicking a {@link IToolable} block bolts it
 * into its upgraded form, consuming the bolts the conversion asks for. Its job in this wave is turning
 * plain Watz casing into the bolted casing the reactor structure requires.
 *
 * <p>It doubles as a weapon, 1.7 {@code onLeftClickEntity}: a swing at a living target spends one bolt from the
 * inventory (spike, steel, tungsten, then dura bolts, first slot first) for 10 armour-piercing damage that ignores
 * hurt cooldown, replacing the ordinary melee hit. A player killed that way earns "Go Fish". Both the bolted hit and a
 * bolted block play the barrel recoil ({@code RECOIL} bus, 1.7 {@code playAnimation(player, SWING)}).
 */
public class ItemBoltgun extends CompatItem implements com.hbm.items.IAnimatedItem {

    /** 1.7 had no attack cooldown: the recoil animation plays with the hand fully raised. */
    @Override
    public boolean compatHoldsRaised(net.minecraft.world.item.ItemStack held) {
        return true;
    }

    public ItemBoltgun(Properties properties) {
        super(properties);
    }

    /** 1.7 order of {@code ItemBoltgun.onLeftClickEntity}'s bolt table. */
    private static final String[] BOLTS = {"bolt_spike", "bolt_steel", "bolt_tungsten", "bolt_dura_steel"};

    /** Hooks the melee replacement; called once from {@code ModAdvancementContent}. */
    public static void installAttack() {
        dev.golem.core.world.Attacks.interceptPlayerAttack(ItemBoltgun::onLeftClickEntity);
    }

    /** 1.7 {@code onLeftClickEntity}; {@code true} takes the swing over from vanilla's attack. */
    static boolean onLeftClickEntity(Player player, net.minecraft.world.entity.Entity entity) {
        if (!(player.getMainHandItem().getItem() instanceof ItemBoltgun) || !entity.isAlive()) return false;
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (String bolt : BOLTS) {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    dev.golem.core.Ids.of(com.hbm.Constants.MOD_ID, bolt));
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack slot = inventory.getItem(i);
                if (slot.isEmpty() || !slot.is(item)) continue;
                if (player.level() instanceof net.minecraft.server.level.ServerLevel server) {
                    server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.BOLTGUN.get(),
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    inventory.removeItem(i, 1);
                    com.hbm.util.EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, com.hbm.lib.ModDamageSources.source(
                            server, com.hbm.lib.ModDamageSources.BOLTGUN, player, player), 10F);
                    // 1.7 achGoFish goes to the victim
                    if (!entity.isAlive() && entity instanceof net.minecraft.server.level.ServerPlayer victim) {
                        com.hbm.advancement.HbmCriteria.BOLTED.fire(victim);
                    }
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2D, entity.getZ(), 1, 0D, 0D, 0D, 0D);
                    ((ItemBoltgun) player.getMainHandItem().getItem()).playAnimation(player,
                            com.hbm.items.ToolAnimation.SWING, player.getMainHandItem());
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
            playAnimation(player, com.hbm.items.ToolAnimation.SWING, context.getItemInHand());
        }
        return InteractionResult.SUCCESS;
    }
}

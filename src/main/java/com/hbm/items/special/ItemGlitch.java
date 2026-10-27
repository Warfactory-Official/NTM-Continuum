package com.hbm.items.special;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModGunSupportItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModMissileItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.main.MainRegistry;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemStacks;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemGlitch}: one use (max damage 1, damaged by 5) that rolls one of 31 outcomes. Its icon
 * ({@code glitch_<id>}) and last tooltip line follow {@link MainRegistry#polaroidID}. 1.7's treasure-block
 * outcomes give the block itself: {@code getItemDropped} is not overridden there, only {@code getDrops}.
 */
public class ItemGlitch extends CompatItem implements IBatteryItem {

    public ItemGlitch(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.CONSUME;
        ItemStacks.hurtAndBreak(player.getItemInHand(hand), 5, player, hand);

        effect(level.getRandom().nextInt(31), server, player);
        player.inventoryMenu.broadcastChanges();
        return InteractionResult.CONSUME;
    }

    /** One of 1.7's 31 outcomes, picked by {@code roll}. */
    public static void effect(int roll, ServerLevel level, Player player) {
        RandomSource rand = level.getRandom();
        Inventory inventory = player.getInventory();
        switch (roll) {
            case 0 -> say(player, "Sorry nothing.");
            case 1 -> say(player, "Prometheus was punished by the gods by giving the gift of knowledge to man. He was cast into the bowels of the earth and pecked by birds.");
            case 2 -> Compat.hurt(player, level, ModDamageSources.radiation(level), 1000);
            case 3 -> Compat.hurt(player, level, ModDamageSources.source(level, ModDamageSources.BOXCAR), 1000);
            case 4 -> Compat.hurt(player, level, ModDamageSources.blackhole(level), 1000);
            case 5 -> inventory.add(new ItemStack(ModBlocks.block_meteor_treasure.get()));
            case 6 -> {
                for (int i = 0; i < 3; i++) inventory.add(new ItemStack(ModBlocks.block_meteor_treasure.get()));
            }
            case 7 -> {
                for (int i = 0; i < 10; i++) inventory.add(new ItemStack(ModBlocks.block_meteor_treasure.get()));
            }
            case 8 -> {
                inventory.add(new ItemStack(ModGunSupportItems.ammo_container.get(), 10));
                say(player, "Oh, and by the way: The polaroid shifts reality. Things can be different if the polaroid is broken.");
            }
            case 9 -> inventory.add(new ItemStack(ModMeleeKitItems.nuke_advanced_kit.get(), 1));
            case 10 -> inventory.add(new ItemStack(ModMeleeKitItems.nuke_starter_kit.get(), 1));
            case 11 -> {
                EntityBoxcar pip = new EntityBoxcar(ModNtmEntities.BOXCAR.get(), level);
                pip.setPos(player.getX(), player.getY() + 50, player.getZ());
                level.addFreshEntity(pip);
            }
            case 12 -> {
                for (int i = 0; i < 10; i++) {
                    EntityBoxcar pippo = new EntityBoxcar(ModNtmEntities.BOXCAR.get(), level);
                    pippo.setPos(player.getX() + rand.nextGaussian() * 25, player.getY() + 50,
                            player.getZ() + rand.nextGaussian() * 25);
                    level.addFreshEntity(pippo);
                }
            }
            case 13 -> {
                inventory.add(new ItemStack(ModGunItems.gun_heavy_revolver_lilmac.get()));
                inventory.add(new ItemStack(ModConsumableTailItems.bottle_sparkle.get()));
                inventory.add(new ItemStack(ModGearItems.geiger_counter.get()));
                say(player, "Have some free stuff. You'll need it for that one cryptic achievement.");
            }
            case 14 -> {
                inventory.dropAll();
                ExplosionChaos.igniteAllBlocks(level, (int) player.getX(), (int) player.getY(), (int) player.getZ(), 5);
            }
            case 15 -> {
                for (int i = 0; i < 36; i++) inventory.add(new ItemStack(Items.DIRT, 64));
            }
            case 16 -> say(player, "v yvxr lbhe nggvghqr!");
            case 17 -> say(player, "89% of magic tricks are not magic. Technically, they are sorcery.");
            case 18 -> {
                inventory.add(new ItemStack(ModGunItems.gun_maresleg.get()));
                inventory.add(new ItemStack(ModAmmoItems.g12.get(), 12));
                say(player, "Here ya go.");
            }
            case 19 -> say(player, "Ë");
            case 20 -> say(player, "Good day, I am text");
            case 21 -> {
                inventory.add(new ItemStack(ModMissileItems.missile_nuclear.get()));
                say(player, "73616d706c652074657874!");
            }
            case 22 -> say(player, "Budget cuts, no effect for you.");
            case 23 -> say(player, "oof");
            case 24 -> {
                Effects.add(player, Effects.resistance(), 60 * 20, 9);
                say(player, "Tank!");
            }
            case 25 -> {
                Effects.add(player, Effects.strength(), 60 * 20, 9);
                say(player, "More devastating than a falling boxcar!");
            }
            case 26 -> {
                Effects.add(player, MobEffects.SLOWNESS, 60 * 20, 9);
                say(player, "Ha!");
            }
            case 27 -> {
                EntityVortex vortex = new EntityVortex(ModEntities.VORTEX.get(), level, 2.5F);
                vortex.setPos(player.getX(), player.getY() - 15, player.getZ());
                level.addFreshEntity(vortex);
            }
            case 28 -> {
                EntityMeteor mirv = new EntityMeteor(level);
                mirv.setPos(player.getX(), player.getY() + 100, player.getZ());
                level.addFreshEntity(mirv);
                say(player, "Watch your head!");
            }
            case 29 -> {
                ExplosionLarge.spawnBurst(level, player.getX(), player.getY(), player.getZ(), 27, 3);
                say(player, "Bam!");
            }
            case 30 -> {
                inventory.add(new ItemStack(ModItems.plate_saturnite.get()));
                say(player, "It's dangerous to go alone, take this!");
            }
            default -> { }
        }
    }

    private static void say(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("It's a gamble!"));
        list.add(Component.literal(""));
        switch (MainRegistry.polaroidID) {
            case 1 -> list.add(Component.literal("Click-click-click!"));
            case 2 -> list.add(Component.literal("Creek!"));
            case 3 -> list.add(Component.literal("Bzzzt!"));
            case 4 -> list.add(Component.literal("TS staring off into space."));
            case 5 -> list.add(Component.literal("BANG!!"));
            case 6 -> list.add(Component.literal("Woop!"));
            case 7 -> list.add(Component.literal("Poow!"));
            case 8 -> list.add(Component.literal("Pft!"));
            case 9 -> list.add(Component.literal("GF fgnevat bss vagb fcnpr."));
            case 10 -> list.add(Component.literal("Backup memory #8 on 1.44 million bytes."));
            case 11 -> list.add(Component.literal("PTANG!"));
            case 12 -> list.add(Component.literal("Bzzt-zrrt!"));
            case 13 -> list.add(Component.literal("Clang, click-brrthththrtrtrtrtrtr!"));
            case 14 -> list.add(Component.literal("KABLAM!"));
            case 15 -> list.add(Component.literal("PLENG!"));
            case 16 -> list.add(Component.literal("Wheeeeeeee-"));
            case 17 -> list.add(Component.literal("Thump."));
            case 18 -> list.add(Component.literal("BANG! Choo-chooo! B A N G ! ! !"));
            default -> { }
        }
    }

    @Override public void chargeBattery(ItemStack stack, long i) { }
    @Override public void setCharge(ItemStack stack, long i) { }
    @Override public void dischargeBattery(ItemStack stack, long i) { }
    @Override public long getCharge(ItemStack stack) { return 200; }
    @Override public long getMaxCharge(ItemStack stack) { return 200; }
    @Override public long getChargeRate(ItemStack stack) { return 0; }
    @Override public long getDischargeRate(ItemStack stack) { return 200; }
}

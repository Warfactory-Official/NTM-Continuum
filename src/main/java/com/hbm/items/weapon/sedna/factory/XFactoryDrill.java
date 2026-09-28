package com.hbm.items.weapon.sedna.factory;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Harvests;
import dev.golem.core.world.Sounds;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class XFactoryDrill {

    public static final String D_REACH = "D_REACH";
    public static final String F_DTNEG = "F_DTNEG";
    public static final String F_PIERCE = "F_PIERCE";
    public static final String I_AOE = "I_AOE";
    public static final String I_HARVEST = "I_HARVEST";
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DRILL_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, true);
    };
    public static boolean didPlink = false;
    private XFactoryDrill() {}

    public static void init() {
        NtmToolGunCatalog.gun_drill = NtmToolGunCatalog.weapon("gun_drill",
                props -> new ItemGunDrill(props, WeaponQuality.UTILITY, new GunConfig()
                        .dura(3_000).draw(10).inspect(55).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0).dmg(10F).delay(20).dry(30).auto(true).jam(0)
                                .mag(new MagazineLiquidEngine(0, 4_000,
                                        () -> new FluidType[]{Fluids.GASOLINE, Fluids.GASOLINE_LEADED,
                                                Fluids.COALGAS, Fluids.COALGAS_LEADED}))
                                .offset(1, -0.0625 * 2.5, -0.25D)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_DRILL_FIRE))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                        .orchestra(Orchestras.ORCHESTRA_DRILL)));
    }

    public static void doStandardFire(ItemStack stack, LambdaContext ctx, GunAnimation anim, boolean calcWear) {
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        if (anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        IMagazine mag = primary.getMagazine(stack);
        HitResult mop = EntityDamageUtil.getMouseOver(ctx.getPlayer(), getModdableReach(stack, 5.0D));
        if (mop != null) {
            if (mop instanceof EntityHitResult entityHit) {
                float damage = primary.getBaseDamage(stack);
                if (entityHit.getEntity() instanceof LivingEntity living && living.level() instanceof ServerLevel server) {
                    float health = living.getHealth();
                    living.invulnerableTime = 0;
                    Vec3 before = living.getDeltaMovement();
                    boolean hurt = DamageResistanceHandler.hurt(server, living,
                            ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage,
                            getModdableDTNegation(stack, 2F), getModdablePiercing(stack, 0.15F));
                    if (!hurt) Compat.hurt(living, server,
                            ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage);
                    if (living.getHealth() >= health) {
                        living.setHealth(Math.max(0F, living.getHealth() - damage));
                    }
                    Vec3 knockback = living.getDeltaMovement().subtract(before);
                    living.setDeltaMovement(before.add(knockback.scale(0.1D)));
                    living.hurtMarked = true;
                } else if (entityHit.getEntity().level() instanceof ServerLevel server) {
                    Compat.hurt(entityHit.getEntity(), server,
                            ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage);
                }
            }
            if (player != null && mop instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                BlockPos hitPos = blockHit.getBlockPos();
                int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(stack, 1);
                for (int i = -aoe; i <= aoe; i++) for (int j = -aoe; j <= aoe; j++) for (int k = -aoe; k <= aoe; k++)
                    breakExtraBlock(player.level(), hitPos.getX() + i, hitPos.getY() + j, hitPos.getZ() + k,
                            player, stack, ctx.hand, hitPos.getX(), hitPos.getY(), hitPos.getZ());
                didPlink = false;
            }
        }

        int ammoToUse = XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_ENGINE_ELECTRIC) ? 1_000 : 10;
        mag.useUpAmmo(stack, ctx.inventory, ammoToUse);
        if (calcWear) ItemGunBaseNT.setWear(stack, index,
                Math.min(ItemGunBaseNT.getWear(stack, index), ctx.config.getDurability(stack)));
    }

    public static void breakExtraBlock(Level world, int x, int y, int z, Player playerEntity, ItemStack callbackStack,
                                       InteractionHand callbackHand, int refX, int refY, int refZ) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isEmptyBlock(pos) || !(playerEntity instanceof ServerPlayer player)) return;
        BlockState state = world.getBlockState(pos);

        ItemStack tool = callbackStack.isEmpty() ? player.getItemInHand(callbackHand) : callbackStack;
        // Keep the source canHarvestBlock gate separate from the server tryHarvestBlock equivalent.
        boolean canHarvest = tool.getItem() instanceof ItemGunDrill drill
                && drill.canHarvestBlock(state);
        if (!canHarvest || (state.getDestroySpeed(world, pos) == -1.0F
                && state.getDestroyProgress(player, world, pos) == 0.0F)
                || state.is(com.hbm.blocks.ModDungeonBlocks.stone_keyhole.get())) {
            if (!didPlink) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_BREAK,
                        SoundSource.PLAYERS, 0.5F, 0.8F + world.getRandom().nextFloat() * 0.6F);
                didPlink = true;
            }
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        boolean movedToMainHand = callbackHand != InteractionHand.MAIN_HAND;
        if (movedToMainHand) player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        boolean destroyed;
        try {
            destroyed = Harvests.destroyBlock(player, pos, tool,
                    movedToMainHand ? InteractionHand.MAIN_HAND : callbackHand, canHarvest);
        } finally {
            if (movedToMainHand) player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        }
        if (destroyed && state.is(net.minecraft.world.level.block.Blocks.STONE) &&
                world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(pos).inflate(0.75D)).isEmpty()) {
            Block.popResource(world, pos, new ItemStack(net.minecraft.world.level.block.Blocks.COBBLESTONE));
        }
        if (destroyed && world.getBlockState(pos).isAir()) world.levelEvent(null, 2001, pos, Block.getId(state));
    }

    public static double getModdableReach(ItemStack stack, double base) {
        return XWeaponModManager.eval(base, stack, D_REACH, NtmToolGunCatalog.gun_drill.get(), 0);
    }
    public static float getModdableDTNegation(ItemStack stack, float base) {
        return XWeaponModManager.eval(base, stack, F_DTNEG, NtmToolGunCatalog.gun_drill.get(), 0);
    }
    public static float getModdablePiercing(ItemStack stack, float base) {
        return XWeaponModManager.eval(base, stack, F_PIERCE, NtmToolGunCatalog.gun_drill.get(), 0);
    }
    public static int getModdableAoE(ItemStack stack, int base) {
        return XWeaponModManager.eval(base, stack, I_AOE, NtmToolGunCatalog.gun_drill.get(), 0);
    }
    public static int getModdableHarvestLevel(ItemStack stack, int base) {
        return XWeaponModManager.eval(base, stack, I_HARVEST, NtmToolGunCatalog.gun_drill.get(), 0);
    }
}

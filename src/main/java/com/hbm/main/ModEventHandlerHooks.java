package com.hbm.main;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.ConsumableTreatments;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.PollutionSyncPayload;
import com.hbm.potion.ModEffects;
import com.hbm.util.ArmorRegistry;
import dev.golem.core.autocraft.CraftingMachines;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.BlockBreaks;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemFirstUse;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.ItemUses;
import dev.golem.core.world.Sleeps;
import dev.golem.core.world.Spawns;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The 1.7 event handlers that needed a hook the port did not have, each on a neutral Golem Core
 * facade:
 * <ul>
 *   <li>{@code ModEventHandler.onFoodEaten} ({@code PlayerUseItemEvent.Finish}) - Core {@link ItemUses};</li>
 *   <li>{@code ModEventHandler.onBlockBreak} ({@code BreakEvent}): the coal dust and the heavy-metal lead poisoning;
 *       the gneiss achievement is the {@code hbm:stratum} advancement's own criterion - Core {@link BlockBreaks};</li>
 *   <li>{@code PollutionHandler.rampantTargetSetter} ({@code PlayerSleepInBedEvent}) - Core {@link Sleeps};</li>
 *   <li>{@code PollutionHandler.rampantScoutPopulator} ({@code WorldEvent.PotentialSpawns}) -
 *       Core {@link Spawns#onPotentialSpawns};</li>
 *   <li>{@code ModEventHandler.onPlayerInteract}'s 528 explosive energistics - Core {@link ItemFirstUse}.</li>
 * </ul>
 */
public final class ModEventHandlerHooks {

    private ModEventHandlerHooks() {}

    private static boolean installed;

    public static synchronized void init() {
        if (installed) return;
        installed = true;
        ItemUses.onFinish(ModEventHandlerHooks::onFoodEaten);
        ConsumableTreatments.init();
        BlockBreaks.onBreak(ModEventHandlerHooks::onBlockBreak);
        Sleeps.onSleepAttempt((player, pos) -> PollutionHandler.rampantTargetSetter(pos));
        Spawns.onPotentialSpawns((level, category, pos) -> PollutionHandler.rampantScoutPopulator(level, pos));
        ItemFirstUse.registerBlockClick(ModEventHandlerHooks::explosiveEnergistics);
        Lifecycle.onServerTick(ModEventHandlerHooks::syncPollution);
        Lifecycle.onServerStopping(server -> SENT_POLLUTION.clear());
    }

    private static final Map<UUID, float[]> SENT_POLLUTION = new HashMap<>();

    /**
     * 1.7 {@code PermaSyncHandler}'s pollution block: the pollution of the region each player stands in (an empty
     * region reads as zeros), sent when it changed for that player. The client smog reads the soot.
     */
    private static void syncPollution(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var data = PollutionHandler.getPollutionData(player.level(), player.blockPosition());
            float[] now = new float[PollutionType.VALUES.length];
            if (data != null) System.arraycopy(data.pollution, 0, now, 0, now.length);
            float[] last = SENT_POLLUTION.get(player.getUUID());
            if (last != null && Arrays.equals(last, now)) continue;
            SENT_POLLUTION.put(player.getUUID(), now);
            GolemNetwork.sendToPlayer(player, PollutionSyncPayload.CODEC,
                    new PollutionSyncPayload(now[0], now[1], now[2], now[3]));
        }
    }

    /** 1.7 {@code onPlayerInteract}: in 528 mode, right-clicking any AE2 block blows it up. */
    public static InteractionResult explosiveEnergistics(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!GeneralConfig.enable528ExplosiveEnergistics || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockPos pos = hit.getBlockPos();
        if (!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getNamespace().equals(CraftingMachines.AE2)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        level.destroyBlock(pos, false);
        ExplosionVNT vnt = new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, null);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 20).setupPiercing(5, 0.2F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code onFoodEaten}: a player finished eating a food laced on the anvil. */
    public static void onFoodEaten(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;
        if (ItemStacks.foodProperties(stack) == null) return; // 1.7: stack.getItem() instanceof ItemFood

        if (ModDataComponents.NTM_CYANIDE.getOr(stack, false)) {
            for (int i = 0; i < 10; i++) {
                Compat.hurt(player, level, level.getRandom().nextBoolean() ? ModDamageSources.euthanizedSelf(level)
                        : ModDamageSources.euthanizedSelf2(level), 1000F);
            }
        }
        if (ModDataComponents.NTM_RED_PILL.getOr(stack, false)) {
            for (int i = 0; i < 10; i++) {
                Effects.addRegistered(player, ModEffects.DEATH.get(), 60 * 60 * 20, 0);
            }
        }
    }

    /** 1.7 {@code onBlockBreak}, minus the gneiss achievement (see the class comment). */
    public static void onBlockBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // coal dust around a broken coal ore, coal block or lignite. Deepslate coal ore is the modern spelling of the
        // same ore below y 0 (1.7 had one coal ore at every depth).
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.COAL_BLOCK
                || block == ModBlocks.ore_lignite.get()) {
            for (Direction dir : Direction.values()) {
                BlockPos side = pos.relative(dir);
                // 1.7: rand.nextInt(2) == 0 && getBlock == Blocks.air; 1.7 had one air block, so cave air counts
                if (level.getRandom().nextInt(2) == 0 && level.getBlockState(side).isAir()) {
                    level.setBlockAndUpdate(side, ModTerrainBlocks.gas_coal.get().defaultBlockState());
                }
            }
        }

        if (RadiationConfig.enablePollution && RadiationConfig.enableLeadFromBlocks) {
            if (!ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, ArmorRegistry.HazardClass.PARTICLE_FINE)) {
                float metal = PollutionHandler.getPollution(level, pos, PollutionType.HEAVYMETAL);
                if (metal < 5) return;
                int amplifier = metal < 10 ? 0 : metal < 25 ? 1 : 2;
                Effects.addRegistered(player, ModEffects.LEAD.get(), 100, amplifier);
            }
        }
    }
}

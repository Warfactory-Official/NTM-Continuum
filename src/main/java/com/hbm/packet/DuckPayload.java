package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.config.MobConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.registry.Entities;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * C2S, the first O press of a client session: 1.7's {@code AuxButtonPacket(0, 0, 0, 999, 0)}. The server throws
 * one duck per player, ever, and the login notice "Press O to Duck!" stops once it has.
 */
public record DuckPayload() implements GolemPayload {

    /** 1.7 {@code ServerProxy.ID_DUCK}. */
    public static final int ID_DUCK = 0;

    public static final GolemCodec<DuckPayload> CODEC = GolemCodec.of(DuckPayload.class,
            Ids.of(Constants.MOD_ID, "duck"), (payload, buffer) -> {}, buffer -> new DuckPayload());

    /** 1.7 {@code ModEventHandler.onPlayerLogin}: 30 seconds of the notice for a player who never ducked. */
    public static void onJoin(ServerPlayer player) {
        if (MobConfig.enableDucks && !HbmPlayerProps.getData(player).hasDucked)
            GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC,
                    PlayerInformPayload.of(ID_DUCK, 30_000, ChatFormatting.RESET, "Press O to Duck!"));
    }

    public static void handle(ServerPlayer player) {
        EntityHbmProps props = HbmPlayerProps.getData(player);
        if (!MobConfig.enableDucks || props.hasDucked) return;
        ServerLevel level = (ServerLevel) player.level();
        EntityDuck duck = Entities.create(ModNtmEntities.DUCK.get(), level, EntitySpawnReason.EVENT);
        duck.setPos(player.getX(), player.getEyeY(), player.getZ());
        Vec3 look = player.getLookAngle();
        duck.setDeltaMovement(look);
        level.addFreshEntity(duck);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ENTITY_DUCC.get(),
                player.getSoundSource(), 1.0F, 1.0F);
        props.hasDucked = true;
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}

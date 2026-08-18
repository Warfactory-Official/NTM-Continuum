package com.hbm.gametest;

import com.hbm.packet.ParticlePayload;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * GameTests for the particle system's server half.
 *
 * <p>Only the server half is reachable here — building the actual particles needs a client, a camera
 * and an atlas, none of which exist under the test server. What these pin down is everything up to
 * that boundary: that the payload survives a wire round trip with its tag intact, that effect names
 * resolve back to constants, and that an effect with no handler (which is every effect on a server)
 * is a silent no-op rather than a crash. That last one matters because {@code HbmEffectNT} is
 * deliberately loadable on a dedicated server, and a null handler there is the normal case.
 */
public final class ParticleGameTests {

    private ParticleGameTests() {}

    /** The payload survives encode/decode with its position and parameter tag unchanged. */
    public static void payloadRoundTrip(GameTestHelper helper) {
        CompoundTag data = new CompoundTag();
        data.putInt("cloudCount", 10);
        data.putFloat("cloudScale", 0.25F);
        data.putDouble("hX", -1.5D);

        ParticlePayload sent = new ParticlePayload(HbmEffectNT.BlackPowder.name(), 1.5D, 64.25D, -3.75D, data);

        var buffer = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        ParticlePayload.CODEC.encode(sent, buffer);
        ParticlePayload received = ParticlePayload.CODEC.decode(buffer);

        if (!sent.effect().equals(received.effect())) {
            helper.fail("Effect name did not survive the round trip: " + received.effect());
        }
        if (sent.x() != received.x() || sent.y() != received.y() || sent.z() != received.z()) {
            helper.fail("Position did not survive the round trip");
        }
        if (Tags.getIntOr(received.data(), "cloudCount", -1) != 10) {
            helper.fail("Int parameter did not survive the round trip");
        }
        if (Tags.getFloatOr(received.data(), "cloudScale", -1F) != 0.25F) {
            helper.fail("Float parameter did not survive the round trip");
        }
        if (Tags.getDoubleOr(received.data(), "hX", 0D) != -1.5D) {
            helper.fail("Double parameter did not survive the round trip");
        }
        helper.succeed();
    }

    /** Every effect resolves by its own name, and an unknown name resolves to null rather than throwing. */
    public static void effectNamesResolve(GameTestHelper helper) {
        for (HbmEffectNT effect : HbmEffectNT.values()) {
            if (HbmEffectNT.byName(effect.name()) != effect) {
                helper.fail("Effect did not resolve by its own name: " + effect.name());
            }
        }
        if (HbmEffectNT.byName("NoSuchEffect") != null) {
            helper.fail("An unknown effect name should resolve to null");
        }
        helper.succeed();
    }

    /**
     * Playing an effect with no registered handler does nothing and throws nothing.
     *
     * <p>This is the dedicated-server case: the enum loads, {@code send} is callable, and no client
     * handler will ever be attached. It must not be a crash.
     */
    public static void unhandledEffectIsSilent(GameTestHelper helper) {
        for (HbmEffectNT effect : HbmEffectNT.values()) {
            if (effect.hasHandler()) continue;
            effect.summonParticle(helper.getLevel(), 0, 0, 0, null);
        }
        helper.succeed();
    }

    /**
     * {@code send} from a server level reaches the network layer without throwing, and is a no-op
     * when handed a level that is not a {@code ServerLevel}.
     */
    public static void sendDoesNotThrow(GameTestHelper helper) {
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        com.hbm.particle.helper.ParticleEffects.blackPowder(helper.getLevel(),
                pos.getX(), pos.getY(), pos.getZ(), 0, 1, 0, 10, 0.25F, 0.5F, 10, 0.25F);
        com.hbm.particle.helper.ParticleEffects.explosionSmall(helper.getLevel(),
                pos.getX(), pos.getY(), pos.getZ(), 10, 2F, 0.5F);
        helper.succeed();
    }
}

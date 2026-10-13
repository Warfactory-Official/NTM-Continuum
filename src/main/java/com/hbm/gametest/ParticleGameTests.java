package com.hbm.gametest;

import com.hbm.packet.ParticlePayload;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.particle.helper.ParticleEffects;
import dev.golem.core.data.Tags;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * GameTests for the particle system's server half.
 *
 * <p>Only the server half is reachable here: building the actual particles needs a client, a camera
 * and an atlas. These pin down everything up to that boundary: the payload survives a wire round
 * trip with its tag intact, effect names resolve back to constants, and an effect with no handler
 * (every effect on a server) is a silent no-op rather than a crash, since {@code HbmEffectNT} loads
 * on a dedicated server and a null handler there is the normal case.
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

        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
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

        CompoundTag towerData = new CompoundTag();
        towerData.putFloat("lift", 0F);
        towerData.putFloat("base", 0.55F);
        towerData.putFloat("max", 0.56F);
        towerData.putFloat("strafe", 1F);
        towerData.putInt("life", 560);
        towerData.putInt("color", 0x404040);
        towerData.putBoolean("noWind", true);
        towerData.putFloat("alpha", 0.25F);
        ParticlePayload tower = roundTrip(helper,
                new ParticlePayload(HbmEffectNT.Tower.name(), 2D, 3D, 4D, towerData));
        if (HbmEffectNT.byName(tower.effect()) != HbmEffectNT.Tower
                || Tags.getFloatOr(tower.data(), "lift", -1F) != 0F
                || Tags.getFloatOr(tower.data(), "base", -1F) != 0.55F
                || Tags.getFloatOr(tower.data(), "max", -1F) != 0.56F
                || Tags.getFloatOr(tower.data(), "strafe", -1F) != 1F
                || Tags.getIntOr(tower.data(), "life", -1) != 560
                || Tags.getIntOr(tower.data(), "color", -1) != 0x404040
                || !tower.data().contains("noWind")
                || Tags.getFloatOr(tower.data(), "alpha", -1F) != 0.25F) {
            helper.fail("Tower payload did not preserve its legacy parameters");
        }

        CompoundTag fluidFillData = new CompoundTag();
        fluidFillData.putDouble("mX", -1D);
        fluidFillData.putDouble("mY", 0D);
        fluidFillData.putDouble("mZ", 0.25D);
        fluidFillData.putInt("color", 0x123456);
        ParticlePayload fluidFill = roundTrip(helper,
                new ParticlePayload(HbmEffectNT.FluidFill.name(), 5D, 6D, 7D, fluidFillData));
        if (HbmEffectNT.byName(fluidFill.effect()) != HbmEffectNT.FluidFill
                || Tags.getDoubleOr(fluidFill.data(), "mX", 0D) != -1D
                || Tags.getDoubleOr(fluidFill.data(), "mY", -1D) != 0D
                || Tags.getDoubleOr(fluidFill.data(), "mZ", 0D) != 0.25D
                || Tags.getIntOr(fluidFill.data(), "color", -1) != 0x123456) {
            helper.fail("FluidFill payload did not preserve its legacy parameters");
        }

        CompoundTag mukeData = new CompoundTag();
        mukeData.putBoolean("balefire", true);
        ParticlePayload muke = roundTrip(helper,
                new ParticlePayload(HbmEffectNT.Muke.name(), 8D, 9D, 10D, mukeData));
        if (HbmEffectNT.byName(muke.effect()) != HbmEffectNT.Muke
                || !Tags.getBooleanOr(muke.data(), "balefire", false)) {
            helper.fail("Muke payload did not preserve its balefire selector");
        }
        ParticlePayload tinyTot = roundTrip(helper,
                new ParticlePayload(HbmEffectNT.TinyTot.name(), 11D, 12D, 13D, new CompoundTag()));
        if (HbmEffectNT.byName(tinyTot.effect()) != HbmEffectNT.TinyTot) {
            helper.fail("TinyTot payload did not preserve its effect name");
        }
        helper.succeed();
    }

    private static ParticlePayload roundTrip(GameTestHelper helper, ParticlePayload sent) {
        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        ParticlePayload.CODEC.encode(sent, buffer);
        return ParticlePayload.CODEC.decode(buffer);
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
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ParticleEffects.blackPowder(helper.getLevel(),
                pos.getX(), pos.getY(), pos.getZ(), 0, 1, 0, 10, 0.25F, 0.5F, 10, 0.25F);
        ParticleEffects.explosionSmall(helper.getLevel(),
                pos.getX(), pos.getY(), pos.getZ(), 10, 2F, 0.5F);

        CompoundTag towerData = new CompoundTag();
        towerData.putFloat("lift", 0F);
        towerData.putFloat("base", 0.55F);
        towerData.putFloat("max", 0.56F);
        towerData.putFloat("strafe", 1F);
        towerData.putInt("life", 560);
        towerData.putInt("color", 0x404040);
        HbmEffectNT.Tower.send(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), towerData);

        CompoundTag fluidFillData = new CompoundTag();
        fluidFillData.putDouble("mX", -1D);
        fluidFillData.putDouble("mY", 0D);
        fluidFillData.putDouble("mZ", 0D);
        fluidFillData.putInt("color", 0x123456);
        HbmEffectNT.FluidFill.send(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), fluidFillData);
        helper.succeed();
    }
}

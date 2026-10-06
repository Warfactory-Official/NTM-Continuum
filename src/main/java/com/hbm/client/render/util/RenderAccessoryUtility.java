package com.hbm.client.render.util;

import com.hbm.main.MainRegistry;
import com.hbm.util.ShadyUtil;
import dev.golem.core.Ids;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code RenderAccessoryUtility.getCloakFromPlayer}: the HBM cape each listed player wears, installed through
 * Core's {@link dev.golem.core.client.Capes}.
 *
 * <p>1.7's table, plus old-hbm's one addition (Golem). Where the two disagree 1.7 is kept: Bob's {@code CapeHbm3}
 * (old-hbm used {@code CapeHbm} and inverted the polaroid swap), {@code CapeNostalgia2}, {@code CapeHoboy_mk3}, and
 * ShimmeringBlaze's polaroid variant. Alcater's cape is not ported (user decision, 2026-10-06). Pheo's
 * {@code CapePheo} stays commented out as in 1.7.
 *
 * <p>The test cape went to any name starting with "Player", which is what a 1.7 dev client was called. The port's
 * dev clients are {@code Player###} on Fabric and {@code Dev} on NeoForge/Forge, so {@code Dev} wears it too (user
 * decision, 2026-10-06).
 */
public final class RenderAccessoryUtility {

    private RenderAccessoryUtility() {}

    private static final Identifier hbm = cape("capehbm3");
    private static final Identifier hbm2 = cape("capehbm2");
    private static final Identifier drillgon = cape("capedrillgon");
    private static final Identifier dafnik = cape("capedafnik");
    private static final Identifier lpkukin = cape("capeshield");
    private static final Identifier vertice = cape("capevertice_2");
    private static final Identifier red = cape("capered");
    private static final Identifier ayy = cape("capeayy");
    private static final Identifier nostalgia2 = cape("capenostalgia2");
    private static final Identifier sam = cape("capesam");
    private static final Identifier hoboy = cape("capehoboy_mk3");
    private static final Identifier master = cape("capemaster");
    private static final Identifier mek = cape("capemek");
    private static final Identifier zippy = cape("capezippysqrl");
    private static final Identifier test = cape("capetest");
    private static final Identifier schrabby = cape("capeschrabbyalt");
    private static final Identifier swiggs = cape("capesweatyswiggs");
    private static final Identifier doctor17 = cape("capedoctor17");
    private static final Identifier shimmeringblaze = cape("capeblaze");
    private static final Identifier blaze2 = cape("capeblaze2");
    private static final Identifier wiki = cape("capewiki");
    private static final Identifier leftnugget = cape("capeleftnugget");
    private static final Identifier rightnugget = cape("caperightnugget");
    private static final Identifier tankish = cape("capetankish");
    private static final Identifier frizzlefrazzle = cape("capefrizzlefrazzle");
    private static final Identifier vaer = cape("capevaer");
    private static final Identifier adam = cape("capeadam");
    private static final Identifier jame = cape("capejame");
    private static final Identifier golem = cape("capegolem"); // old-hbm

    private static Identifier cape(String name) {
        return Ids.of("hbm", "textures/models/capes/" + name + ".png");
    }

    public static @Nullable Identifier getCloakFromPlayer(UUID id, String name) {
        String uuid = id.toString();

        if (uuid.equals(ShadyUtil.HbMinecraft)) return MainRegistry.polaroidID == 11 ? hbm2 : hbm;
        if (uuid.equals(ShadyUtil.Drillgon)) return drillgon;
        if (uuid.equals(ShadyUtil.Dafnik)) return dafnik;
        if (uuid.equals(ShadyUtil.LPkukin)) return lpkukin;
        if (uuid.equals(ShadyUtil.LordVertice)) return vertice;
        if (uuid.equals(ShadyUtil.CodeRed_)) return red;
        if (uuid.equals(ShadyUtil.dxmaster769)) return ayy;
        if (uuid.equals(ShadyUtil.Dr_Nostalgia)) return nostalgia2;
        if (uuid.equals(ShadyUtil.Samino2)) return sam;
        if (uuid.equals(ShadyUtil.Hoboy03new)) return hoboy;
        if (uuid.equals(ShadyUtil.Dragon59MC)) return master;
        if (uuid.equals(ShadyUtil.Steelcourage)) return mek;
        if (uuid.equals(ShadyUtil.ZippySqrl)) return zippy;
        if (uuid.equals(ShadyUtil.Schrabby)) return schrabby;
        if (uuid.equals(ShadyUtil.SweatySwiggs)) return swiggs;
        if (uuid.equals(ShadyUtil.Doctor17) || uuid.equals(ShadyUtil.Doctor17PH)) return doctor17;
        if (uuid.equals(ShadyUtil.ShimmeringBlaze)) return MainRegistry.polaroidID == 11 ? blaze2 : shimmeringblaze;
        if (uuid.equals(ShadyUtil.FifeMiner)) return leftnugget;
        if (uuid.equals(ShadyUtil.lag_add)) return rightnugget;
        if (uuid.equals(ShadyUtil.Tankish)) return tankish;
        if (uuid.equals(ShadyUtil.FrizzleFrazzle)) return frizzlefrazzle;
        // 1.7: if(uuid.equals(ShadyUtil.Barnaby99_x)) return pheo; (commented out upstream)
        if (uuid.equals(ShadyUtil.Ma118)) return vaer;
        if (uuid.equals(ShadyUtil.Adam29Adam29)) return adam;
        if (uuid.equals(ShadyUtil.ege444)) return jame;
        if (uuid.equals(ShadyUtil.Golem)) return golem;
        if (ShadyUtil.contributors.contains(uuid)) return wiki;
        if (name.startsWith("Player") || name.equals("Dev")) return test;

        return null;
    }
}

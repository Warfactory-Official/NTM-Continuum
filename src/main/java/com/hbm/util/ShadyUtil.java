package com.hbm.util;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/**
 * 1.7 {@code ShadyUtil}: the UUIDs of the players who get an accessory or a special effect. 1.7's names and values,
 * plus the four 1.12.2 added (Golem, movblock, Hacker6329, EOS). The capes are in
 * {@code RenderAccessoryUtility}, the effects in {@link com.hbm.handler.SpecialPlayers}.
 *
 * <p>The sign hashes and {@code smoosh} also retain the hidden Bobmazon catalogue's authored acquisition.
 * Bob's {@code !gv} debug chat command and Alcater's effects are not included.
 */
public final class ShadyUtil {

    // 1.7 ShadyUtil:54-56
    public static final Set<String> HASHES = new java.util.HashSet<>(Set.of(
            "41de5c372b0589bbdb80571e87efa95ea9e34b0d74c6005b8eab495b7afd9994",
            "31da6223a100ed348ceb3254ceab67c9cc102cb2a04ac24de0df3ef3479b1036"));

    private ShadyUtil() {}

    public static final String HbMinecraft = "192af5d7-ed0f-48d8-bd89-9d41af8524f8";
    public static final String LPkukin = "937c9804-e11f-4ad2-a5b1-42e62ac73077";
    public static final String Dafnik = "3af1c262-61c0-4b12-a4cb-424cc3a9c8c0";
    public static final String a20 = "4729b498-a81c-42fd-8acd-20d6d9f759e0";
    public static final String LordVertice = "a41df45e-13d8-4677-9398-090d3882b74f";
    public static final String Dr_Nostalgia = "e82684a7-30f1-44d2-ab37-41b342be1bbd";
    public static final String Samino2 = "87c3960a-4332-46a0-a929-ef2a488d1cda";
    public static final String Hoboy03new = "d7f29d9c-5103-4f6f-88e1-2632ff95973f";
    public static final String Dragon59MC = "dc23a304-0f84-4e2d-b47d-84c8d3bfbcdb";
    public static final String Steelcourage = "ac49720b-4a9a-4459-a26f-bee92160287a";
    public static final String ZippySqrl = "03c20435-a229-489a-a1a1-671b803f7017";
    public static final String Schrabby = "3a4a1944-5154-4e67-b80a-b6561e8630b7";
    public static final String SweatySwiggs = "5544aa30-b305-4362-b2c1-67349bb499d5";
    public static final String Drillgon = "41ebd03f-7a12-42f3-b037-0caa4d6f235b";
    public static final String Doctor17 = "e4ab1199-1c22-4f82-a516-c3238bc2d0d1";
    public static final String Doctor17PH = "4d0477d7-58da-41a9-a945-e93df8601c5a";
    public static final String ShimmeringBlaze = "061bc566-ec74-4307-9614-ac3a70d2ef38";
    public static final String FifeMiner = "37e5eb63-b9a2-4735-9007-1c77d703daa3";
    public static final String lag_add = "259785a0-20e9-4c63-9286-ac2f93ff528f";
    public static final String Pu_238 = "c95fdfd3-bea7-4255-a44b-d21bc3df95e3";
    public static final String Tankish = "609268ad-5b34-49c2-abba-a9d83229af03";
    public static final String FrizzleFrazzle = "fc4cc2ee-12e8-4097-b26a-1c6cb1b96531";
    public static final String the_NCR = "28ae585f-4431-4491-9ce8-3def6126e3c6";
    public static final String Barnaby99_x = "b04cf173-cff0-4acd-aa19-3d835224b43d";
    public static final String Ma118 = "1121cb7a-8773-491f-8e2b-221290c93d81";
    public static final String Adam29Adam29 = "bbae7bfa-0eba-40ac-a0dd-f3b715e73e61";
    public static final String ege444 = "42ee978c-442a-4cd8-95b6-29e469b6df10";
    public static final String LePeeperSauvage = "433c2bb7-018c-4d51-acfe-27f907432b5e";
    // 1.12.2 additions
    public static final String Golem = "058b52a6-05b7-4d11-8cfa-2db665d9a521";
    public static final String movblock = "d45aac48-013d-4efd-aa4d-bc40d6b6026d";
    public static final String Hacker6329 = "a53a3807-cd85-4833-b791-0c27705bb6fd";
    public static final String EOS = "cac1f285-001c-47dc-94a3-44052fb7e4e8";
    public static final String MrNorwood = "ab2e13f0-0198-411a-bddb-00382418d594"; // HyperboreanA10
    public static final String SilentYeti = "5742ae80-133d-4089-94da-40e0b38d6e55"; // SilentYeti69

    public static final Set<String> contributors = Set.of(
            "06ab7c03-55ce-43f8-9d3c-2850e3c652de", // mustang_rudolf
            "5bf069bc-5b46-4179-aafe-35c0a07dee8b", // JMF781
            "ccd9aa1c-26b9-4dde-8f37-b96f8d99de22"  // kakseao
    );

    /** 1.7's usual test: the UUID, or (for most of its checks) the player name. */
    public static boolean is(Entity entity, String uuid, String... names) {
        if (entity == null) return false;
        if (is(entity.getUUID(), uuid)) return true;
        String name = entity.getName().getString();
        for (String candidate : names) if (candidate.equals(name)) return true;
        return false;
    }

    public static boolean is(UUID id, String uuid) {
        return id != null && id.toString().equals(uuid);
    }

    /**
     * 1.7 charset was the platform default; UTF-8 is pinned so the digest does not move with the JVM.
     * Only {@code b[0]} of each line feeds a seed, so any single-byte-compatible line agrees with 1.7.
     */
    public static String smoosh(String s1, String s2, String s3, String s4) {

        var rand = new java.util.Random();
        StringBuilder s = new StringBuilder();

        byte[] b1 = s1.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b2 = s2.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b3 = s3.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b4 = s4.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        if (b1.length == 0 || b2.length == 0 || b3.length == 0 || b4.length == 0) return "";

        s.append(s1);
        rand.setSeed(b1[0]);
        s.append(rand.nextInt(0xffffff));
        s.append(s2);
        rand.setSeed(rand.nextInt(0xffffff) + b2[0]);
        s.append(rand.nextInt(0xffffff));
        s.append(s3);
        rand.setSeed(rand.nextInt(0xffffff) + b3[0]);
        s.append(rand.nextInt(0xffffff));
        s.append(s4);
        rand.setSeed(rand.nextInt(0xffffff) + b4[0]);
        s.append(rand.nextInt(0xffffff));
        return getHash(s.toString());
    }

    public static String getHash(String input) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder str = new StringBuilder(bytes.length * 2);
            for (int b : bytes) str.append(Integer.toString((b & 0xFF) + 256, 16).substring(1));
            return str.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

package com.hbm.config;

/**
 * Ore-worldgen rates, ported from 1.12.2's {@code CompatibilityConfig} (plain constants, like
 * {@link GeneralConfig}/{@link BombConfig} — the port has no file-backed config system).
 *
 * <p><b>Why CompatibilityConfig and not the old WorldConfig:</b> upstream had both, and the old
 * {@code WorldConfig} was mostly a dead mirror — {@code HbmWorldGen} read
 * {@code CompatibilityConfig.<x>Spawn.get(dimID)} directly for almost every vein. The values below are
 * therefore the <b>CompatibilityConfig defaults at dimension 0</b> (or −1 / 1 for the nether and end
 * sets), which is what the 1.12.2 mod actually generated with. The handful of fields upstream really
 * did read off {@code WorldConfig} ({@code limestoneSpawn}, the cave/layer switches, the bedrock-ore
 * weights) are marked as such.
 *
 * <p>Two flavours of number, matching upstream and easy to mix up:
 * <ul>
 *   <li><b>counts</b> — vein attempts <i>per chunk</i> (bigger = more ore).</li>
 *   <li><b>chances</b> — the {@code n} in {@code rand.nextInt(n) == 0}, i.e. one per <i>n</i> chunks
 *       (bigger = <i>less</i> ore). Every such field is named {@code …Chance}.</li>
 * </ul>
 *
 * <p>Upstream's per-dimension maps also carried entries for dim −6 (a Galacticraft-era planet) and the
 * −28..−31 planet ores; those dimensions do not exist here, so only the overworld/nether/end values are
 * ported. Australium keeps its own hardcoded overworld pocket (see the feature class).
 */
public final class WorldConfig {

    private WorldConfig() {}

    // ---------------------------------------------------------------- meteors (1.12.2 CATEGORY_METEOR)
    /** {@code 5.00_enableMeteorStrikes}: toggles the spawning of meteors. */
    public static boolean enableMeteorStrikes = true;
    /** {@code 5.01_enableMeteorShowers}: meteor showers, a 1% chance for every spawned meteor. */
    public static boolean enableMeteorShowers = true;
    /** {@code 5.03_enableSpecialMeteors}: rare, special meteor types with different impact effects. */
    public static boolean enableSpecialMeteors = true;
    /** {@code 5.02_enableMeteorTails}: the particle effect created by falling meteors. */
    public static boolean enableMeteorTails = true;
    /** {@code 5.03_meteorStrikeChance}: one meteor every n ticks on average. */
    public static int meteorStrikeChance = 20 * 60 * 60 * 5;
    /** 1.7 {@code 5.04_meteorShowerChance}: one meteor every n ticks on average while a shower lasts. */
    public static int meteorShowerChance = 20 * 60 * 15;
    /** 1.7 {@code 5.05_meteorShowerDuration}: the longest a shower lasts, in ticks (it runs 75..100 % of this). */
    public static int meteorShowerDuration = 20 * 60 * 30;

    // ---------------------------------------------------------------- overworld veins (count/chunk)
    // Field names mirror upstream's CompatibilityConfig keys (thoriumSpawn = 01.11_thoriumSpawnrate);
    // only the ones that do something extra carry a note.

    // 2026-09-25 (§19ec-worldfeatures): original-hbm (1.7) wins where 1.7 and 1.12 disagree. The values below are
    // 1.7's config defaults (WorldConfig.loadFromConfig, what a 1.7 game generated with); the key named on a field
    // is the 1.7 key. Differences from the 1.12 CompatibilityConfig values this class used to carry are noted.

    /** Vein attempts per chunk, plain stone host. 1.7 {@code 2.00_uraniumSpawnrate}. */
    public static int uraniumSpawn = 7;
    public static int thoriumSpawn = 7;
    public static int titaniumSpawn = 8;
    public static int sulfurSpawn = 5;
    public static int aluminiumSpawn = 7;
    public static int copperSpawn = 12;
    public static int fluoriteSpawn = 6;
    public static int niterSpawn = 6;
    public static int tungstenSpawn = 10;
    public static int leadSpawn = 6;
    public static int berylliumSpawn = 6;
    /** 1.7 {@code 2.14_rareEarthSpawnRate}. Also the count of the gneiss rare-earth veins ({@code ore_gneiss_rare}). */
    public static int rareSpawn = 6;
    public static int ligniteSpawn = 2;
    /** 1.7 {@code 2.12_asbestosSpawnRate}. Multiplied by 3 for the gneiss variant. */
    public static int asbestosSpawn = 2;
    public static int cinnabarSpawn = 1;
    public static int cobaltSpawn = 2;
    /** Old {@code 01.14_lithiumSpawnRate}. Gneiss host, NOT multiplied by 3. */
    public static int lithiumSpawn = 6;
    /** Old {@code 01.17_gasShaleSpawnRate}. Gneiss host, multiplied by 3. */
    public static int gasShaleSpawn = 5;

    // ---------------------------------------------------------------- clusters (count/chunk)

    public static int ironClusterSpawn = 4;
    public static int titaniumClusterSpawn = 2;
    public static int aluminiumClusterSpawn = 3;
    /**
     * Old {@code 01.24_copperClusterSpawn}. Note upstream's own two config classes disagreed here
     * (CompatibilityConfig 3, WorldConfig 4); CompatibilityConfig is what generation actually read.
     */
    public static int copperClusterSpawn = 4; // 1.7 2.C03_copperClusterSpawn (1.12 CompatibilityConfig had 3)

    // ---------------------------------------------------------------- chance-gated (1 per N chunks)

    /** One flammable-gas bubble per N chunks. 1.7 {@code 2.17_gasBubbleSpawnRate} (1.12 had 40). */
    public static int gasBubbleChance = 12;
    /** One explosive-gas bubble per N chunks; 0 = never. 1.7 {@code 2.19_explosiveBubbleSpawnRate} (1.12 had 80). */
    public static int explosiveBubbleChance = 0;
    /** One alexandrite vein per N chunks. Old {@code 01.32_alexandriteSpawnRate}. */
    public static int alexandriteChance = 100;

    // ---------------------------------------------------------------- resource stone (WorldConfig-read)

    /**
     * Limestone vein attempts per chunk, 1.7 {@code 2.L02_limestoneSpawn}: overworld only, size 16, y 25..54. (1.12
     * read it in every dimension with size 6 at y 15..34; 1.7 wins.)
     */
    public static int limestoneSpawn = 1;

    // ---------------------------------------------------------------- 1.7 terrain + surface features

    /** 1.7 {@code 2.21_oilSpawnRate}: one oil bubble origin per N chunks (MapGenBubble). */
    public static int oilSpawn = 100;
    /** 1.7 {@code 2.22_bedrockOilSpawnRate}: one bedrock oil origin per N chunks (MapGenBedrockOil). */
    public static int bedrockOilSpawn = 200;
    /**
     * 1.7 {@code 2.23_meteoriteSpawnRate}: one fallen meteorite per N chunks. The config default is 200; the field
     * initialiser in 1.7's class said 500, but every 1.7 game read the config, so 200 is what generated.
     */
    public static int meteoriteSpawn = 200;
    /** 1.7 {@code 4.15_radHotspotSpawn}: one Sellafield crater origin per N chunks (desert only). */
    public static int radfreq = 5000;
    /** 1.7 {@code 4.18_geyserChlorineSpawn}: one chlorine geyser per N plains chunks. */
    public static int geyserChlorine = 3000;

    /** Old {@code WorldConfig.enableSulfurCave} — the sulfur cave-wall Perlin feature. */
    public static boolean enableSulfurCave = true;
    public static boolean enableAsbestosCave = true;
    /** Old {@code WorldConfig.enableHematite} — the 3D-noise ore layer. */
    public static boolean enableHematite = true;
    public static boolean enableMalachite = true;
    public static boolean enableBauxite = true;

    // ---------------------------------------------------------------- crater fallout biomes

    /** Whether fallout sweeps may replace overworld biome columns with the crater biomes. */
    public static boolean enableCraterBiomes = true;

    // ---------------------------------------------------------------- nether (count/chunk)

    public static int netherUraniumSpawn = 8;
    public static int netherTungstenSpawn = 10;
    public static int netherSulfurSpawn = 26;
    /** Old {@code 02.N03_phosphorusSpawnrate} — places {@code ore_nether_fire}. */
    public static int netherPhosphorusSpawn = 24;
    public static int netherCoalSpawn = 8; // 1.7 2.N04_coalSpawnrate (1.12 had 24)
    /** Old {@code 02.N06_cobaltSpawnrate}. Generates near the nether ROOF — see the feature. */
    public static int netherCobaltSpawn = 2;
    /** Old {@code 02.N05_plutoniumSpawnrate}. Gated off by {@link GeneralConfig#enablePlutoniumOre}. */
    public static int netherPlutoniumSpawn = 8;

    // ---------------------------------------------------------------- end

    /**
     * Old {@code 03.E01_tixiteSpawnrate}. Note the spelling drift upstream: the config key says
     * "tixite", the old WorldConfig mirror says "tikite", and the block is {@code ore_tikite}.
     */
    public static int endTikiteSpawn = 8;
}

package com.hbm.world.feature;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.items.ModFuelItems;
import com.hbm.world.gen.ConfigInt;
import com.hbm.world.gen.WorldgenKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * {@code hbm:bedrock_ore} / {@code hbm:bedrock_ore_nether}: 1.7's bedrock-ore deposit for the chunk
 * ({@code BedrockOre.generateAuto} in the overworld, the weighted {@code weightedOresNether} pick in the nether).
 * 1.7's {@code rand.nextInt(10) == 0} gate is the placed feature's {@code rarity_filter}; the deposit then picks its
 * own column {@code chunk + 8 + rand(2)} exactly as 1.7 did, so the placement adds no {@code in_square}. The
 * {@code depth_rock} cap replaces bedrock and {@code host} blocks above the deposit.
 */
public final class BedrockDepositFeature {

    private BedrockDepositFeature() {}

    private static RuleTest baseStone() {
        return FeatureCodecs.tag(BlockTags.BASE_STONE_OVERWORLD);
    }

    /** The overworld deposit: its resource, tier and acid come from the world's ore field; {@code color} tints it. */
    public static class Overworld extends Feature<Overworld.Config> {

        public record Config(Block depthRock, RuleTest host, int color) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    lazy(BuiltInRegistries.BLOCK.byNameCodec(), "depth_rock", () -> ModOreBlocks.stone_depth.get()).forGetter(Config::depthRock),
                    FeatureCodecs.rule("host", BedrockDepositFeature::baseStone).forGetter(Config::host),
                    field(Codec.INT, "color", 0xD78A16).forGetter(Config::color)
            ).apply(i, Config::new));
        }

        public Overworld() {
            super(Config.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<Config> context) {
            Config c = context.config();
            int cx = context.origin().getX() & ~15, cz = context.origin().getZ() & ~15;
            BedrockOreFeature.generateAutoChunk(context.level(), context.random(), cx, cz, c.depthRock(), c.host(), c.color());
            return true;
        }
    }

    /** The nether deposit: one weighted entry per chunk, picked from the seed and chunk position. */
    public static class Nether extends Feature<Nether.Config> {

        /** {@code weight} may name an {@code hbm.cfg} key; 1.7's three read {@code 2.BN0x_bedrock*Weight}. */
        public record Entry(Item item, int count, int color, int tier, ConfigInt weight) {
            public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                    field(Codec.INT, "count", 1).forGetter(Entry::count),
                    field(Codec.INT, "color", 0xFFFFFF).forGetter(Entry::color),
                    field(Codec.INT, "tier", 1).forGetter(Entry::tier),
                    ConfigInt.CODEC.fieldOf("weight").forGetter(Entry::weight)
            ).apply(i, Entry::new));
        }

        public record Config(Block depthRock, RuleTest host, List<Entry> entries) implements FeatureConfiguration {
            public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                    lazy(BuiltInRegistries.BLOCK.byNameCodec(), "depth_rock", () -> ModOreBlocks.stone_depth_nether.get()).forGetter(Config::depthRock),
                    FeatureCodecs.rule("host", BedrockDepositFeature::baseStone).forGetter(Config::host),
                    lazy(Entry.CODEC.listOf(), "entries", Config::entries17).forGetter(Config::entries)
            ).apply(i, Config::new));

            /** 1.7 {@code BedrockOre.weightedOresNether}: glowstone, phosphorus, quartz, 4 each. */
            static List<Entry> entries17() {
                return List.of(
                        new Entry(Items.GLOWSTONE_DUST, 4, 0xF9FF4D, 1, ConfigInt.key(WorldgenKeys.BEDROCK_GLOWSTONE)),
                        new Entry(ModFuelItems.powder_fire.get(), 4, 0xD7341F, 1, ConfigInt.key(WorldgenKeys.BEDROCK_PHOSPHORUS)),
                        new Entry(Items.QUARTZ, 4, 0xF0EFDD, 1, ConfigInt.key(WorldgenKeys.BEDROCK_QUARTZ)));
            }
        }

        public Nether() {
            super(Config.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<Config> context) {
            Config c = context.config();
            int cx = context.origin().getX() & ~15, cz = context.origin().getZ() & ~15;
            BedrockOreFeature.generateNetherChunk(context.level(), context.random(), cx, cz, c.depthRock(), c.host(), c.entries());
            return true;
        }
    }
}

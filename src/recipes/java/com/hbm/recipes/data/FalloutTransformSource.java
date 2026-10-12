package com.hbm.recipes.data;

import com.google.gson.JsonElement;
import com.hbm.config.FalloutConfig;
import com.hbm.config.FalloutConfig.FalloutEntry;
import com.hbm.config.FalloutConfig.TransformFile;
import com.hbm.util.SavedIds;
import dev.golem.core.Ids;
import dev.golem.core.world.DataSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code FalloutConfigJSON.initDefault}: HBM's fallout transforms, {@code data/hbm/fallout_transform/default.json}
 * at priority 0, entry by entry in 1.7's order. 1.7's {@code mB(block).mM(meta)} became the modern block or state,
 * {@code mMa(Material)} a named preset ({@code hbm:ignited_by_lava} wood, {@code hbm:vegetation} plants and vines,
 * {@code hbm:solid_terrain} iron/rock/sand/ground, {@code hbm:grass_like} grass), and {@code Triplet(block, meta, weight)}
 * a state and its weight (the sellafite meta is its {@code shade}).
 */
public final class FalloutTransformSource extends DataSource<TransformFile> {

    public FalloutTransformSource() {
        super(FalloutConfig.FOLDER);
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Ids.parse(id));
    }

    private static TagKey<Block> tag(String id) {
        return TagKey.create(Registries.BLOCK, Ids.parse(id));
    }

    private static BlockState state(String name) {
        BlockState state = SavedIds.stateFromName(name, null);
        if (state == null) throw new IllegalStateException("fallout_transform: unknown block state " + name);
        return state;
    }

    /** A sellafite block at shade {@code m} (1.7's metadata). */
    private static BlockState sellafield(String id, int m) {
        return state(id + "[shade=" + m + "]");
    }

    @Override
    protected void registerDefaults() {
        List<FalloutEntry> entries = new ArrayList<>();
        BlockState air = state("minecraft:air");

        double woodEffectRange = 65D;

        /* petrify all wooden things possible */
        // 1.7 Blocks.log / log2: the logs tag. 1.7 .prim(new Triplet(ModBlocks.waste_log, 0, 1)), no .sol: its waste_log
        // had no axis (always drawn upright), so every log becomes the upright one
        entries.add(FalloutEntry.builder().matchesTag(tag("minecraft:logs")).addPrimary(state("hbm:waste_log[axis=y]"), 1)
                .max(woodEffectRange));
        // 1.7 red/brown_mushroom_block meta 10 (the stem)
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:mushroom_stem")).addPrimary(state("hbm:waste_log[axis=y]"), 1)
                .max(woodEffectRange));
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:red_mushroom_block")).addPrimary(air, 1).max(woodEffectRange));
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:brown_mushroom_block")).addPrimary(air, 1).max(woodEffectRange));
        // 1.7 Blocks.snow_layer only (no entry for the snow block, Blocks.snow)
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:snow")).addPrimary(air, 1).max(woodEffectRange));
        // 1.7 .mB(Blocks.planks).prim(new Triplet(ModBlocks.waste_planks, 0, 1)).max(woodEffectRange), no .sol
        entries.add(FalloutEntry.builder().matchesTag(tag("minecraft:planks")).addPrimary(state("hbm:waste_planks"), 1)
                .max(woodEffectRange));
        /* if it can't be petrified, destroy it */
        entries.add(FalloutEntry.builder().matchesPreset("hbm:ignited_by_lava").addPrimary(air, 1).max(woodEffectRange));
        /* destroy all leaves within the radios, kill all leaves outside of it */
        entries.add(FalloutEntry.builder().matchesTag(tag("minecraft:leaves")).addPrimary(air, 1).max(woodEffectRange));
        // 1.7 Material.plants and Material.vine: one preset
        entries.add(FalloutEntry.builder().matchesPreset("hbm:vegetation").addPrimary(air, 1).max(woodEffectRange));
        entries.add(FalloutEntry.builder().matchesBlock(block("hbm:waste_leaves")).addPrimary(air, 1).max(woodEffectRange));
        // 1.7 Blocks.leaves / leaves2: the leaves tag
        entries.add(FalloutEntry.builder().matchesTag(tag("minecraft:leaves")).addPrimary(state("hbm:waste_leaves"), 1)
                .min(woodEffectRange - 5D));

        entries.add(FalloutEntry.builder().matchesBlock(block("hbm:glyphid_base")).addPrimary(state("hbm:glyphid_base[type=rad]"), 1));
        entries.add(FalloutEntry.builder().matchesBlock(block("hbm:glyphid_spawner")).addPrimary(state("hbm:glyphid_spawner[type=rad]"), 1));

        // 1.7 new FalloutEntry().mB(Blocks.mossy_cobblestone).prim(new Triplet(Blocks.coal_ore, 0, 1)), no .sol
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:mossy_cobblestone")).addPrimary(state("minecraft:coal_ore"), 1));
        // 1.7 .mB(ModBlocks.ore_nether_uranium).prim(new Triplet(ModBlocks.ore_nether_schrabidium, 0, 1),
        // new Triplet(ModBlocks.ore_nether_uranium_scorched, 0, 99))
        entries.add(FalloutEntry.builder().matchesBlock(block("hbm:ore_nether_uranium"))
                .addPrimary(state("hbm:ore_nether_schrabidium"), 1).addPrimary(state("hbm:ore_nether_uranium_scorched"), 99));

        // 1.7's ring entries set no mO (opaque) flag. The deepslate coal and diamond ores (no 1.7 blocks) ring like their
        // stone ores
        for (int i = 1; i <= 10; i++) {
            int m = 10 - i;
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_diamond", m), 3).addPrimary(sellafield("hbm:ore_sellafield_emerald", m), 2)
                    .primaryChance(0.5).max(i * 5).solid(true).matchesBlock(block("minecraft:coal_ore")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_diamond", m), 3).addPrimary(sellafield("hbm:ore_sellafield_emerald", m), 2)
                    .primaryChance(0.5).max(i * 5).solid(true).matchesBlock(block("minecraft:deepslate_coal_ore")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_diamond", m), 1)
                    .primaryChance(0.2).max(i * 5).solid(true).matchesBlock(block("hbm:ore_lignite")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_emerald", m), 1)
                    .max(i * 5).solid(true).matchesBlock(block("hbm:ore_beryllium")));
            if (m > 4) entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_schrabidium", m), 1).addPrimary(sellafield("hbm:ore_sellafield_uranium_scorched", m), 9)
                    .max(i * 5).solid(true).matchesBlock(block("hbm:ore_uranium")));
            if (m > 4) entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_schrabidium", m), 1).addPrimary(sellafield("hbm:ore_sellafield_uranium_scorched", m), 9)
                    .max(i * 5).solid(true).matchesBlock(block("hbm:ore_gneiss_uranium")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_radgem", m), 1)
                    .max(i * 5).solid(true).matchesBlock(block("minecraft:diamond_ore")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:ore_sellafield_radgem", m), 1)
                    .max(i * 5).solid(true).matchesBlock(block("minecraft:deepslate_diamond_ore")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:sellafield_bedrock", m), 1).max(i * 5).solid(true).matchesBlock(block("minecraft:bedrock")));
            // 1.7 ModBlocks.ore_bedrock
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:sellafield_bedrock", m), 1).max(i * 5).solid(true).matchesBlock(block("hbm:ore_bedrock_block")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:sellafield_bedrock", m), 1).max(i * 5).solid(true).matchesBlock(block("hbm:ore_bedrock_oil")));
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:sellafield_bedrock", m), 1).max(i * 5).solid(true).matchesBlock(block("hbm:sellafield_bedrock")));
            // 1.7 Material.iron / rock / sand / ground: one preset
            entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:sellafield_slaked", m), 1).max(i * 5).solid(true).matchesPreset("hbm:solid_terrain"));
            // 1.7 Material.grass
            if (i <= 9) entries.add(FalloutEntry.builder().addPrimary(sellafield("hbm:sellafield_slaked", m), 1).max(i * 5).solid(true).matchesPreset("hbm:grass_like"));
            // 1.7 Et Futurum deepslate and stone (Compat.MOD_EF): vanilla's, under hbm:solid_terrain
        }

        // 1.7 sets no .sol on the last four entries
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:mycelium")).addPrimary(state("hbm:waste_mycelium"), 1));
        entries.add(FalloutEntry.builder().matchesState(state("minecraft:sand")).addPrimary(state("hbm:waste_trinitite"), 1)
                .primaryChance(0.05));
        // 1.7 Blocks.sand meta 1
        entries.add(FalloutEntry.builder().matchesState(state("minecraft:red_sand")).addPrimary(state("hbm:waste_trinitite_red"), 1)
                .primaryChance(0.05));
        // 1.7 Blocks.hardened_clay
        entries.add(FalloutEntry.builder().matchesBlock(block("minecraft:clay")).addPrimary(state("minecraft:terracotta"), 1));

        register("default", new TransformFile(0, entries));
    }

    @Override
    public JsonElement encode(TransformFile file, HolderLookup.Provider registries) {
        return FalloutConfig.write(file);
    }

    @Override
    public TransformFile decode(JsonElement json, HolderLookup.Provider registries) {
        return FalloutConfig.read(json);
    }
}

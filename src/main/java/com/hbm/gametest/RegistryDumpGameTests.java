package com.hbm.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Writes every {@code hbm:} registration this node made to {@code hbm-registry-dump.json} in the run
 * directory, as the before/after evidence for registration refactors (the 2026-10-01 {@code Reg}
 * migration): ids per registry in registration order, the creative-tab contents in display order, and
 * a fingerprint of each block, item, block-entity type and entity type built from properties the
 * registration chain sets. Two dumps that compare equal mean the registration code was rewritten
 * without changing what it registers. Never fails, so it is a tool rather than a test: it is declared only
 * when the GameTest filter names it, {@code -Pgametest.filter=.*registry_dump.*} (see {@link #requested()}),
 * and a full run neither writes the dump nor counts it as a pass.
 */
public final class RegistryDumpGameTests {

    private static final String PREFIX = Constants.MOD_ID + ":";

    private RegistryDumpGameTests() {}

    /**
     * True when this run's GameTest filter ({@code golem.gametest.filter}, fed by {@code -Pgametest.filter})
     * explicitly names {@code registry_dump}. A blank filter (the full suite) or any other filter leaves the
     * dump undeclared, so required-test totals count only real assertions.
     */
    static boolean requested() {
        String filter = System.getProperty("golem.gametest.filter");
        return filter != null && filter.contains("registry_dump");
    }

    public static void dump(GameTestHelper helper) {
        JsonObject root = new JsonObject();

        JsonObject registries = new JsonObject();
        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            JsonArray ids = ids(registry);
            if (!ids.isEmpty()) registries.add(registryId(registry), ids);
        }
        root.add("registries", registries);

        JsonArray components = new JsonArray();
        for (String key : new TreeSet<>(GolemComponent.declaredKeys())) {
            if (key.startsWith(PREFIX)) components.add(key);
        }
        root.add("golem_components", components);

        root.add("creative_tabs", tabs());
        root.add("blocks", blocks(helper));
        root.add("items", items());
        root.add("block_entity_types", blockEntityTypes());
        root.add("entity_types", entityTypes());

        Path out = Path.of(System.getProperty("hbm.registryDump", "hbm-registry-dump.json")).toAbsolutePath();
        try {
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root),
                    StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Constants.LOG.info("[REGDUMP] wrote {} registries to {}", registries.size(), out);
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String registryId(Registry<?> registry) {
        return String.valueOf(((Registry) BuiltInRegistries.REGISTRY).getKey(registry));
    }

    private static <T> JsonArray ids(Registry<T> registry) {
        JsonArray ids = new JsonArray();
        for (T value : registry) {
            String id = String.valueOf(registry.getKey(value));
            if (id.startsWith(PREFIX)) ids.add(id);
        }
        return ids;
    }

    private static <T> List<T> owned(Registry<T> registry) {
        List<T> values = new ArrayList<>();
        for (T value : registry) {
            if (String.valueOf(registry.getKey(value)).startsWith(PREFIX)) values.add(value);
        }
        return values;
    }

    private static JsonObject tabs() {
        JsonObject tabs = new JsonObject();
        for (Map.Entry<?, ? extends List<? extends Supplier<? extends Item>>> tab : ModTabs.CONTENTS.entrySet()) {
            JsonObject entry = new JsonObject();
            JsonArray items = new JsonArray();
            for (Supplier<? extends Item> item : tab.getValue()) {
                items.add(String.valueOf(BuiltInRegistries.ITEM.getKey(item.get())));
            }
            entry.add("items", items);
            JsonArray stacks = new JsonArray();
            List<Supplier<List<ItemStack>>> extra = ModTabs.STACK_CONTENTS.get(tab.getKey());
            if (extra != null) {
                for (Supplier<List<ItemStack>> batch : extra) {
                    try {
                        for (ItemStack stack : batch.get()) {
                            stacks.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()
                                    + " " + stack.getHoverName().getString());
                        }
                    } catch (RuntimeException failure) {
                        stacks.add("<error " + failure + ">");
                    }
                }
            }
            entry.add("stacks", stacks);
            tabs.add(String.valueOf(tab.getKey()), entry);
        }
        return tabs;
    }

    private static JsonObject blocks(GameTestHelper helper) {
        JsonObject blocks = new JsonObject();
        for (Block block : owned(BuiltInRegistries.BLOCK)) {
            BlockState state = block.defaultBlockState();
            JsonObject entry = new JsonObject();
            entry.addProperty("class", block.getClass().getName());
            entry.addProperty("default_state", state.toString());
            entry.addProperty("description", block.getDescriptionId());
            entry.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(block.asItem())));
            float hardness;
            try {
                hardness = state.getDestroySpeed(helper.getLevel(), BlockPos.ZERO);
            } catch (RuntimeException failure) {
                hardness = Float.NaN;
            }
            entry.addProperty("hardness", hardness);
            entry.addProperty("resistance", block.getExplosionResistance());
            entry.addProperty("requires_tool", state.requiresCorrectToolForDrops());
            entry.addProperty("friction", block.getFriction());
            entry.addProperty("speed_factor", block.getSpeedFactor());
            entry.addProperty("jump_factor", block.getJumpFactor());
            entry.addProperty("occludes", state.canOcclude());
            entry.addProperty("light", state.getLightEmission());
            entry.addProperty("random_ticks", state.isRandomlyTicking());
            SoundType sound = state.getSoundType();
            entry.addProperty("sound", BuiltInRegistries.SOUND_EVENT.getKey(sound.getBreakSound()) + "/"
                    + BuiltInRegistries.SOUND_EVENT.getKey(sound.getStepSound()) + "/" + sound.getVolume()
                    + "/" + sound.getPitch());
            blocks.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(block)), entry);
        }
        return blocks;
    }

    private static JsonObject items() {
        JsonObject items = new JsonObject();
        for (Item item : owned(BuiltInRegistries.ITEM)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("class", item.getClass().getName());
            entry.addProperty("description", item.getDescriptionId());
            if (item instanceof BlockItem blockItem) {
                entry.addProperty("block", String.valueOf(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock())));
            }
            try {
                ItemStack stack = new ItemStack(item);
                entry.addProperty("max_stack", stack.getMaxStackSize());
                entry.addProperty("max_damage", stack.getMaxDamage());
                entry.addProperty("rarity", String.valueOf(stack.getRarity()));
            } catch (RuntimeException failure) {
                entry.addProperty("stack_error", failure.toString());
            }
            items.add(String.valueOf(BuiltInRegistries.ITEM.getKey(item)), entry);
        }
        return items;
    }

    private static JsonObject blockEntityTypes() {
        List<Block> blocks = owned(BuiltInRegistries.BLOCK);
        JsonObject types = new JsonObject();
        for (BlockEntityType<?> type : owned(BuiltInRegistries.BLOCK_ENTITY_TYPE)) {
            JsonObject entry = new JsonObject();
            JsonArray valid = new JsonArray();
            for (Block block : blocks) {
                if (type.isValid(block.defaultBlockState())) {
                    valid.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(block)));
                }
            }
            entry.add("valid_blocks", valid);
            entry.addProperty("ticks_client", BlockEntities.ticks(type, true));
            entry.addProperty("ticks_server", BlockEntities.ticks(type, false));
            types.add(String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)), entry);
        }
        return types;
    }

    private static JsonObject entityTypes() {
        JsonObject types = new JsonObject();
        for (EntityType<?> type : owned(BuiltInRegistries.ENTITY_TYPE)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("category", String.valueOf(type.getCategory()));
            entry.addProperty("width", type.getWidth());
            entry.addProperty("height", type.getHeight());
            entry.addProperty("tracking_range", type.clientTrackingRange());
            entry.addProperty("update_interval", type.updateInterval());
            entry.addProperty("fire_immune", type.fireImmune());
            entry.addProperty("summonable", type.canSummon());
            entry.addProperty("description", type.getDescriptionId());
            types.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type)), entry);
        }
        return types;
    }
}

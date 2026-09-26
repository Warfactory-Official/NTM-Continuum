package com.hbm.api.content;

import com.hbm.scripting.ScriptedContent;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.NTMEvents;
import com.hbm.Constants;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.radiation.RadiationBlocks;
import com.hbm.api.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.transformer.HazardTransformerCustom;
import com.hbm.api.hazard.HazardType;
import com.hbm.api.hazard.IHazardType;
import com.hbm.util.ArmorRegistry;
import com.hbm.api.hazard.HazardClass;
import com.hbm.util.DamageResistanceHandler;
import dev.golem.core.Ids;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code NTMEvents.hazards}. Added hazards stack on top of what an item already carries.
 *
 * <pre>
 * e.item('minecraft:rotten_flesh').add('radiation', 5).add('hot', 2)
 * e.tag('#c:ingots/gold').add('digamma', 0.01)
 * e.item('hbm:ingot_u238').clear().add('radiation', 1)    clear(): drop every entry first
 * e.multiplier('hbm:ingot_u235', 2)                       scales the entries the item carries
 * e.blacklist('hbm:ingot_u235')  e.blacklist('#c:ingots/uranium')
 * e.type('kubejs:itchy', t => t.name('Itchy').trait('RED')
 *     .onCarried((entity, stack, level) => ...).onDropped((itemEntity, level) => ...)
 *     .tooltip((player, lines, level, stack) => ...))
 * e.radResistant('minecraft:tinted_glass')  e.radResistant('#c:glass_blocks')  walls between radiation pockets
 * e.radResistant('hbm:reinforced_glass', false)                                 unseal a built-in one
 * e.block('minecraft:amethyst_block').radiation(50)   block or #tag, in the world: emits into its pocket up to 50
 * e.armor('minecraft:iron_helmet', a => a.radResistance(0.1).protects('PARTICLE_COARSE', 'GAS_LUNG')
 *     .damage('PHYS', 2, 0.1).damageExact('fall', 0, 0.5).damageOther(1, 0.05))
 * </pre>
 *
 * Built-in types: radiation contaminating digamma hot blinding asbestos coal hydroactive explosive toxic cold.
 * Sealing is binary: a pocket wall or not. Armour: {@code radResistance} per piece, worn pieces summed, dose
 * {@code ×10^-sum}; {@code protects} adds {@code HazardClass}es: GAS_LUNG GAS_MONOXIDE GAS_INERT PARTICLE_COARSE
 * PARTICLE_FINE BACTERIA NERVE_AGENT GAS_BLISTERING SAND LIGHT; {@code damage*} replace the item's own
 * threshold/resistance profile, categories EXPL FIRE PHYS EN.
 */
public final class HazardsEvent {

    private static final List<Runnable> UNDO = new ArrayList<>();

    private final List<Runnable> edits = new ArrayList<>();

    HazardsEvent() {}

    List<Runnable> edits() {
        return edits;
    }

    static synchronized void undoAll() {
        for (int i = UNDO.size() - 1; i >= 0; i--) UNDO.get(i).run();
        UNDO.clear();
    }

    private static synchronized void undo(Runnable undo) {
        UNDO.add(undo);
    }

    /** {@code item}'s hazards, replacing any earlier registration of it. */
    public HazardsEvent register(net.minecraft.world.level.ItemLike item, com.hbm.api.hazard.HazardSpec spec) {
        edits.add(() -> {
            var resolved = item.asItem();
            var previous = HazardSystem.replace(resolved, data(spec));
            undo(() -> HazardSystem.replace(resolved, previous));
        });
        return this;
    }

    /** Every item in {@code tag}; an item's own registration wins unless the spec overrides. */
    public HazardsEvent register(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag, com.hbm.api.hazard.HazardSpec spec) {
        edits.add(() -> {
            var previous = HazardSystem.replace(tag, data(spec));
            undo(() -> HazardSystem.replace(tag, previous));
        });
        return this;
    }

    public HazardsEvent blacklistItem(net.minecraft.world.level.ItemLike item) {
        edits.add(() -> {
            var resolved = item.asItem();
            HazardSystem.blacklist(resolved);
            undo(() -> HazardSystem.unblacklist(resolved));
        });
        return this;
    }

    private static com.hbm.hazard.HazardData data(com.hbm.api.hazard.HazardSpec spec) {
        var data = new com.hbm.hazard.HazardData();
        spec.entries().forEach(data::addEntry);
        return data.setOverride(spec.overrides());
    }

    public Target item(String id) {
        Target target = new Target();
        edits.add(() -> {
            var item = ScriptSpecs.item(id);
            HazardTransformerCustom.onItem(item, target);
            undo(() -> HazardTransformerCustom.removeItem(item, target));
        });
        return target;
    }

    public Target tag(String id) {
        Target target = new Target();
        edits.add(() -> {
            var tag = ScriptSpecs.tag(id);
            HazardTransformerCustom.onTag(tag, target);
            undo(() -> HazardTransformerCustom.removeTag(tag, target));
        });
        return target;
    }

    public HazardsEvent multiplier(String item, double multiplier) {
        edits.add(() -> {
            var resolved = ScriptSpecs.item(item);
            HazardTransformerCustom.setMultiplier(resolved, multiplier);
            undo(() -> HazardTransformerCustom.clearMultiplier(resolved));
        });
        return this;
    }

    public HazardsEvent blacklist(String itemOrTag) {
        edits.add(() -> {
            if (itemOrTag.startsWith("#")) {
                var tag = ScriptSpecs.tag(itemOrTag);
                HazardSystem.blacklist(tag);
                undo(() -> HazardSystem.unblacklist(tag));
            } else {
                var item = ScriptSpecs.item(itemOrTag);
                HazardSystem.blacklist(item);
                undo(() -> HazardSystem.unblacklist(item));
            }
        });
        return this;
    }

    public HazardsEvent type(String id, Consumer<TypeBuilder> spec) {
        var key = Ids.parse(id);
        if (HazardRegistry.getType(key) != null) {
            Constants.LOG.info("[scripts] hazard type {} exists; types apply at launch only", id);
            return this;
        }
        TypeBuilder builder = new TypeBuilder(HazardType.builder(key));
        spec.accept(builder);
        if (builder.name != null) {
            ScriptedContent.lang(Constants.MOD_ID.equals(key.getNamespace())
                    ? "trait." + key.getPath() : "trait." + key.getNamespace() + "." + key.getPath(), builder.name);
        }
        HazardRegistry.registerType(key, builder.builder.build());
        return this;
    }

    public HazardsEvent radResistant(String blockOrTag) {
        return radResistant(blockOrTag, true);
    }

    public HazardsEvent radResistant(String blockOrTag, boolean sealed) {
        edits.add(() -> {
            if (blockOrTag.startsWith("#")) {
                var tag = ScriptSpecs.blockTag(blockOrTag);
                Boolean previous = RadiationBlocks.seal(tag, sealed);
                undo(() -> RadiationBlocks.seal(tag, previous));
            } else {
                var block = ScriptSpecs.block(blockOrTag);
                Boolean previous = RadiationBlocks.seal(block, sealed);
                undo(() -> RadiationBlocks.seal(block, previous));
            }
        });
        return this;
    }

    public BlockTarget block(String blockOrTag) {
        BlockTarget target = new BlockTarget();
        edits.add(() -> {
            if (target.rads == null) return;
            if (blockOrTag.startsWith("#")) {
                var tag = ScriptSpecs.blockTag(blockOrTag);
                Double previous = RadiationBlocks.emit(tag, target.rads);
                undo(() -> RadiationBlocks.emit(tag, previous));
            } else {
                var block = ScriptSpecs.block(blockOrTag);
                Double previous = RadiationBlocks.emit(block, target.rads);
                undo(() -> RadiationBlocks.emit(block, previous));
            }
        });
        return target;
    }

    public HazardsEvent armor(String id, Consumer<ArmorTarget> spec) {
        ArmorTarget target = new ArmorTarget();
        spec.accept(target);
        edits.add(() -> {
            var item = ScriptSpecs.item(id);
            if (target.radResistance != null) {
                Double previous = HazmatRegistry.registered(item);
                HazmatRegistry.registerHazmat(item, target.radResistance);
                undo(() -> {
                    if (previous == null) HazmatRegistry.unregisterHazmat(item);
                    else HazmatRegistry.registerHazmat(item, previous);
                });
            }
            if (!target.protects.isEmpty()) {
                ArrayList<HazardClass> previous = ArmorRegistry.hazardClasses.get(item);
                ArrayList<HazardClass> classes = previous == null ? new ArrayList<>() : new ArrayList<>(previous);
                for (HazardClass hazard : target.protects) if (!classes.contains(hazard)) classes.add(hazard);
                ArmorRegistry.hazardClasses.put(item, classes);
                undo(() -> {
                    if (previous == null) ArmorRegistry.hazardClasses.remove(item);
                    else ArmorRegistry.hazardClasses.put(item, previous);
                });
            }
            if (target.damage != null) {
                var previous = DamageResistanceHandler.setItemStats(item, target.damage);
                undo(() -> DamageResistanceHandler.setItemStats(item, previous));
            }
        });
        return this;
    }

    public static IHazardType hazardType(String name) {
        var key = name.indexOf(':') >= 0 ? Ids.parse(name) : Ids.of(Constants.MOD_ID, name.toLowerCase(Locale.US));
        IHazardType type = HazardRegistry.getType(key);
        if (type == null) throw new IllegalArgumentException("Unknown hazard type " + name);
        return type;
    }

    public static final class Target implements BiConsumer<ItemStack, List<HazardEntry>> {

        private final List<HazardEntry> added = new ArrayList<>();
        private boolean clear;

        private Target() {}

        public Target add(String type, double level) {
            added.add(new HazardEntry(hazardType(type), level));
            return this;
        }

        public Target clear() {
            clear = true;
            return this;
        }

        @Override
        public void accept(ItemStack stack, List<HazardEntry> entries) {
            if (clear) entries.clear();
            entries.addAll(added);
        }
    }

    public static final class BlockTarget {

        private Double rads;

        private BlockTarget() {}

        public BlockTarget radiation(double rads) {
            this.rads = rads;
            return this;
        }
    }

    public static final class ArmorTarget {

        private Double radResistance;
        private final List<HazardClass> protects = new ArrayList<>();
        private DamageResistanceHandler.ResistanceStats damage;

        private ArmorTarget() {}

        public ArmorTarget radResistance(double resistance) {
            this.radResistance = resistance;
            return this;
        }

        public ArmorTarget protects(String... classes) {
            for (String name : classes) protects.add(ScriptSpecs.constant(HazardClass.class, name));
            return this;
        }

        public ArmorTarget damage(String category, float threshold, float resistance) {
            stats().addCategory(category, threshold, resistance);
            return this;
        }

        public ArmorTarget damageExact(String type, float threshold, float resistance) {
            stats().addExact(type, threshold, resistance);
            return this;
        }

        public ArmorTarget damageOther(float threshold, float resistance) {
            stats().setOther(threshold, resistance);
            return this;
        }

        private DamageResistanceHandler.ResistanceStats stats() {
            if (damage == null) damage = new DamageResistanceHandler.ResistanceStats();
            return damage;
        }
    }

    public static final class TypeBuilder {

        private final HazardType.Builder<Void> builder;
        private String name;

        private TypeBuilder(HazardType.Builder<Void> builder) {
            this.builder = builder;
        }

        public TypeBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TypeBuilder trait(String formatting) {
            builder.trait(ScriptSpecs.constant(ChatFormatting.class, formatting));
            return this;
        }

        public TypeBuilder onCarried(Carried carried) {
            builder.onCarried((target, stack, level, payload) -> carried.apply(target, stack, level));
            return this;
        }

        public TypeBuilder onDropped(Dropped dropped) {
            builder.onDropped((item, level, payload) -> dropped.apply(item, level));
            return this;
        }

        public TypeBuilder tooltip(Info info) {
            builder.tooltip((player, tooltip, level, stack, modifiers, payload) -> info.addInfo(player, tooltip, level, stack));
            return this;
        }
    }

    @FunctionalInterface
    public interface Carried {
        void apply(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, double level);
    }

    @FunctionalInterface
    public interface Dropped {
        void apply(net.minecraft.world.entity.item.ItemEntity item, double level);
    }

    @FunctionalInterface
    public interface Info {
        void addInfo(net.minecraft.world.entity.player.Player player, List<net.minecraft.network.chat.Component> lines,
                double level, ItemStack stack);
    }
}

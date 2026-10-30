package com.hbm.delegacy.legacy;

import com.hbm.delegacy.AtomicNbt;
import com.hbm.delegacy.ContinuumStacks;
import com.hbm.delegacy.de7.StackContext;
import com.hbm.delegacy.de7.Walker;
import com.mojang.logging.LogUtils;
import dev.golem.core.data.ItemContents;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Converts a DE stack that loaded after the import (another mod's storage, a quest reward) with the importer's own
 * stack rules. What has no Continuum home goes to {@code data/hbm_de_legacy_leftovers.dat}, never away.
 */
public final class LegacyStacks {
    public static final String LEFTOVERS = "data/hbm_de_legacy_leftovers.dat";
    private static final Logger LOGGER = LogUtils.getLogger();

    private LegacyStacks() {}

    /** True for a stack that still carries a DE id or a raw DE component, itself or in a stack it holds. */
    public static boolean isLegacy(ItemStack stack) {
        return own(stack) || stack.getComponentsPatch().entrySet().stream()
                .anyMatch(e -> e.getValue().map(LegacyStacks::held).orElse(Stream.empty()).anyMatch(LegacyStacks::isLegacy));
    }

    private static boolean own(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof LegacyItem) return true;
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : stack.getComponentsPatch().entrySet()) {
            if (DeLegacy.isRaw(e.getKey())) return true;
        }
        return false;
    }

    /** The stacks a component value holds: a container, a bundle, crossbow projectiles, Continuum's item lists. */
    private static Stream<ItemStack> held(Object value) {
        return switch (value) {
            case ItemStack stack -> Stream.of(stack);
            case ItemContents contents -> contents.copies().stream();
            case ItemContainerContents container -> container.stream();
            case BundleContents bundle -> bundle.itemCopyStream();
            case ChargedProjectiles projectiles -> projectiles.getItems().stream();
            default -> Stream.empty();
        };
    }

    /**
     * Converts every legacy stack in {@code container}; returns how many. Nothing in the container changes unless what
     * was parked is on disk first.
     */
    public static int convertAll(Container container, MinecraftServer server, String where) {
        Run run = null;
        ItemStack[] out = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!isLegacy(stack)) continue;
            if (run == null) run = new Run(server);
            out[i] = run.convert(stack, where);
        }
        if (run == null || !run.finish(where)) return 0;
        for (int i = 0; i < out.length; i++) if (out[i] != null) container.setItem(i, out[i]);
        return run.converted;
    }

    /** The converted stack, empty when it was parked, or the stack itself when the parked record could not be kept. */
    public static ItemStack convert(ItemStack stack, MinecraftServer server, String where) {
        Run run = new Run(server);
        ItemStack out = run.convert(stack, where);
        return run.finish(where) ? out : stack;
    }

    private static final class Run implements StackContext {
        private final MinecraftServer server;
        private final ListTag parked = new ListTag();
        private final Map<String, Long> counts = new TreeMap<>();
        private int converted;

        Run(MinecraftServer server) {
            this.server = server;
        }

        ItemStack convert(ItemStack stack, String where) {
            if (!own(stack)) return inside(stack, where);
            // the stack codec takes counts up to 99; a storage mod's bigger stack keeps its count in the tag
            CompoundTag tag = (CompoundTag) stack.copyWithCount(Math.min(stack.getCount(), 99)).save(server.registryAccess());
            tag.putInt("count", stack.getCount());
            converted++;
            if (!new Walker(this).stacks().convert(tag, where)) return ItemStack.EMPTY;
            int count = tag.getInt("count");
            CompoundTag probe = tag.copy();
            probe.putInt("count", Math.clamp(count, 1, 99));
            ItemStack out = ItemStack.parse(server.registryAccess(), probe).orElse(ItemStack.EMPTY);
            if (out.isEmpty() || own(out)) {
                park("item", where, tag.getString("id"), tag);
                return ItemStack.EMPTY;
            }
            out.setCount(count);
            return out;
        }

        /** A clean stack holding legacy ones (a DE: Space PLSS in a helmet): only those convert, never it a second time. */
        @SuppressWarnings("unchecked")
        private ItemStack inside(ItemStack stack, String where) {
            ItemStack out = stack.copy();
            for (Map.Entry<DataComponentType<?>, Optional<?>> e : stack.getComponentsPatch().entrySet()) {
                if (e.getValue().isEmpty()) continue;
                Object value = e.getValue().get();
                Object converted = switch (value) {
                    case ItemStack held -> each(held, where);
                    case ItemContents contents -> new ItemContents(contents.copies().stream().map(s -> each(s, where)).toList());
                    case ItemContainerContents container -> ItemContainerContents.fromItems(container.stream().map(s -> each(s, where)).toList());
                    // these and a lone held stack cannot save an empty stack; a parked one is kept in the leftovers
                    case BundleContents bundle -> new BundleContents(bundle.itemCopyStream().map(s -> each(s, where)).filter(s -> !s.isEmpty()).toList());
                    case ChargedProjectiles projectiles -> ChargedProjectiles.of(projectiles.getItems().stream().map(s -> each(s, where)).filter(s -> !s.isEmpty()).toList());
                    default -> value;
                };
                if (converted == value) continue;
                if (converted instanceof ItemStack single && single.isEmpty()) out.remove(e.getKey());
                else out.set((DataComponentType<Object>) e.getKey(), converted);
            }
            return out;
        }

        private ItemStack each(ItemStack stack, String where) {
            return isLegacy(stack) ? convert(stack, where) : stack;
        }

        /** Keeps the parked records; false leaves the stacks as they were, to convert on a later touch. */
        boolean finish(String where) {
            LOGGER.info("DE legacy: converted {} stack(s) at {}, {}", converted, where, counts);
            if (parked.isEmpty()) return true;
            Path file = server.getWorldPath(LevelResource.ROOT).resolve(LEFTOVERS);
            synchronized (LegacyStacks.class) {
                try {
                    CompoundTag root = AtomicNbt.read(file);
                    if (root == null) root = new CompoundTag();
                    ListTag records = root.getList("records", Tag.TAG_COMPOUND);
                    records.addAll(parked);
                    root.put("records", records);
                    AtomicNbt.write(root, file);
                    return true;
                } catch (IOException e) {
                    LOGGER.error("DE legacy: could not keep {} parked record(s) from {}; the stacks stay as they are", parked.size(), where, e);
                    return false;
                }
            }
        }

        @Override
        public void park(String kind, String where, String id, @Nullable Tag nbt) {
            CompoundTag record = new CompoundTag();
            record.putString("kind", kind);
            record.putString("where", where);
            record.putString("id", id);
            if (nbt != null) record.put("nbt", nbt.copy());
            parked.add(record);
            count(kind + " parked", 1);
        }

        @Override
        public void count(String key, long n) {
            counts.merge(key, n, Long::sum);
        }

        @Override
        public boolean hasComponent(String id) {
            return ContinuumStacks.hasComponent(id);
        }

        @Override
        public @Nullable CompoundTag filledComponents(String item, String fluid, int amount) {
            return ContinuumStacks.filledComponents(server.registryAccess(), item, fluid, amount);
        }

        @Override
        public @Nullable String weaponModName(int id) {
            return ContinuumStacks.weaponModName(id);
        }

        @Override
        public @Nullable List<String> undecodable(CompoundTag stack) {
            return ContinuumStacks.undecodable(server.registryAccess(), stack);
        }

        @Override
        public long gameTime() {
            return server.overworld().getGameTime();
        }

        @Override
        public boolean hasEntity(String id) {
            return BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(id));
        }

        @Override
        public byte @Nullable [] lockedContentsKey() {
            return ContinuumStacks.lockedContentsKey(server.getWorldPath(LevelResource.ROOT));
        }
    }
}

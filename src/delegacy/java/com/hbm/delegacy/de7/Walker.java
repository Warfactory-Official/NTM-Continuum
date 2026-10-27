package com.hbm.delegacy.de7;

import java.util.ArrayList;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Finds every item stack and entity in any NBT shape at any depth (shulkers, bundles, frames, minecart chests, mob
 * equipment, contraptions, other mods' blocks) and converts it in place. A stack or entity that has no Continuum home
 * is parked and removed from its container.
 */
public final class Walker {
    /** Where vanilla keeps a block state outside a palette: falling block, enderman, minecart, moving piston, display. */
    private static final Set<String> BLOCK_STATE_KEYS = Set.of("BlockState", "carriedBlockState", "DisplayState", "blockState", "block_state");
    /** Entity data without a position: a spawner's next mob and its candidates, a spawn egg's or bucket's mob. */
    private static final Set<String> SPAWN_KEYS = Set.of("entity", "minecraft:entity_data", "minecraft:bucket_entity_data");
    /** Lists read by index: a parked stack leaves an empty entry, so the stacks after it keep their slot. */
    private static final Set<String> POSITIONAL = Set.of("ArmorItems", "HandItems", "hbm:armor_mods", "hbm:ammo_bag",
            "hbm:casing_bag", "hbm:plastic_bag", "hbm:containment_box", "hbm:toolbox", "hbm:arty_cargo");
    /** What a {slot, item} wrapper holds besides its stack; with the stack parked, nothing worth keeping is left. */
    private static final Set<String> WRAPPER_KEYS = Set.of("slot", "Slot", "progress");
    private final StackContext ctx;
    private final Stacks stacks;

    public Walker(StackContext ctx) {
        this.ctx = ctx;
        this.stacks = new Stacks(ctx, this);
    }

    public Stacks stacks() {
        return stacks;
    }

    public static boolean isStack(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING) && tag.contains("count", Tag.TAG_ANY_NUMERIC);
    }

    public static boolean isEntity(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING) && tag.contains("Pos", Tag.TAG_LIST) && !tag.contains("count");
    }

    /** Converts what {@code tag} holds; returns false if {@code tag} itself must be removed from its parent. */
    public boolean walk(CompoundTag tag, String where) {
        lootTable(tag, "LootTable");
        if (isStack(tag)) {
            lootTable(tag.getCompound("components").getCompound("minecraft:container_loot"), "loot_table");
            return stacks.convert(tag, where);
        }
        boolean entity = isEntity(tag);
        if (entity && !entity(tag, where)) return false;
        boolean stackRemoved = false;
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag child = tag.get(key);
            if (child instanceof CompoundTag compound && BLOCK_STATE_KEYS.contains(key) && compound.getString("Name").startsWith("hbm:")) {
                tag.put(key, blockState(compound, where));
            } else if (child instanceof CompoundTag compound) {
                if (SPAWN_KEYS.contains(key) && !isEntity(compound)) spawnedId(compound, where);
                if (!walk(compound, where)) {
                    tag.remove(key);
                    stackRemoved = true;
                }
            } else if (child instanceof ListTag list) {
                walkList(list, where, key);
            }
        }
        if (entity && hasHbmData(tag)) ctx.entityConverted(tag, where);
        // a wrapper left holding only its slot would fail its container's codec and take the other stacks with it
        return !(stackRemoved && !entity && WRAPPER_KEYS.containsAll(tag.getAllKeys()));
    }

    public void walkList(ListTag list, String where) {
        walkList(list, where, "");
    }

    private void walkList(ListTag list, String where, String key) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Tag child = list.get(i);
            if (child instanceof CompoundTag compound) {
                if (walk(compound, where)) continue;
                if (POSITIONAL.contains(key)) list.set(i, new CompoundTag());
                else list.remove(i);
            } else if (child instanceof ListTag inner) {
                walkList(inner, where, key);
            }
        }
    }

    /** A refunded block has no state to stand for it, so the entity holding it keeps air and the original is parked. */
    private CompoundTag blockState(CompoundTag state, String where) {
        CompoundTag out = ctx.blockState(state);
        if (out.getString("Name").equals("minecraft:air")) ctx.park("block_state", where, state.getString("Name"), state);
        return out;
    }

    private void spawnedId(CompoundTag tag, String where) {
        String id = tag.getString("id");
        String target = Tables.get().entities.get(id);
        if (target != null && !target.equals("park")) {
            tag.putString("id", target);
            ctx.count("entities renamed");
        } else if (id.startsWith("hbm:") && !ctx.hasEntity(id)) {
            ctx.park("spawned_entity", where, id, tag.copy());
        }
    }

    /** A container's lazy loot (block entity, chest minecart, an item's container_loot) by the table served for it. */
    private static void lootTable(CompoundTag tag, String key) {
        String to = tag.contains(key, Tag.TAG_STRING) ? Tables.get().lootTables.get(tag.getString(key)) : null;
        if (to != null) tag.putString(key, to);
    }

    /** An HBM entity, or any entity with an HBM attachment. */
    public static boolean hasHbmData(CompoundTag tag) {
        if (tag.getString("id").startsWith("hbm:")) return true;
        for (String key : tag.getCompound("neoforge:attachments").getAllKeys()) if (key.startsWith("hbm:")) return true;
        return false;
    }

    /** Renames and reshapes an entity; false parks it. Its contents are walked by the caller's loop. */
    private boolean entity(CompoundTag tag, String where) {
        String id = tag.getString("id");
        String target = Tables.get().entities.get(id);
        if ("park".equals(target)) {
            ctx.park("entity", where, id, tag);
            ctx.count("entities parked");
            return false;
        }
        if (target != null) {
            tag.putString("id", target);
            ctx.count("entities renamed");
        }
        if (tag.getString("id").startsWith("hbm:") && !ctx.hasEntity(tag.getString("id"))) {
            ctx.park("entity", where, id, tag);
            ctx.count("entities parked");
            return false;
        }
        Entities.reshape(tag, id, ctx, where);
        return true;
    }
}

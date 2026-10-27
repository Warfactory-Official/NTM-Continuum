package com.hbm.delegacy.de7;

import com.hbm.items.special.ItemHot;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** What converting a DE stack or entity needs: where leftovers go, counters, and a few answers from Continuum. */
public interface StackContext {
    /** Keeps what could not be converted, with where it was. */
    void park(String kind, String where, String id, @Nullable Tag nbt);

    void count(String key, long n);

    default void count(String key) {
        count(key, 1);
    }

    boolean hasComponent(String id);

    /** The components Continuum's own fluid item writes when filled, or null. */
    @Nullable CompoundTag filledComponents(String item, String fluid, int amount);

    @Nullable String weaponModName(int id);

    /** DE's AES key for sealed crate contents, or null. */
    byte @Nullable [] lockedContentsKey();

    /**
     * Component keys Continuum cannot decode on this converted stack; null when the stack as a whole cannot be decoded
     * (an unknown item), empty when it decodes or cannot be checked here.
     */
    @Nullable List<String> undecodable(CompoundTag stack);

    default boolean hasEntity(String id) {
        return true;
    }

    /** The world's game time, which DE's fuse components count against. */
    long gameTime();

    /** The heat a hot item of this id holds when fully hot here, or 0 for one that does not heat. */
    default int maxHeat(String item) {
        ResourceLocation key = ResourceLocation.tryParse(item);
        return key != null && BuiltInRegistries.ITEM.get(key) instanceof ItemHot hot ? hot.maxHeat : 0;
    }

    /** True for an id registered as an item here. */
    default boolean isItem(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.ITEM.containsKey(key);
    }

    /** Continuum's custom machine name (1.7's unlocalized name) for a DE definition id, or null. */
    default @Nullable String customMachineName(String id) {
        return id.equals("hbm:paper_press") ? "paperPress" : null;
    }

    /** A DE block state inside entity data in Continuum's terms; unchanged where there are no block tables. */
    default CompoundTag blockState(CompoundTag state) {
        return state;
    }

    /**
     * A DE block entity inside entity data, converted for the block {@code state} (already in Continuum's terms);
     * false when it has no Continuum home and was parked. Its stacks are left to the walker.
     */
    default boolean blockEntity(CompoundTag be, CompoundTag state, String where) {
        return true;
    }

    /** A converted entity carrying HBM data, for a check against Continuum's own save once a level exists. */
    default void entityConverted(CompoundTag entity, String where) {}
}

package com.hbm.deimport.engine;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** What the converter needs to know about the running Continuum; only {@code target/} answers it with game classes. */
public interface Target {
    boolean hasBlock(String id);

    boolean hasItem(String id);

    boolean hasEntity(String id);

    boolean hasComponent(String id);

    /** The criteria names of a Continuum advancement, or null if there is none. */
    @Nullable List<String> advancementCriteria(String id);

    /** Valid values per property of a block; an absent property is not on the block. */
    Map<String, List<String>> properties(String id);

    /** The block-entity type id the block creates for these properties, or null if it has none. */
    @Nullable String blockEntityType(String id, CompoundTag properties);

    boolean isMultiblockCore(String id);

    /** A multiblock core's own state, never one of its cells, which can be the core's block in another state. */
    boolean isCoreState(String id, CompoundTag properties);

    /** A conductor that joins its network when placed, so an imported one must be announced once the level exists. */
    boolean isPlacedConductor(String id);

    /**
     * The components Continuum's own fluid item writes when filled (fluid, amount and its tint), or null when the item
     * holds no fluid that way.
     */
    @Nullable CompoundTag filledComponents(String item, String fluid, int amount);

    /**
     * Cell positions and states a multiblock core places in Continuum, worked out without a level; an RBMK column is
     * {@code columnHeight} blocks high, core included, or the default height when null.
     */
    Map<Long, CompoundTag> footprint(String block, CompoundTag properties, long core, @Nullable Integer columnHeight);

    /** The stack size an item of this id keeps here, or 0 when it is no item here. */
    int maxStackSize(String item);

    /** A block a refund chest may be written over: air or a replaceable block of a mod that is here. */
    boolean canHoldRefund(CompoundTag state);

    /** The block state a refund chest is written as. */
    CompoundTag refundChest();

    /** Points a pile member's saved block entity at its core; false for any other block entity. */
    boolean linkPileMember(CompoundTag be, long core);

    /** Continuum's saved name of a gun modifier with this 1.7 id, or null. */
    @Nullable String weaponModName(int id);

    /** The item a block drops as itself, for refunds, or null. */
    @Nullable String itemFor(String blockId);

    /** Whether the running game knows this structure; true when it cannot be asked. */
    boolean hasStructure(String id);

    /** Whether a structure piece decodes as vanilla loads it: its type, and a jigsaw piece's pool element. */
    boolean loadsPiece(CompoundTag piece);

    /** Continuum's name for the {@code index}th mixer recipe making {@code fluid}, or null. */
    @Nullable String mixerRecipe(String fluid, int index);

    /** Continuum's assembler recipe that packs {@code fluid} into a fluid pack, or unpacks it; null without one. */
    @Nullable String packRecipe(String fluid, boolean unpack);

    boolean hasSatelliteType(String type);

    /** The lowest section index of a dimension, -4 when it is not known. */
    int minSection(String dim);

    /** See {@code StackContext.undecodable}. */
    @Nullable List<String> undecodable(CompoundTag stack);

    /**
     * What of a converted block entity Continuum's own block entity for that block does not keep, path to original
     * value; null when it cannot be checked outside a level. Numbers in {@code tag} are first cast to the types
     * Continuum saves.
     */
    @Nullable Map<String, Tag> blockEntityLost(String block, CompoundTag properties, CompoundTag tag);
}

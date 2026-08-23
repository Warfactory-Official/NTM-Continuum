package com.hbm.handler.neutron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.HashMap;
import java.util.Map;

/** Cached cell in a world's neutron graph. */
public abstract class NeutronNode {
    private final Map<Key<?>, Object> data = new HashMap<>();
    protected NeutronStream.NeutronType type;
    protected BlockPos pos;
    protected BlockEntity tile;
    public NeutronNode(BlockEntity tile, NeutronStream.NeutronType type) { this.type = type; this.tile = tile; this.pos = tile.getBlockPos(); }
    public <T> void set(Key<T> key, T value) { data.put(key, value); }
    public <T> T get(Key<T> key) { return key.type().cast(data.get(key)); }
    public record Key<T>(String name, Class<T> type) {}
}

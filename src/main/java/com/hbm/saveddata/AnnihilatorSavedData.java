package com.hbm.saveddata;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hbm.inventory.recipes.AnnihilatorRecipes;

/**
 * 1.7 {@code AnnihilatorSavedData}: named pools counting everything ever annihilated, per dimension.
 *
 * <p>Pool keys are an {@link Item} (1.7's wildcard item, which with flattened metadata is also its
 * "type + meta" ComparableStack), a {@link FluidType}, or a {@link String} tag id standing in for the
 * ore-dictionary name. Amounts are {@link BigInteger}s stored as decimal strings.
 */
public final class AnnihilatorSavedData extends CompatSavedData<AnnihilatorSavedData> {

    private static final String ID = Constants.MOD_ID + ":annihilator";

    private record Entry(byte key, String id, String amount) {}
    private record Pool(String name, List<Entry> entries) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BYTE.fieldOf("key").forGetter(Entry::key),
            Codec.STRING.fieldOf("id").forGetter(Entry::id),
            Codec.STRING.fieldOf("amount").forGetter(Entry::amount)
    ).apply(i, Entry::new));
    private static final Codec<Pool> POOL_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("poolname").forGetter(Pool::name),
            ENTRY_CODEC.listOf().optionalFieldOf("pool", List.of()).forGetter(Pool::entries)
    ).apply(i, Pool::new));
    public static final Codec<AnnihilatorSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            POOL_CODEC.listOf().optionalFieldOf("pools", List.of()).forGetter(AnnihilatorSavedData::toPools)
    ).apply(i, AnnihilatorSavedData::fromPools));

    public final Map<String, AnnihilatorPool> pools = new HashMap<>();

    @Override
    protected Codec<AnnihilatorSavedData> codec() {
        return CODEC;
    }

    public static AnnihilatorSavedData getData(ServerLevel level) {
        return CompatSavedData.get(level, ID, AnnihilatorSavedData::new, CODEC);
    }

    private static AnnihilatorSavedData fromPools(List<Pool> pools) {
        AnnihilatorSavedData data = new AnnihilatorSavedData();
        for (Pool pool : pools) {
            AnnihilatorPool instance = new AnnihilatorPool();
            for (Entry entry : pool.entries()) {
                try {
                    Object key = deserializeKey(entry);
                    if (key != null) instance.items.put(key, new BigInteger(entry.amount()));
                } catch (Throwable ignored) { } // because world data can be dented to all hell and back
            }
            data.pools.put(pool.name(), instance);
        }
        return data;
    }

    private List<Pool> toPools() {
        List<Pool> list = new ArrayList<>();
        for (Map.Entry<String, AnnihilatorPool> pool : pools.entrySet()) {
            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<Object, BigInteger> entry : pool.getValue().items.entrySet()) {
                Entry serialized = serializeKey(entry.getKey(), entry.getValue());
                if (serialized != null) entries.add(serialized);
            }
            list.add(new Pool(pool.getKey(), entries));
        }
        return list;
    }

    public AnnihilatorPool grabPool(String pool) {
        AnnihilatorPool poolInstance = pools.get(pool);
        if (poolInstance == null) {
            poolInstance = new AnnihilatorPool();
            pools.put(pool, poolInstance);
        }
        return poolInstance;
    }

    /** For fluids */
    public @Nullable ItemStack pushToPool(String pool, FluidType type, long amount, boolean alwaysPayOut) {
        AnnihilatorPool poolInstance = grabPool(pool);
        ItemStack payout = poolInstance.increment(type, amount, alwaysPayOut);
        this.setDirty();
        return payout;
    }

    /** For items (the item itself, plus every tag it carries in place of the 1.7 ore dictionary) */
    public @Nullable ItemStack pushToPool(String pool, ItemStack stack, boolean alwaysPayOut) {
        AnnihilatorPool poolInstance = grabPool(pool);

        ItemStack itemPayout = poolInstance.increment(stack.getItem(), stack.getCount(), alwaysPayOut);
        // 1.7 also counted the type+meta ComparableStack; with flattened metadata that is the item itself
        ItemStack dictPayout = null;

        for (String name : tagNames(stack)) {
            ItemStack payout = poolInstance.increment(name, stack.getCount(), alwaysPayOut);
            if (payout != null) dictPayout = payout;
        }

        this.setDirty();

        return dictPayout != null ? dictPayout : itemPayout;
    }

    /** The 1.7 ore-dictionary names of a stack: its item tag ids. */
    public static List<String> tagNames(ItemStack stack) {
        List<String> names = new ArrayList<>();
        stack.getItem().builtInRegistryHolder().tags().forEach(tag -> names.add(tag.location().toString()));
        return names;
    }

    private static @Nullable Entry serializeKey(Object key, BigInteger amount) {
        if (key instanceof Item item) { // 0
            return new Entry((byte) 0, BuiltInRegistries.ITEM.getKey(item).toString(), amount.toString());
        }
        if (key instanceof FluidType type) { // 2
            return new Entry((byte) 2, type.getName(), amount.toString());
        }
        if (key instanceof String dict) { // 3
            return new Entry((byte) 3, dict, amount.toString());
        }
        return null;
    }

    private static @Nullable Object deserializeKey(Entry entry) {
        return switch (entry.key()) {
            case 0 -> BuiltInRegistries.ITEM.getValue(Ids.parse(entry.id()));
            case 1 -> {
                // written before 2026-10-06 as "<item>@<metadata>" for a family kept on one item; every family is one
                // item per type now, so the count goes to the base item (History §19es-itemvariant)
                int at = entry.id().lastIndexOf('@');
                yield BuiltInRegistries.ITEM.getValue(Ids.parse(at < 0 ? entry.id() : entry.id().substring(0, at)));
            }
            case 2 -> Fluids.fromName(entry.id());
            case 3 -> entry.id();
            default -> null;
        };
    }

    public static final class AnnihilatorPool {

        /** Valid keys: Items, FluidTypes, Strings for tag ids. */
        public final Map<Object, BigInteger> items = new HashMap<>();

        public @Nullable ItemStack increment(Object type, long amount, boolean alwaysPayOut) {
            ItemStack payout;
            BigInteger counter = items.get(type);
            if (counter == null) {
                counter = BigInteger.valueOf(amount);
                payout = AnnihilatorRecipes.getHighestPayoutFromKey(type, BigInteger.ZERO, counter);
            } else {
                BigInteger prev = counter;
                counter = counter.add(BigInteger.valueOf(amount));
                payout = AnnihilatorRecipes.getHighestPayoutFromKey(type, alwaysPayOut ? null : prev, counter);
            }
            items.put(type, counter);
            return payout;
        }
    }
}

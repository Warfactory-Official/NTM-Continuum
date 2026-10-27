package com.hbm.delegacy;

import com.hbm.delegacy.legacy.DeLegacy;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.weapon.sedna.mods.IWeaponMod;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Continuum's answers that converting a DE stack needs, shared by the converter and the legacy layer. */
public final class ContinuumStacks {
    private ContinuumStacks() {}

    /** A component Continuum reads; DE's raw holders do not count. */
    public static boolean hasComponent(String id) {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ResourceLocation.parse(id)) && !DeLegacy.isRaw(id);
    }

    /** The components Continuum's own fluid item writes when filled (fluid, amount and its tint), or null. */
    public static @Nullable CompoundTag filledComponents(@Nullable HolderLookup.Provider registries, String item, String fluid, int amount) {
        if (registries == null || !(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)) instanceof ItemFluidTank tank)) return null;
        FluidType type = Fluids.byRegistryName(fluid);
        if (type == null || type == Fluids.NONE) return null;
        ItemStack stack = new ItemStack(tank);
        tank.setFluid(stack, type, amount);
        return ((CompoundTag) stack.save(registries)).getCompound("components");
    }

    /** See {@code StackContext.undecodable}; checked with Continuum's own item codec, nothing logged. */
    public static @Nullable List<String> undecodable(@Nullable HolderLookup.Provider registries, CompoundTag stack) {
        if (registries == null) return List.of();
        ResourceLocation id = ResourceLocation.tryParse(stack.getString("id"));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        // probed at a count the codec takes: a storage mod's oversized stack is not a bad component
        stack = stack.copy();
        if (stack.getInt("count") > 99) stack.putInt("count", 99);
        if (ItemStack.CODEC.parse(ops, stack).isSuccess()) return List.of();
        CompoundTag components = stack.getCompound("components");
        List<String> bad = new ArrayList<>();
        for (String key : components.getAllKeys()) {
            CompoundTag probe = stack.copy();
            CompoundTag one = new CompoundTag();
            one.put(key, components.get(key).copy());
            probe.put("components", one);
            if (ItemStack.CODEC.parse(ops, probe).isError()) bad.add(key);
        }
        return bad.isEmpty() ? null : bad;
    }

    public static @Nullable String weaponModName(int id) {
        IWeaponMod mod = XWeaponModManager.idToMod.get(id);
        return mod == null ? null : XWeaponModManager.nameOf(mod);
    }

    /** DE's AES key for sealed crate contents ({@code data/hbm_locked_contents_key.dat}), or null. */
    public static byte @Nullable [] lockedContentsKey(Path world) {
        Path file = world.resolve("data/hbm_locked_contents_key.dat");
        try {
            if (!Files.isRegularFile(file)) return null;
            // DE's saved data sits inside vanilla's own "data" wrapper: {data: {data: [B; 32]}}
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            byte[] key = root.getCompound("data").getByteArray("data");
            if (key.length == 0) key = root.getByteArray("data");
            return key.length == 32 ? key : null;
        } catch (IOException e) {
            // an unreadable key leaves sealed crates parked, which is what a missing key does
            return null;
        }
    }
}

package com.hbm.tileentity.bomb;

import com.hbm.Constants;
import com.hbm.api.data.MachineDataMaps;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemCell;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import com.hbm.inventory.container.ContainerNukeCustom;
import com.hbm.tileentity.ModBombs2BlockEntities;

/**
 * The Custom Nuke's 27-slot inventory. Every server tick the contents are weighed through the
 * {@link #entries} table (1.7 {@code registerBombItems}) into seven stage levels; additive entries sum
 * {@code value * count}, multiplicative entries multiply the stage's modifier by {@code value * count}
 * (yes, times count - a stack of 64 redstone multiplies by 67.2; the original did that and players use it).
 * Stage gating follows: nuclear needs TNT 16, hydrogen needs nuclear 100, antimatter and schrabidium need
 * nuclear 50, anti-mass needs schrabidium.
 */
public class TileEntityNukeCustom extends TileEntityNukeBomb {

    public float tnt;
    public float nuke;
    public float hydro;
    public float amat;

    /**
     * The nuclear level below which the antimatter stage is void (1.7: 50; ADDON-API-INVENTORY V-171). NTM: Space sets
     * 0: hbm-space commented the gate out, so antimatter arms on its own.
     */
    public static float amatNukeFloor = 50F;
    public float dirty;
    public float schrab;
    public float euph;

    public TileEntityNukeCustom(BlockPos pos, BlockState state) {
        super(ModBombs2BlockEntities.NUKE_CUSTOM.get(), pos, state, 27);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityNukeCustom bomb) {
        bomb.updateEntity();
    }

    /** 1.7 {@code updateEntity}: the yield pass. */
    public void updateEntity() {
        float tnt = 0F, tntMod = 1F;
        float nuke = 0F, nukeMod = 1F;
        float hydro = 0F, hydroMod = 1F;
        float amat = 0F, amatMod = 1F;
        float dirty = 0F, dirtyMod = 1F;
        float schrab = 0F, schrabMod = 1F;
        float euph = 0F;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            CustomNukeEntry ent = entryFor(stack);
            if (ent == null) continue;

            if (ent.entry == EnumEntryType.ADD) {
                switch (ent.type) {
                    case TNT -> tnt += ent.value * stack.getCount();
                    case NUKE -> nuke += ent.value * stack.getCount();
                    case HYDRO -> hydro += ent.value * stack.getCount();
                    case AMAT -> amat += ent.value * stack.getCount();
                    case DIRTY -> dirty += ent.value * stack.getCount();
                    case SCHRAB -> schrab += ent.value * stack.getCount();
                    case EUPH -> euph += ent.value * stack.getCount();
                }
            } else if (ent.entry == EnumEntryType.MULT) {
                switch (ent.type) {
                    case TNT -> tntMod *= ent.value * stack.getCount();
                    case NUKE -> nukeMod *= ent.value * stack.getCount();
                    case HYDRO -> hydroMod *= ent.value * stack.getCount();
                    case AMAT -> amatMod *= ent.value * stack.getCount();
                    case DIRTY -> dirtyMod *= ent.value * stack.getCount();
                    case SCHRAB -> schrabMod *= ent.value * stack.getCount();
                    default -> { }
                }
            }
        }

        tnt *= tntMod;
        nuke *= nukeMod;
        hydro *= hydroMod;
        amat *= amatMod;
        dirty *= dirtyMod;
        schrab *= schrabMod;

        if (tnt < 16) nuke = 0;
        if (nuke < 100) hydro = 0;
        if (nuke < amatNukeFloor) amat = 0;
        if (nuke < 50) schrab = 0;
        if (schrab == 0) euph = 0;

        boolean changed = this.tnt != tnt || this.nuke != nuke || this.hydro != hydro || this.amat != amat
                || this.dirty != dirty || this.schrab != schrab || this.euph != euph;
        this.tnt = tnt;
        this.nuke = nuke;
        this.hydro = hydro;
        this.amat = amat;
        this.dirty = dirty;
        this.schrab = schrab;
        this.euph = euph;
        // 1.7 clients recomputed this themselves from the synced slots; here the server ships the result.
        if (changed) networkPackNT();
    }

    public float getNukeAdj() {
        if (nuke == 0) return 0;
        return Math.min(nuke + tnt / 2, ExplosionNukeCustom.MAX_NUKE);
    }

    public float getHydroAdj() {
        if (hydro == 0) return 0;
        return Math.min(hydro + nuke / 2 + tnt / 4, ExplosionNukeCustom.MAX_HYDRO);
    }

    public float getAmatAdj() {
        if (amat == 0) return 0;
        return Math.min(amat + hydro / 2 + nuke / 4 + tnt / 8, ExplosionNukeCustom.MAX_AMAT);
    }

    public float getSchrabAdj() {
        if (schrab == 0) return 0;
        return Math.min(schrab + amat / 2 + hydro / 4 + nuke / 8 + tnt / 16, ExplosionNukeCustom.MAX_SCHRAB);
    }

    /** A {@code custom_fall} anywhere turns detonation into a drop. */
    public boolean isFalling() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ModItems.custom_fall.get()) return true;
        }
        return false;
    }

    @Override
    public boolean isReady() {
        // 1.7 had no readiness gate: an empty custom nuke does nothing when it "explodes".
        return true;
    }

    @Override
    public String getDefaultName() {
        return "container.nukeCustom";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeCustom(containerId, inventory, this);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("tnt", tnt);
        output.putFloat("nuke", nuke);
        output.putFloat("hydro", hydro);
        output.putFloat("amat", amat);
        output.putFloat("dirty", dirty);
        output.putFloat("schrab", schrab);
        output.putFloat("euph", euph);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tnt = input.getFloatOr("tnt", 0F);
        nuke = input.getFloatOr("nuke", 0F);
        hydro = input.getFloatOr("hydro", 0F);
        amat = input.getFloatOr("amat", 0F);
        dirty = input.getFloatOr("dirty", 0F);
        schrab = input.getFloatOr("schrab", 0F);
        euph = input.getFloatOr("euph", 0F);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeFloat(tnt);
        buffer.writeFloat(nuke);
        buffer.writeFloat(hydro);
        buffer.writeFloat(amat);
        buffer.writeFloat(dirty);
        buffer.writeFloat(schrab);
        buffer.writeFloat(euph);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        tnt = buffer.readFloat();
        nuke = buffer.readFloat();
        hydro = buffer.readFloat();
        amat = buffer.readFloat();
        dirty = buffer.readFloat();
        schrab = buffer.readFloat();
        euph = buffer.readFloat();
    }

    // The ingredient table

    public enum EnumBombType {
        TNT("TNT"),
        NUKE("Nuclear"),
        HYDRO("Hydrogen"),
        AMAT("Antimatter"),
        DIRTY("Salted"),
        SCHRAB("Schrabidium"),
        EUPH("Anti Mass");

        final String name;

        EnumBombType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum EnumEntryType {
        ADD,
        MULT
    }

    public static class CustomNukeEntry {
        public final EnumBombType type;
        public final EnumEntryType entry;
        public final float value;

        public CustomNukeEntry(EnumBombType type, float value) {
            this(type, value, EnumEntryType.ADD);
        }

        public CustomNukeEntry(EnumBombType type, float value, EnumEntryType entry) {
            this.type = type;
            this.entry = entry;
            this.value = value;
        }
    }

    /**
     * The ingredient value of {@code stack}: 1.7 {@code registerBombItems} as the data maps {@code hbm:item/custom_nuke}
     * (items) and {@code hbm:fluid/custom_nuke} (a full fluid cell by its fluid; 1.7's metadata cells), MachineDataMaps.
     */
    public static @Nullable CustomNukeEntry entryFor(ItemStack stack) {
        MachineDataMaps.NukeEntry entry;
        if (stack.getItem() instanceof ItemCell) {
            FluidType fluid = ItemCell.getFluidType(stack);
            if (fluid == null || !ItemCell.isFullCell(stack, fluid)) return null;
            entry = MachineDataMaps.CUSTOM_NUKE_CELL.get(Fluids.toVanilla(fluid));
        } else {
            entry = MachineDataMaps.CUSTOM_NUKE.get(stack.getItem());
        }
        return entry == null ? null : new CustomNukeEntry(entry.type(), entry.value(), entry.op());
    }
}

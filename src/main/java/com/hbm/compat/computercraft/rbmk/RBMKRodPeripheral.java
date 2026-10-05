package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code TileEntityRBMKRod}'s component {@code rbmk_fuel_rod}, ReaSim channels included. The rod readouts answer
 * {@code "N/A"} while the channel holds no fuel rod.
 */
public final class RBMKRodPeripheral extends RBMKColumnPeripheral<BlockEntityRBMKRod> {

    private static final int FLUX_QUANTITY = SLOTS;
    private static final int FLUX_RATIO = SLOTS + 1;
    private static final int HAS_ROD = SLOTS + 2;
    private static final int ENRICHMENT = SLOTS + 3;
    private static final int POISON = SLOTS + 4;
    private static final int CORE_HEAT = SLOTS + 5;
    private static final int HULL_HEAT = SLOTS + 6;
    private static final int MODERATED = SLOTS + 7;
    private static final int ROD_TYPE = 0;

    public RBMKRodPeripheral(BlockEntityRBMKRod machine) {
        super(machine, "rbmk_fuel_rod", SLOTS + 8, 1);
    }

    @Override
    protected void capture(BlockEntityRBMKRod machine) {
        super.capture(machine);
        put(FLUX_QUANTITY, machine.lastFluxQuantity);
        put(FLUX_RATIO, machine.lastFluxRatio);
        put(MODERATED, machine.isModerated());
        ItemStack stack = machine.getItem(0);
        boolean fuelled = stack.getItem() instanceof ItemRBMKRod;
        put(HAS_ROD, fuelled);
        if (fuelled) {
            put(ENRICHMENT, ItemRBMKRod.getEnrichment(stack));
            put(POISON, ItemRBMKRod.getPoison(stack));
            put(CORE_HEAT, ItemRBMKRod.getCoreHeat(stack));
            put(HULL_HEAT, ItemRBMKRod.getHullHeat(stack));
            // the description id ("item.hbm.x") stands in for 1.7's getUnlocalizedName ("item.x")
            putRef(ROD_TYPE, stack.getItem().getDescriptionId());
        }
    }

    private static Object orNA(SnapshotPeripheral<?> s, int slot) {
        return s.booleanAt(HAS_ROD) ? (Object) s.doubleAt(slot) : "N/A";
    }

    private static Object typeOrNA(SnapshotPeripheral<?> s) {
        return s.booleanAt(HAS_ROD) ? s.refAt(ROD_TYPE) : "N/A";
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getFluxQuantity() {
        return read(s -> new Object[]{s.doubleAt(FLUX_QUANTITY)});
    }

    @LuaFunction
    public final Object[] getFluxRatio() {
        return read(s -> new Object[]{s.doubleAt(FLUX_RATIO)});
    }

    @LuaFunction
    public final Object[] getDepletion() {
        return read(s -> new Object[]{orNA(s, ENRICHMENT)});
    }

    @LuaFunction
    public final Object[] getXenonPoison() {
        return read(s -> new Object[]{orNA(s, POISON)});
    }

    @LuaFunction
    public final Object[] getCoreHeat() {
        return read(s -> new Object[]{orNA(s, CORE_HEAT)});
    }

    @LuaFunction
    public final Object[] getSkinHeat() {
        return read(s -> new Object[]{orNA(s, HULL_HEAT)});
    }

    // the Java name cannot be getType: IPeripheral.getType is the peripheral's type string
    @LuaFunction("getType")
    public final Object[] rodType() {
        return read(s -> new Object[]{typeOrNA(s)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(HEAT), orNA(s, HULL_HEAT), orNA(s, CORE_HEAT),
                s.doubleAt(FLUX_QUANTITY), s.doubleAt(FLUX_RATIO), orNA(s, ENRICHMENT), orNA(s, POISON),
                typeOrNA(s), s.booleanAt(MODERATED), s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }

    @LuaFunction
    public final Object[] getModerated() {
        return read(s -> new Object[]{s.booleanAt(MODERATED)});
    }
}
//?}

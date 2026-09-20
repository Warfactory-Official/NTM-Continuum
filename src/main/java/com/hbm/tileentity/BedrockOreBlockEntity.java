package com.hbm.tileentity;

import dev.golem.core.data.Tags;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DrillBitType;
import com.hbm.items.special.ItemBedrockOreBase;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Persistent deposit state and the exact infinite-extraction gate used by the legacy excavator. */
public class BedrockOreBlockEntity extends CompatBlockEntity {

    private ItemStack resource = ItemStack.EMPTY;
    private @Nullable FluidStack acidRequirement;
    private int tier;
    private int color;
    private int shape;

    public BedrockOreBlockEntity(BlockPos pos, BlockState state) {
        super(ModOreBlocks.BEDROCK_ORE_BLOCK_ENTITY.get(), pos, state);
    }

    public void setDeposit(ItemStack resource, @Nullable FluidStack acidRequirement,
                           int tier, int color, int shape) {
        this.resource = resource.copy();
        this.acidRequirement = acidRequirement == null ? null : acidRequirement.copy();
        this.tier = tier;
        this.color = color;
        this.shape = shape;
        setChanged();
    }

    public ItemStack getResource() {
        return resource.copy();
    }

    public @Nullable FluidStack getAcidRequirement() {
        return acidRequirement == null ? null : acidRequirement.copy();
    }

    public int getTier() { return tier; }
    public int getColor() { return color; }
    public int getShape() { return shape; }

    /**
     * Performs the old excavator's collectBedrock check. Success consumes only the required fluid;
     * the deposit remains in place and can be extracted again after the five-minute drill cycle.
     */
    public ExtractionResult extract(@Nullable DrillBitType drill, @Nullable FluidTankNTM tank) {
        if (drill == null) return ExtractionResult.failure(ExtractionFailure.NO_DRILL);
        if (resource.isEmpty()) return ExtractionResult.failure(ExtractionFailure.NO_RESOURCE);
        if (tier > drill.tier) return ExtractionResult.failure(ExtractionFailure.TIER);
        if (acidRequirement != null) {
            if (tank == null || tank.getTankType() != acidRequirement.type) {
                return ExtractionResult.failure(ExtractionFailure.FLUID_TYPE);
            }
            if (tank.getFill() < acidRequirement.fill) {
                return ExtractionResult.failure(ExtractionFailure.FLUID_AMOUNT);
            }
            tank.setFill(tank.getFill() - acidRequirement.fill);
        }

        ItemStack output = resource.copy();
        if (output.is(ModOreBlocks.bedrock_ore_base.get())) {
            ItemBedrockOreBase.setOreAmount(output, worldPosition.getX(), worldPosition.getZ(),
                    1D + drill.fortune * 0.25D);
        }
        return new ExtractionResult(output, ExtractionFailure.NONE);
    }

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    @Override
    protected void saveData(DataOut output) {
        saveDeposit(output);
    }

    @Override
    protected void loadData(DataIn input) {
        loadDeposit(input);
    }

    /** Public for deterministic persistence tests and future feature-local migration code. */
    public void saveDeposit(DataOut output) {
        if (!resource.isEmpty()) output.store("resource", ItemStack.CODEC, resource);
        if (acidRequirement != null) {
            output.putString("fluid", acidRequirement.type.getName());
            output.putInt("amount", acidRequirement.fill);
        }
        output.putInt("tier", tier);
        output.putInt("color", color);
        output.putInt("shape", shape);
    }

    /** Public counterpart to {@link #saveDeposit}; malformed/missing old resource data falls back to iron. */
    public void loadDeposit(DataIn input) {
        resource = input.read("resource", ItemStack.CODEC)
                .filter(stack -> !stack.isEmpty())
                .orElseGet(() -> new ItemStack(ModItems.powder_iron.get()));
        FluidType fluid = Fluids.fromNameCompat(input.getStringOr("fluid", "NONE"));
        acidRequirement = fluid == Fluids.NONE ? null : new FluidStack(fluid, input.getIntOr("amount", 0));
        tier = input.getIntOr("tier", 0);
        color = input.getIntOr("color", 0);
        shape = input.getIntOr("shape", 0);
    }

    public enum ExtractionFailure {
        NONE,
        NO_DRILL,
        NO_RESOURCE,
        TIER,
        FLUID_TYPE,
        FLUID_AMOUNT
    }

    public record ExtractionResult(ItemStack output, ExtractionFailure failure) {
        public static ExtractionResult failure(ExtractionFailure failure) {
            return new ExtractionResult(ItemStack.EMPTY, failure);
        }

        public boolean successful() {
            return failure == ExtractionFailure.NONE;
        }
    }
}

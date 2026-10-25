package com.hbm.blocks.generic;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.items.ModItems;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 1.7 {@code BlockOreBasalt} ({@code ore_basalt}): the volcanic basalt ores. {@code EnumBasaltOreType} is
 * flattened to {@code ore_basalt_<type>}. Each drops exactly one of its item (no fortune: the old
 * {@code getDrops} went through {@code getDropsWithoutDamage} with {@code quantityDropped = 1}); silk
 * touch returns the block. The asbestos ore vents asbestos dust when walked on with air above (1 in 10,
 * plus five spore particles) and always when broken.
 */
public class BlockOreBasalt extends CompatBlock {

    public enum Type {
        SULFUR(() -> ModItems.sulfur.get()),
        FLUORITE(() -> ModItems.fluorite.get()),
        ASBESTOS(() -> ModItems.ingot_asbestos.get()),
        GEM(() -> ModItems.gem_volcanic.get()),
        MOLYSITE(() -> ModItems.powder_molysite.get());

        public final Supplier<Item> drop;
        Type(Supplier<Item> drop) { this.drop = drop; }

        public String id() { return "ore_basalt_" + name().toLowerCase(Locale.ROOT); }
        public String texture() { return "hbm:blocks/ore_basalt_" + name().toLowerCase(Locale.ROOT); }
    }

    public final Type type;

    public BlockOreBasalt(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        return List.of(new ItemStack(type.drop.get()));
    }

    /** 1.7 {@code onEntityWalking}: the asbestos ore sheds dust into the air above it. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (type != Type.ASBESTOS) return;
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) return;
        if (level instanceof ServerLevel server) {
            if (server.getRandom().nextInt(10) == 0) {
                server.setBlockAndUpdate(above, ModNuclearBlocks.gas_asbestos.get().defaultBlockState());
            }
            for (int i = 0; i < 5; i++) {
                server.sendParticles(ParticleTypes.MYCELIUM, pos.getX() + server.getRandom().nextFloat(), pos.getY() + 1.1,
                        pos.getZ() + server.getRandom().nextFloat(), 1, 0, 0, 0, 0);
            }
        }
    }

    /** 1.7 {@code dropBlockAsItemWithChance}: mining or blasting the asbestos ore leaves dust, replacing it does not. */
    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (type == Type.ASBESTOS) level.setBlockAndUpdate(pos, ModNuclearBlocks.gas_asbestos.get().defaultBlockState());
    }
}

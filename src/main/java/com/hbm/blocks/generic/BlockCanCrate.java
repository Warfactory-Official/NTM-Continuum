package com.hbm.blocks.generic;

import com.hbm.blocks.machine.storage.BlockCrateSupply;
import com.hbm.items.food.ItemConserve;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.deco.TileEntityCanCrate;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatBlock;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code BlockCanCrate} (crate_can, the conserve crate): a wooden crate of tinned food. A crowbar breaks it
 * open with the crate-break sound and spills 5-8 random cans - each of the canned conserves, the soda cans and
 * pudding equally likely; mined, it drops itself. Drawn from {@code conservecrate.obj} in its own texture.
 */
public class BlockCanCrate extends CompatBlock implements EntityBlock {

    public BlockCanCrate(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCanCrate(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.is(BlockCrateSupply.CROWBAR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            dropContents(level, pos);
            level.removeBlock(pos, false);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack item : getContents(level.getRandom())) {
            if (!item.isEmpty()) popResource(level, pos, item);
        }
    }

    public List<ItemStack> getContents(RandomSource rand) {
        List<ItemStack> ret = new ArrayList<>();
        int count = getContentAmount(rand);
        for (int i = 0; i < count; i++) ret.add(getRandomItem(rand));
        return ret;
    }

    /** One of the 1.7 list, uniformly; an entry this port does not register keeps its share as nothing. */
    public ItemStack getRandomItem(RandomSource rand) {
        List<String> items = new ArrayList<>();
        for (ItemConserve.EnumFoodType type : ItemConserve.EnumFoodType.values())
            items.add("canned_conserve_" + type.name().toLowerCase(Locale.US));
        items.add("can_smart");
        items.add("can_creature");
        items.add("can_redbomb");
        items.add("can_mrsugar");
        items.add("can_overcharge");
        items.add("can_luna");
        items.add("can_breen");
        items.add("can_bepis");
        items.add("pudding");
        return stackOf(items.get(rand.nextInt(items.size())));
    }

    public int getContentAmount(RandomSource rand) {
        return 5 + rand.nextInt(4);
    }

    private static ItemStack stackOf(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}

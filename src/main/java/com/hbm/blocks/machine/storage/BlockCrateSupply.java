package com.hbm.blocks.machine.storage;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.storage.TileEntityCrateSupply;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockSupplyCrate}: the crate the parachute drop places, no GUI, contents carried by the
 * dropped item. The crowbar is 1.7's only way into a placed one; the shared {@code hbm:crowbar} tag keeps
 * this interaction aligned with the other legacy loot crates.
 */
public class BlockCrateSupply extends CarriedBlock {

    public static final TagKey<Item> CROWBAR = TagKey.create(Registries.ITEM, dev.golem.core.Ids.of(com.hbm.Constants.MOD_ID, "crowbar"));

    public BlockCrateSupply(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCrateSupply(pos, state);
    }

    /** 1.7 BlockSupplyCrate:79-90: the crowbar spills the crate and destroys it. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.is(CROWBAR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof Container crate) Containers.dropContents(level, pos, crate);
            level.destroyBlock(pos, false);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}

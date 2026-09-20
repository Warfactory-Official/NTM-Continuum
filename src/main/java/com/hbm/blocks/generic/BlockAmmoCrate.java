package com.hbm.blocks.generic;

import com.hbm.blocks.machine.storage.BlockCrateSupply;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Random;

/**
 * 1.7 {@code BlockAmmoCrate} ("Starmetal Crate"): a crowbar spills a randomised ammunition hoard and
 * breaks the crate. Metadata ammo subtypes are the port's flattened {@code ammo_standard_<type>} ids.
 */
public class BlockAmmoCrate extends CompatBlock {

    private final Random rand = new Random();

    public BlockAmmoCrate(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.is(BlockCrateSupply.CROWBAR)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            for (ItemStack stack : getContents()) {
                if (!stack.isEmpty()) Block.popResource(level, pos, stack);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    static ItemStack stack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public ArrayList<ItemStack> getContents() {
        ArrayList<ItemStack> ret = new ArrayList<>();
        ret.add(stack("cap_nuka", 12 + rand.nextInt(21)));
        ret.add(stack("syringe_metal_stimpak", 1 + rand.nextInt(3)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_p9_sp", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_p9_fmj", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_m357_sp", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_m357_fmj", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_m44_sp", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_m44_fmj", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_r556_sp", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_r556_fmj", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_r762_sp", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_r762_fmj", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_g12", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_g12_slug", 16 + rand.nextInt(17)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_g40_he", 2 + rand.nextInt(3)));
        if (rand.nextBoolean()) ret.add(stack("ammo_standard_rocket_he", 2 + rand.nextInt(3)));
        if (rand.nextInt(10) == 0) ret.add(stack("syringe_metal_super", 2));
        return ret;
    }
}

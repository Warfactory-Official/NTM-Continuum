package com.hbm.world.feature;

import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import net.minecraft.util.RandomSource;

/**
 * 1.7 {@code com.hbm.world.feature.Dud}: a crashed bomb of a random dud type (balefire, conventional, nuke, salted) on
 * the surface, on {@link LegacyDungeonWorld}. A structure in the port ({@code hbm:dud}), so {@code /locate} finds it.
 */
public class Dud {

	/** 1.7 {@code BlockCrashedBomb.EnumDudType.values().length}. */
	private static final int DUD_TYPES = 4;

	protected LegacyBlock[] GetValidSpawnBlocks() {
		return new LegacyBlock[] {
			Blocks.grass,
			Blocks.dirt,
			Blocks.stone,
			Blocks.sand,
			Blocks.sandstone,
		};
	}

	public boolean LocationIsValidSpawn(LegacyDungeonWorld world, int x, int y, int z) {

		LegacyBlock checkBlock = world.getBlock(x, y - 1, z);
		LegacyBlock blockAbove = world.getBlock(x, y, z);
		LegacyBlock blockBelow = world.getBlock(x, y - 2, z);

		for(LegacyBlock i : GetValidSpawnBlocks()) {
			if(blockAbove != Blocks.air) {
				return false;
			}
			if(checkBlock == i) {
				return true;
			} else if(checkBlock == Blocks.snow_layer && blockBelow == i) {
				return true;
			} else if(LegacyDungeonWorld.isPlant(checkBlock) && blockBelow == i) {
				return true;
			}
		}
		return false;
	}

	public boolean generate(LegacyDungeonWorld world, RandomSource rand, int x, int y, int z) {
		int i = rand.nextInt(1);

		if(i == 0) {
			generate_r0(world, rand, x, y, z);
		}

		return true;

	}

	public boolean generate_r0(LegacyDungeonWorld world, RandomSource rand, int x, int y, int z) {
		if(!LocationIsValidSpawn(world, x, y, z))
			return false;

		world.setBlock(x, y, z, ModBlocks.crashed_balefire, rand.nextInt(DUD_TYPES), 3);

		return true;

	}
}

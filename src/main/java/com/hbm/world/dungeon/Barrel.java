// Ported 1:1 from original-hbm (1.7) com.hbm.world.dungeon.Barrel ("Schematic to java Structure by jajo_11").
// The builder runs on LegacyDungeonWorld, 1.7's World made deterministic (see that class); the block writes are 1.7's,
// unchanged. Machine-converted from the 1.7 source: only the 1.7 API calls without a virtual-world twin were rewritten
// (tile-entity access, ItemDoor, Library.getRandomConcrete, Material tests, debug prints).

package com.hbm.world.dungeon;

import com.hbm.itempool.ItemPools;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import net.minecraft.util.RandomSource;




public class Barrel {
	
	protected LegacyBlock[] GetValidSpawnBlocks() {
		
		return new LegacyBlock[] {
				Blocks.grass,
				Blocks.dirt,
				Blocks.sand,
				Blocks.stone,
				Blocks.sandstone
			};
	}

	public boolean LocationIsValidSpawn(LegacyDungeonWorld world, int x, int y, int z) {

		LegacyBlock checkBlock = world.getBlock(x, y - 1, z);
		LegacyBlock blockAbove = world.getBlock(x, y, z);
		LegacyBlock blockBelow = world.getBlock(x, y - 2, z);

		for (LegacyBlock i : GetValidSpawnBlocks()) {
			if (blockAbove != Blocks.air) {
				return false;
			}
			if (checkBlock == i) {
				return true;
			} else if (checkBlock == Blocks.snow_layer && blockBelow == i) {
				return true;
			} else if (LegacyDungeonWorld.isPlant(checkBlock) && blockBelow == i) {
				return true;
			}
		}
		return false;
	}

	public boolean generate(LegacyDungeonWorld world, RandomSource rand, int x, int y, int z) {
		int i = rand.nextInt(1);

		if (i == 0) {
			generate_r0(world, rand, x, y, z);
		}

		return true;

	}
	
	LegacyBlock Block1 = ModBlocks.reinforced_brick;
	LegacyBlock Block2 = ModBlocks.sellafield_slaked;
	LegacyBlock Block3 = ModBlocks.brick_concrete;
	LegacyBlock sellafield = ModBlocks.sellafield;
	LegacyBlock Block10 = ModBlocks.deco_lead;
	LegacyBlock Block11 = ModBlocks.reinforced_glass;
	LegacyBlock Block12 = ModBlocks.toxic_block;

	public boolean generate_r0(LegacyDungeonWorld world, RandomSource rand, int x, int y, int z) {
		if (!LocationIsValidSpawn(world, x, y, z) || !LocationIsValidSpawn(world, x + 4, y, z)
				|| !LocationIsValidSpawn(world, x + 4, y, z + 6) || !LocationIsValidSpawn(world, x, y, z + 6)) {
			return false;
		}

		world.setBlock(x + 1, y + -1, z + 0, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 0, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 0, Block1, 0, 3);
		world.setBlock(x + 0, y + -1, z + 1, Block1, 0, 3);
		world.setBlock(x + 1, y + -1, z + 1, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 1, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 1, Block1, 0, 3);
		world.setBlock(x + 4, y + -1, z + 1, Block1, 0, 3);
		world.setBlock(x + 0, y + -1, z + 2, Block1, 0, 3);
		world.setBlock(x + 1, y + -1, z + 2, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 2, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 2, Block1, 0, 3);
		world.setBlock(x + 4, y + -1, z + 2, Block1, 0, 3);
		world.setBlock(x + 0, y + -1, z + 3, Block1, 0, 3);
		world.setBlock(x + 1, y + -1, z + 3, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 3, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 3, Block1, 0, 3);
		world.setBlock(x + 4, y + -1, z + 3, Block1, 0, 3);
		world.setBlock(x + 1, y + -1, z + 4, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 4, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 4, Block1, 0, 3);
		world.setBlock(x + 1, y + -1, z + 5, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 5, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 5, Block1, 0, 3);
		world.setBlock(x + 1, y + -1, z + 6, Block1, 0, 3);
		world.setBlock(x + 2, y + -1, z + 6, Block1, 0, 3);
		world.setBlock(x + 3, y + -1, z + 6, Block1, 0, 3);
		world.setBlock(x + 1, y + 0, z + 0, Block2, 0, 3);
		world.setBlock(x + 2, y + 0, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 0, z + 0, Block2, 0, 3);
		world.setBlock(x + 0, y + 0, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 0, z + 1, sellafield, 3, 3);
		world.setBlock(x + 2, y + 0, z + 1, sellafield, 4, 3);
		world.setBlock(x + 3, y + 0, z + 1, sellafield, 3, 3);
		world.setBlock(x + 4, y + 0, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 0, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 0, z + 2, sellafield, 4, 3);
		world.setBlock(x + 2, y + 0, z + 2, sellafield, 5, 3);
		
		// 1.7 set a TileEntitySellafield radius of 2.5 here; 1.7's sellafield has no tile entity, so it never applied
		
		world.setBlock(x + 3, y + 0, z + 2, sellafield, 4, 3);
		world.setBlock(x + 4, y + 0, z + 2, Block2, 0, 3);
		world.setBlock(x + 0, y + 0, z + 3, Block2, 0, 3);
		world.setBlock(x + 1, y + 0, z + 3, sellafield, 4, 3);
		world.setBlock(x + 2, y + 0, z + 3, sellafield, 3, 3);
		world.setBlock(x + 3, y + 0, z + 3, sellafield, 4, 3);
		world.setBlock(x + 4, y + 0, z + 3, Block2, 0, 3);
		world.setBlock(x + 1, y + 0, z + 4, Block2, 0, 3);
		world.setBlock(x + 2, y + 0, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 0, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 1, z + 0, Block2, 0, 3);
		world.setBlock(x + 2, y + 1, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 1, z + 0, Block2, 0, 3);
		world.setBlock(x + 0, y + 1, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 1, z + 1, sellafield, 2, 3);
		world.setBlock(x + 2, y + 1, z + 1, sellafield, 3, 3);
		world.setBlock(x + 3, y + 1, z + 1, sellafield, 3, 3);
		world.setBlock(x + 4, y + 1, z + 1, Block2, 0, 3);
		world.setBlock(x + 0, y + 1, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 1, z + 2, sellafield, 3, 3);
		
		/*world.setBlock(x + 2, y + 1, z + 2, Blocks.chest, 3, 3);

		if(world.getBlock(x + 2, y + 1, z + 2) == Blocks.chest)
		{
			WeightedRandomChestContent.generateChestContents(rand, HbmChestContents.getLoot(3), (TileEntityChest)world.getTileEntity(x + 2, y + 1, z + 2), 16);
		}*/

		world.setBlock(x + 2, y + 1, z + 2, ModBlocks.crate_steel, 0, 3);

		if(world.getBlock(x + 2, y + 1, z + 2) == ModBlocks.crate_steel)
		{
			world.generateChestContents(rand, ItemPools.POOL_EXPENSIVE, x + 2, y + 1, z + 2, 16);
		}
		
		world.setBlock(x + 3, y + 1, z + 2, sellafield, 3, 3);
		world.setBlock(x + 4, y + 1, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 1, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 1, z + 3, sellafield, 3, 3);
		world.setBlock(x + 2, y + 1, z + 3, sellafield, 2, 3);
		world.setBlock(x + 3, y + 1, z + 3, sellafield, 3, 3);
		world.setBlock(x + 4, y + 1, z + 3, Block2, 0, 3);
		world.setBlock(x + 1, y + 1, z + 4, Block2, 0, 3);
		world.setBlock(x + 2, y + 1, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 1, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 2, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 2, z + 0, Block2, 0, 3);
		world.setBlock(x + 3, y + 2, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 2, z + 1, Block2, 0, 3);
		world.setBlock(x + 1, y + 2, z + 1, sellafield, 1, 3);
		world.setBlock(x + 2, y + 2, z + 1, sellafield, 2, 3);
		world.setBlock(x + 3, y + 2, z + 1, sellafield, 2, 3);
		world.setBlock(x + 4, y + 2, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 2, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 2, z + 2, sellafield, 2, 3);
		world.setBlock(x + 2, y + 2, z + 2, sellafield, 4, 3);
		world.setBlock(x + 3, y + 2, z + 2, sellafield, 2, 3);
		world.setBlock(x + 4, y + 2, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 2, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 2, z + 3, sellafield, 2, 3);
		world.setBlock(x + 2, y + 2, z + 3, sellafield, 1, 3);
		world.setBlock(x + 3, y + 2, z + 3, sellafield, 2, 3);
		world.setBlock(x + 4, y + 2, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 2, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 2, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 2, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 3, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 3, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 3, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 3, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 3, z + 1, sellafield, 1, 3);
		world.setBlock(x + 2, y + 3, z + 1, sellafield, 1, 3);
		world.setBlock(x + 3, y + 3, z + 1, sellafield, 1, 3);
		world.setBlock(x + 4, y + 3, z + 1, Block2, 0, 3);
		world.setBlock(x + 0, y + 3, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 3, z + 2, sellafield, 1, 3);
		world.setBlock(x + 2, y + 3, z + 2, sellafield, 3, 3);
		world.setBlock(x + 3, y + 3, z + 2, sellafield, 1, 3);
		world.setBlock(x + 4, y + 3, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 3, z + 3, Block2, 0, 3);
		world.setBlock(x + 1, y + 3, z + 3, sellafield, 1, 3);
		world.setBlock(x + 2, y + 3, z + 3, sellafield, 0, 3);
		world.setBlock(x + 3, y + 3, z + 3, sellafield, 1, 3);
		world.setBlock(x + 4, y + 3, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 3, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 3, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 3, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 4, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 4, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 4, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 4, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 4, z + 1, sellafield, 0, 3);
		world.setBlock(x + 2, y + 4, z + 1, sellafield, 1, 3);
		world.setBlock(x + 3, y + 4, z + 1, sellafield, 0, 3);
		world.setBlock(x + 4, y + 4, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 4, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 4, z + 2, sellafield, 0, 3);
		world.setBlock(x + 2, y + 4, z + 2, sellafield, 2, 3);
		world.setBlock(x + 3, y + 4, z + 2, sellafield, 1, 3);
		world.setBlock(x + 4, y + 4, z + 2, Block2, 0, 3);
		world.setBlock(x + 0, y + 4, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 4, z + 3, sellafield, 1, 3);
		world.setBlock(x + 2, y + 4, z + 3, sellafield, 0, 3);
		world.setBlock(x + 3, y + 4, z + 3, sellafield, 0, 3);
		world.setBlock(x + 4, y + 4, z + 3, Block2, 0, 3);
		world.setBlock(x + 1, y + 4, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 4, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 4, z + 4, Block2, 0, 3);
		world.setBlock(x + 1, y + 5, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 5, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 5, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 5, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 5, z + 1, sellafield, 0, 3);
		world.setBlock(x + 2, y + 5, z + 1, sellafield, 0, 3);
		world.setBlock(x + 3, y + 5, z + 1, sellafield, 0, 3);
		world.setBlock(x + 4, y + 5, z + 1, Block2, 0, 3);
		world.setBlock(x + 0, y + 5, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 5, z + 2, Block12, 0, 3);
		world.setBlock(x + 2, y + 5, z + 2, sellafield, 1, 3);
		world.setBlock(x + 3, y + 5, z + 2, Block12, 0, 3);
		world.setBlock(x + 4, y + 5, z + 2, Blocks.air, 0, 3);
		world.setBlock(x + 0, y + 5, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 5, z + 3, sellafield, 0, 3);
		world.setBlock(x + 2, y + 5, z + 3, sellafield, 0, 3);
		world.setBlock(x + 3, y + 5, z + 3, Block12, 0, 3);
		world.setBlock(x + 4, y + 5, z + 3, Block2, 0, 3);
		world.setBlock(x + 1, y + 5, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 5, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 5, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 6, z + 0, Block10, 0, 3);
		world.setBlock(x + 2, y + 6, z + 0, Block10, 0, 3);
		world.setBlock(x + 3, y + 6, z + 0, Block10, 0, 3);
		world.setBlock(x + 0, y + 6, z + 1, Block10, 0, 3);
		world.setBlock(x + 1, y + 6, z + 1, Block12, 0, 3);
		world.setBlock(x + 2, y + 6, z + 1, Block12, 0, 3);
		world.setBlock(x + 3, y + 6, z + 1, Block12, 0, 3);
		world.setBlock(x + 4, y + 6, z + 1, Block10, 0, 3);
		world.setBlock(x + 0, y + 6, z + 2, Block10, 0, 3);
		world.setBlock(x + 1, y + 6, z + 2, Block12, 0, 3);
		world.setBlock(x + 2, y + 6, z + 2, sellafield, 0, 3);
		world.setBlock(x + 3, y + 6, z + 2, Block12, 0, 3);
		world.setBlock(x + 4, y + 6, z + 2, Block10, 0, 3);
		world.setBlock(x + 0, y + 6, z + 3, Block10, 0, 3);
		world.setBlock(x + 1, y + 6, z + 3, Block12, 0, 3);
		world.setBlock(x + 2, y + 6, z + 3, Block12, 0, 3);
		world.setBlock(x + 3, y + 6, z + 3, Block12, 0, 3);
		world.setBlock(x + 4, y + 6, z + 3, Block10, 0, 3);
		world.setBlock(x + 1, y + 6, z + 4, Block10, 0, 3);
		world.setBlock(x + 2, y + 6, z + 4, Block10, 0, 3);
		world.setBlock(x + 3, y + 6, z + 4, Block10, 0, 3);
		world.setBlock(x + 1, y + 7, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 7, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 7, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 7, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 7, z + 1, Block12, 0, 3);
		world.setBlock(x + 2, y + 7, z + 1, Block12, 0, 3);
		world.setBlock(x + 3, y + 7, z + 1, Block12, 0, 3);
		world.setBlock(x + 4, y + 7, z + 1, Block2, 0, 3);
		world.setBlock(x + 0, y + 7, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 7, z + 2, Block12, 0, 3);
		world.setBlock(x + 2, y + 7, z + 2, Block12, 0, 3);
		world.setBlock(x + 3, y + 7, z + 2, Block12, 0, 3);
		world.setBlock(x + 4, y + 7, z + 2, Block2, 0, 3);
		world.setBlock(x + 0, y + 7, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 7, z + 3, Block12, 0, 3);
		world.setBlock(x + 2, y + 7, z + 3, Block12, 0, 3);
		world.setBlock(x + 3, y + 7, z + 3, Block12, 0, 3);
		world.setBlock(x + 4, y + 7, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 7, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 7, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 7, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 8, z + 0, Block10, 0, 3);
		world.setBlock(x + 2, y + 8, z + 0, Block10, 0, 3);
		world.setBlock(x + 3, y + 8, z + 0, Block10, 0, 3);
		world.setBlock(x + 0, y + 8, z + 1, Block10, 0, 3);
		world.setBlock(x + 1, y + 8, z + 1, Block12, 0, 3);
		world.setBlock(x + 2, y + 8, z + 1, Block12, 0, 3);
		world.setBlock(x + 3, y + 8, z + 1, Block12, 0, 3);
		world.setBlock(x + 4, y + 8, z + 1, Block10, 0, 3);
		world.setBlock(x + 0, y + 8, z + 2, Block10, 0, 3);
		world.setBlock(x + 1, y + 8, z + 2, Block12, 0, 3);
		world.setBlock(x + 2, y + 8, z + 2, Block12, 0, 3);
		world.setBlock(x + 3, y + 8, z + 2, Block12, 0, 3);
		world.setBlock(x + 4, y + 8, z + 2, Block10, 0, 3);
		world.setBlock(x + 0, y + 8, z + 3, Block10, 0, 3);
		world.setBlock(x + 1, y + 8, z + 3, Block12, 0, 3);
		world.setBlock(x + 2, y + 8, z + 3, Block12, 0, 3);
		world.setBlock(x + 3, y + 8, z + 3, Block12, 0, 3);
		world.setBlock(x + 4, y + 8, z + 3, Block10, 0, 3);
		world.setBlock(x + 1, y + 8, z + 4, Block10, 0, 3);
		world.setBlock(x + 2, y + 8, z + 4, Block10, 0, 3);
		world.setBlock(x + 3, y + 8, z + 4, Block10, 0, 3);
		world.setBlock(x + 1, y + 9, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 9, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 9, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 9, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 9, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 9, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 9, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 9, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 9, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 9, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 9, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 9, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 10, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 10, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 10, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 10, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 10, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 10, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 10, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 10, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 10, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 10, z + 4, world.getRandomConcrete(), 0, 3);
		//world.setBlock(x + 2, y + 10, z + 4, Blocks.iron_door, 2, 3);
		world.setBlock(x + 3, y + 10, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 11, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 11, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 11, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 11, z + 1, Block11, 0, 3);
		world.setBlock(x + 0, y + 11, z + 2, Block11, 0, 3);
		world.setBlock(x + 4, y + 11, z + 2, Block11, 0, 3);
		world.setBlock(x + 0, y + 11, z + 3, Block11, 0, 3);
		world.setBlock(x + 4, y + 11, z + 3, Block11, 0, 3);
		world.setBlock(x + 1, y + 11, z + 4, world.getRandomConcrete(), 0, 3);
		//world.setBlock(x + 2, y + 11, z + 4, Blocks.iron_door, 8, 3);
        world.placeDoorBlock(x + 2, y + 10, z + 4, 2, Blocks.iron_door);
		world.setBlock(x + 3, y + 11, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 12, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 12, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 12, z + 0, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 12, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 12, z + 1, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 12, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 12, z + 2, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 0, y + 12, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 4, y + 12, z + 3, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 12, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 2, y + 12, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 3, y + 12, z + 4, world.getRandomConcrete(), 0, 3);
		world.setBlock(x + 1, y + 13, z + 0, Block1, 0, 3);
		world.setBlock(x + 2, y + 13, z + 0, Block1, 0, 3);
		world.setBlock(x + 3, y + 13, z + 0, Block1, 0, 3);
		world.setBlock(x + 0, y + 13, z + 1, Block1, 0, 3);
		world.setBlock(x + 1, y + 13, z + 1, Block10, 0, 3);
		world.setBlock(x + 2, y + 13, z + 1, Block10, 0, 3);
		world.setBlock(x + 3, y + 13, z + 1, Block10, 0, 3);
		world.setBlock(x + 4, y + 13, z + 1, Block1, 0, 3);
		world.setBlock(x + 0, y + 13, z + 2, Block1, 0, 3);
		world.setBlock(x + 1, y + 13, z + 2, Block10, 0, 3);
		world.setBlock(x + 2, y + 13, z + 2, Block10, 0, 3);
		world.setBlock(x + 4, y + 13, z + 2, Block1, 0, 3);
		world.setBlock(x + 0, y + 13, z + 3, Block1, 0, 3);
		world.setBlock(x + 1, y + 13, z + 3, Block10, 0, 3);
		world.setBlock(x + 2, y + 13, z + 3, Block10, 0, 3);
		world.setBlock(x + 1, y + 13, z + 4, Block1, 0, 3);
		world.setBlock(x + 2, y + 13, z + 4, Block1, 0, 3);
		world.setBlock(x + 3, y + 13, z + 4, Block1, 0, 3);

		generate_r02_last(world, rand, x, y, z);
		return true;

	}

	public boolean generate_r02_last(LegacyDungeonWorld world, RandomSource rand, int x, int y, int z) {

		world.setBlock(x + 2, y + 0, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 1, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 2, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 3, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 4, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 5, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 6, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 7, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 8, z + 5, Blocks.ladder, 3, 3);
		world.setBlock(x + 2, y + 9, z + 5, Blocks.ladder, 3, 3);
		
		return true;

	}

}
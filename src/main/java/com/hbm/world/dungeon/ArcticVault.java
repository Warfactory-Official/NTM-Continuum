package com.hbm.world.dungeon;

import com.hbm.blocks.generic.BlockBobble;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.generator.DungeonToolbox;
import com.hbm.world.generator.DungeonToolbox.MetaBlock;

import java.util.Arrays;
import java.util.List;

/**
 * 1.7 {@code com.hbm.world.dungeon.ArcticVault}: the "arctic code vault", an 11 x 7 x 11 stone-brick room buried at
 * y 16..47 under a cold biome, holding tape recorders, snow, cobwebs, crates and one bobblehead, with a tape recorder on
 * the surface above it. On {@link LegacyDungeonWorld}.
 *
 * <p>1.7's biome test ({@code getFloatTemperature(x, y, z) < 0.2} at y &lt;= 47, where the height adjustment does not
 * apply) is the structure's biome tag in the port; the rock test is here.
 */
public class ArcticVault {

	public void trySpawn(LegacyDungeonWorld world, int x, int y, int z) {

		y--;

		// 1.7: biome.getFloatTemperature(x, y, z) < 0.2 && world.getBlock(x, y, z).getMaterial() == Material.rock
		if(world.getBlock(x, y, z) == Blocks.stone) {
			build(world, x, y, z);
		}

	}

	private void build(LegacyDungeonWorld world, int x, int y, int z) {

		List<MetaBlock> brick = Arrays.asList(new MetaBlock[] {new MetaBlock(Blocks.stonebrick), new MetaBlock(Blocks.stonebrick, 2)});
		List<MetaBlock> web = Arrays.asList(new MetaBlock[] {new MetaBlock(Blocks.air), new MetaBlock(Blocks.air), new MetaBlock(Blocks.web)});
		List<MetaBlock> crates = Arrays.asList(new MetaBlock[] {new MetaBlock(ModBlocks.crate), new MetaBlock(ModBlocks.crate_metal), new MetaBlock(ModBlocks.crate_ammo), new MetaBlock(ModBlocks.crate_can), new MetaBlock(ModBlocks.crate_jungle)});

		DungeonToolbox.generateBox(world, x - 5, y, z - 5, 11, 1, 11, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 6, z - 5, 11, 1, 11, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 1, z - 5, 11, 5, 1, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 1, z + 5, 11, 5, 1, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 1, z - 5, 1, 5, 11, brick);
		DungeonToolbox.generateBox(world, x + 5, y + 1, z - 5, 1, 5, 11, brick);
		DungeonToolbox.generateBox(world, x - 4, y + 1, z - 4, 9, 3, 9, Blocks.air);
		DungeonToolbox.generateBox(world, x - 4, y + 1, z - 4, 9, 1, 9, new MetaBlock(Blocks.snow_layer));
		DungeonToolbox.generateBox(world, x - 2, y + 1, z - 2, 5, 2, 1, new MetaBlock(ModBlocks.tape_recorder, 3));
		DungeonToolbox.generateBox(world, x - 2, y + 3, z - 2, 5, 1, 1, new MetaBlock(Blocks.snow_layer));
		DungeonToolbox.generateBox(world, x - 2, y + 1, z + 2, 5, 2, 1, new MetaBlock(ModBlocks.tape_recorder, 2));
		DungeonToolbox.generateBox(world, x - 2, y + 3, z + 2, 5, 1, 1, new MetaBlock(Blocks.snow_layer));
		DungeonToolbox.generateBox(world, x - 4, y + 4, z - 4, 9, 2, 9, web);

		for(int i = 0; i < 15; i++) {
			int ix = x - 4 + world.rand.nextInt(10);
			int iz = z - 4 + world.rand.nextInt(10);

			if(world.getBlock(ix, y + 1, iz) == Blocks.snow_layer) {

				if(i == 0) {
					world.setBlock(ix, y + 1, iz, ModBlocks.bobblehead, world.rand.nextInt(16), 3);
					// bobble.type = BobbleType.values()[world.rand.nextInt(BobbleType.values().length - 1) + 1]
					int type = world.rand.nextInt(BlockBobble.BobbleType.VALUES.length - 1) + 1;
					world.onTile(ix, y + 1, iz, be -> {
						if(be instanceof TileEntityBobble bobble) bobble.setTypeOrdinal(type);
					});

				} else {
					MetaBlock b = DungeonToolbox.getRandom(crates, world.rand);
					world.setBlock(ix, y + 1, iz, b.block(), b.meta(), 2);
					world.setBlock(ix, y + 2, iz, Blocks.snow_layer);
				}
			}
		}

		int iy = world.getHeightValue(x, z);

		// 1.7: world.getBlock(x, iy - 1, z).canPlaceTorchOnTop(world, x, iy - 1, z) - a solid top; the terrain's surface is
		if(LegacyDungeonWorld.isSolid(world.getBlock(x, iy - 1, z))) {
			world.setBlock(x, iy, z, ModBlocks.tape_recorder);
		}
	}
}

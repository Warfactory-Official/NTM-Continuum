package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import dev.golem.core.data.Inventories;
import dev.golem.core.world.CompatBlockItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/** Regression coverage for HBM behavior routed through cross-version Core facades. */
public final class CompatibilityBoundaryGameTests {

    private CompatibilityBoundaryGameTests() {}

    public static void foundryRemovalScraps(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModMachineBlocks.foundry_channel.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityFoundryChannel,
                "foundry channel did not create its block entity");
        TileEntityFoundryChannel channel = (TileEntityFoundryChannel) helper.getLevel().getBlockEntity(pos);
        channel.type = Mats.MAT_IRON;
        channel.amount = MaterialShapes.INGOT.quantity * 2;

        helper.getLevel().destroyBlock(pos, false);
        Mats.MaterialStack dropped = onlyScrap(helper, pos);
        helper.assertTrue(dropped != null && dropped.material == Mats.MAT_IRON
                        && dropped.amount == MaterialShapes.INGOT.quantity * 2,
                "breaking a molten foundry channel did not preserve its exact material");
        helper.succeed();
    }

    public static void crucibleRemovalScraps(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModMachineBlocks.machine_crucible.get().defaultBlockState()
                , 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityCrucible,
                "crucible did not create its block entity");
        TileEntityCrucible crucible = (TileEntityCrucible) helper.getLevel().getBlockEntity(pos);
        crucible.setItem(0, new ItemStack(Items.COBBLESTONE));
        crucible.recipeStack.add(new Mats.MaterialStack(Mats.MAT_IRON, MaterialShapes.INGOT.quantity));
        crucible.wasteStack.add(new Mats.MaterialStack(Mats.MAT_COPPER, MaterialShapes.NUGGET.quantity * 3));

        helper.getLevel().destroyBlock(pos, false);
        List<Mats.MaterialStack> scraps = scraps(helper, pos);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2D));
        helper.assertTrue(scraps.size() == 2
                        && scraps.stream().anyMatch(stack -> stack.material == Mats.MAT_IRON
                        && stack.amount == MaterialShapes.INGOT.quantity)
                        && scraps.stream().anyMatch(stack -> stack.material == Mats.MAT_COPPER
                        && stack.amount == MaterialShapes.NUGGET.quantity * 3)
                        && drops.stream().filter(drop -> drop.getItem().is(Items.COBBLESTONE))
                        .mapToInt(drop -> drop.getItem().getCount()).sum() == 1,
                "breaking a crucible duplicated or lost ordinary/molten contents");
        helper.succeed();
    }

    public static void crateCarriedRoundTrip(GameTestHelper helper) {
        BlockPos source = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(source, ModStorageBlocks.crate_iron.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(source) instanceof TileEntityCrate,
                "iron crate did not create its block entity");
        TileEntityCrate crate = (TileEntityCrate) helper.getLevel().getBlockEntity(source);
        crate.setItem(0, new ItemStack(Items.DIAMOND, 3));
        crate.setCustomName("Audit Crate");

        helper.getLevel().destroyBlock(source, true);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(source).inflate(2D));
        List<ItemEntity> crateDrops = drops.stream()
                .filter(drop -> drop.getItem().is(ModStorageBlocks.crate_iron.get().asItem())).toList();
        helper.assertTrue(crateDrops.size() == 1
                        && drops.stream().noneMatch(drop -> drop.getItem().is(Items.DIAMOND)),
                "breaking a crate did not produce exactly one carried crate item");
        ItemStack carried = crateDrops.get(0).getItem().copy();
        crateDrops.get(0).discard();

        BlockPos support = helper.absolutePos(new BlockPos(5, 1, 2));
        helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        carried.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, carried, hit));
        helper.assertTrue(helper.getLevel().getBlockEntity(support.above()) instanceof TileEntityCrate,
                "carried crate item did not place its block entity");
        TileEntityCrate restored = (TileEntityCrate) helper.getLevel().getBlockEntity(support.above());
        helper.assertTrue(restored.getItem(0).is(Items.DIAMOND) && restored.getItem(0).getCount() == 3
                        && restored.hasCustomName() && restored.getName().equals("Audit Crate"),
                "crate contents or custom name did not survive break and replacement");
        helper.succeed();
    }

    public static void facadeContracts(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        var main = Inventories.main(player.getInventory());
        helper.assertTrue(main.size() == 36, "main inventory facade must expose exactly 36 slots");
        main.set(0, new ItemStack(Items.DIAMOND));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STICK));
        helper.assertTrue(player.getInventory().getItem(0).is(Items.DIAMOND)
                        && main.stream().noneMatch(stack -> stack.is(Items.STICK)),
                "main inventory facade copied slots or included equipment");
        helper.assertTrue(ModMachineBlocks.machine_mining_laser.get().asItem() instanceof CompatBlockItem,
                "mining laser item bypasses the legacy Forge renderer bridge");
        helper.succeed();
    }

    private static Mats.MaterialStack onlyScrap(GameTestHelper helper, BlockPos pos) {
        List<Mats.MaterialStack> scraps = scraps(helper, pos);
        return scraps.size() == 1 ? scraps.get(0) : null;
    }

    private static List<Mats.MaterialStack> scraps(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2D)).stream()
                .map(ItemEntity::getItem)
                .map(ItemScraps::getMats)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}

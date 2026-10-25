package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.packet.DuckPayload;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1.7's {@code PlayerInformPacket} notices: "Press O to Duck!" with its one duck, and the coal dust warning. */
public final class PlayerInformGameTests {

    private PlayerInformGameTests() {}

    private static final String CHANNEL = Constants.MOD_ID + ":player_inform";

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function DUCK =
            TESTS.function("inform_duck_once", 20, () -> PlayerInformGameTests::duckOnce);
    public static final GameTests.Function COAL_DUST =
            TESTS.function("inform_coal_dust_warning", 20, () -> PlayerInformGameTests::coalDust);

    public static void init() {}

    private static int ducks(ServerPlayer player) {
        return player.level().getEntitiesOfClass(EntityDuck.class, new AABB(player.blockPosition()).inflate(8)).size();
    }

    private static void duckOnce(GameTestHelper helper) {
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = mock.player();
            player.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
            mock.drainOutbound();
            DuckPayload.onJoin(player);
            helper.assertTrue(GameTestHelpers.golemPayloads(mock.drainOutbound(), CHANNEL) == 1,
                    "a player who never ducked is told to press O on login");

            DuckPayload.handle(player);
            helper.assertTrue(ducks(player) == 1, "the first request throws one duck, got " + ducks(player));
            helper.assertTrue(HbmPlayerProps.getData(player).hasDucked, "the first request sets hasDucked");

            DuckPayload.handle(player);
            helper.assertTrue(ducks(player) == 1, "a second request throws nothing, got " + ducks(player));
            mock.drainOutbound();
            DuckPayload.onJoin(player);
            helper.assertTrue(GameTestHelpers.golemPayloads(mock.drainOutbound(), CHANNEL) == 0,
                    "a player who ducked gets no notice");
            player.level().getEntitiesOfClass(EntityDuck.class, new AABB(player.blockPosition()).inflate(8))
                    .forEach(EntityDuck::discard);
            helper.succeed();
        }
    }

    private static void coalDust(GameTestHelper helper) {
        BlockPos gas = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(gas, ModTerrainBlocks.gas_coal.get().defaultBlockState(), 2);
        helper.getLevel().setBlock(gas.above(), ModTerrainBlocks.gas_coal.get().defaultBlockState(), 2);
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = mock.player();
            int bare = inDust(mock, gas);
            helper.assertTrue(HbmLivingProps.getBlackLung(player) > 0, "coal dust raises black lung without a mask");
            helper.assertTrue(bare > 0, "coal dust without a mask sends the warning");

            HbmLivingProps.setBlackLung(player, 0);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmorSetBItems.mask_rag.get()));
            int masked = inDust(mock, gas);
            helper.assertTrue(HbmLivingProps.getBlackLung(player) == 0, "a rag mask keeps the dust out");
            helper.assertTrue(masked == 0, "no warning behind a rag mask, got " + masked);
            helper.succeed();
        }
    }

    /** One server step in the dust: 1.21.1 checks inside blocks when the player moves, 26.1 in its tick. */
    private static int inDust(GameTestHelpers.MockServerPlayer mock, BlockPos gas) {
        ServerPlayer player = mock.player();
        player.setPos(gas.getX() + 0.5, gas.getY(), gas.getZ() + 0.5);
        mock.drainOutbound();
        player.move(MoverType.PLAYER, Vec3.ZERO);
        player.doTick();
        return GameTestHelpers.golemPayloads(mock.drainOutbound(), CHANNEL);
    }
}

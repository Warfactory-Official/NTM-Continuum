package com.hbm.gametest;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.entity.projectile.EntityBulletBaseMK4CL;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.packet.ProjectileStatePayload;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.world.chunk.ChunkLoadingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Focused contracts for the original gun projectile entity family, separate from gun-state tests. */
public final class GunProjectileEntityGameTests {

    private GunProjectileEntityGameTests() {}

    public static void projectileEntityRegistrations(GameTestHelper helper) {
        helper.assertTrue(ModEntities.BULLET_MK4_CL.get() != null, "MK4 chunkloading entity is missing");
        helper.assertTrue(ModEntities.BEAM_MK4.get() != null, "MK4 beam entity is missing");
        helper.assertTrue(ModEntities.CHEMICAL.get() != null, "chemical projectile entity is missing");
        helper.assertTrue(com.hbm.entity.NtmToolEntityCatalog.COIN.get() != null,
                "NI4NI coin entity is missing");
        helper.assertTrue(XFactoryAccelerator.ni4ni_arc != null
                        && "hbm:ni4ni_arc".equals(XFactoryAccelerator.ni4ni_arc.id.toString())
                        && BulletConfigs.get(XFactoryAccelerator.ni4ni_arc.id) == XFactoryAccelerator.ni4ni_arc
                        && BulletConfigs.byIndex(BulletConfigs.indexOf(XFactoryAccelerator.ni4ni_arc))
                        == XFactoryAccelerator.ni4ni_arc,
                "NI4NI beam config ID/index synchronization is broken");
        helper.succeed();
    }

    public static void chunkloadingBulletClaimLifecycle(GameTestHelper helper) {
        EntityBulletBaseMK4CL bullet = new EntityBulletBaseMK4CL(ModEntities.BULLET_MK4_CL.get(), helper.getLevel());
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        bullet.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(bullet);
        bullet.startChunkLoading();

        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        helper.assertTrue(ChunkLoadingManager.isForced(helper.getLevel(), chunkX, chunkZ),
                "chunkloading bullet did not claim its current chunk");
        bullet.discard();
        helper.assertFalse(ChunkLoadingManager.isForced(helper.getLevel(), chunkX, chunkZ),
                "removed chunkloading bullet leaked its claim");
        helper.succeed();
    }

    public static void beamStopsAtNearestBlock(GameTestHelper helper) {
        BlockPos wallRelative = new BlockPos(6, 2, 2);
        BlockPos wall = helper.absolutePos(wallRelative);
        helper.setBlock(wallRelative, Blocks.STONE);
        EntityBulletBeamBase beam = new EntityBulletBeamBase(helper.getLevel(), XFactory357.m357_sp, 10F);
        beam.setPos(wall.getX() - 5.5D, wall.getY() + 0.5D, wall.getZ() + 0.5D);
        beam.headingX = 20D;
        beam.headingY = 0D;
        beam.headingZ = 0D;
        beam.performHitscanExternal(20D);
        helper.assertTrue(beam.hasRenderState(), "beam did not retain its authoritative render state");
        helper.assertTrue(beam.beamLength > 4D && beam.beamLength < 6D,
                "beam length must stop at the wall, was " + beam.beamLength);
        ProjectileStatePayload state = ProjectileStatePayload.beam(beam);
        helper.assertTrue(state.beamLength() > 0D && state.headingX() != 0D && state.endpointX() > beam.getX(),
                "beam payload must carry a nonzero endpoint, was " + state);
        double dx = state.endpointX() - beam.getX();
        double dy = state.endpointY() - beam.getY();
        double dz = state.endpointZ() - beam.getZ();
        helper.assertTrue(Math.abs(Math.sqrt(dx * dx + dy * dy + dz * dz) - state.beamLength()) < 0.001D,
                "beam endpoint and length diverged in payload: " + state);
        double headingLength = Math.sqrt(state.headingX() * state.headingX()
                + state.headingY() * state.headingY() + state.headingZ() * state.headingZ());
        helper.assertTrue(headingLength > 0D
                        && Math.abs((dx * state.headingX() + dy * state.headingY() + dz * state.headingZ())
                        / (headingLength * state.beamLength()) - 1D) < 0.001D,
                "beam payload direction diverged from endpoint: " + state);
        helper.succeed();
    }

    public static void beamPayloadAppliesClientState(GameTestHelper helper) {
        EntityBulletBeamBase beam = new EntityBulletBeamBase(helper.getLevel(), XFactory357.m357_sp, 10F);
        beam.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        beam.headingX = 12D;
        beam.headingY = 0D;
        beam.headingZ = 0D;
        beam.performHitscanExternal(12D);
        ProjectileStatePayload payload = ProjectileStatePayload.beam(beam);

        payload.apply(helper.getLevel());
        helper.assertTrue(beam.hasRenderState() && beam.beamLength > 0D,
                "client-applied beam state lost its measured length");
        double direction = Math.sqrt(beam.headingX * beam.headingX + beam.headingY * beam.headingY
                + beam.headingZ * beam.headingZ);
        helper.assertTrue(direction > 0D && beam.beamEndX != beam.getX(),
                "client-applied beam state lost its direction or endpoint");
        helper.succeed();
    }

    public static void bulletRotationInterpolatesDisplacement(GameTestHelper helper) {
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(ModEntities.BULLET_MK4.get(), helper.getLevel());
        bullet.setBulletConfig(new com.hbm.items.weapon.sedna.BulletConfig()
                .setVel(1F).setLife(10).setSpectral(true).setImpactsEntities(false));
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        bullet.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        bullet.setYRot(0F);
        bullet.setXRot(0F);
        bullet.yRotO = 0F;
        bullet.xRotO = 0F;
        bullet.setDeltaMovement(1D, 1D, 0D);

        bullet.tick();

        float midpointYaw = bullet.yRotO + (bullet.getYRot() - bullet.yRotO) * 0.5F;
        float midpointPitch = bullet.xRotO + (bullet.getXRot() - bullet.xRotO) * 0.5F;
        helper.assertTrue(Math.abs(bullet.getYRot() - 90F) < 0.001F
                        && Math.abs(bullet.getXRot() - 45F) < 0.001F,
                "displacement must set +X/+Y angles, was " + bullet.getYRot() + "/" + bullet.getXRot());
        helper.assertTrue(Math.abs(bullet.yRotO) < 0.001F && Math.abs(bullet.xRotO) < 0.001F
                        && Math.abs(midpointYaw - 45F) < 0.001F && Math.abs(midpointPitch - 22.5F) < 0.001F,
                "previous/current angles must remain interpolable, previous " + bullet.yRotO + "/" + bullet.xRotO
                        + ", current " + bullet.getYRot() + "/" + bullet.getXRot());
        helper.succeed();
    }

    public static void chemicalFluidStyleAndLifetime(GameTestHelper helper) {
        EntityChemical chemical = new EntityChemical(ModEntities.CHEMICAL.get(), helper.getLevel())
                .setFluid(Fluids.CHLORINE);
        helper.assertTrue(chemical.style() == EntityChemical.ChemicalStyle.GAS,
                "chlorine must use the original gaseous chemical style");
        helper.assertTrue(chemical.getMaxAge() == 60, "gaseous chemical lifetime changed");
        helper.succeed();
    }
}

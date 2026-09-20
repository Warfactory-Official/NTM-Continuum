package com.hbm.gametest;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.albion.IParticleUser;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPABeamline;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import com.hbm.tileentity.machine.albion.TileEntityPAQuadrupole;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import com.hbm.tileentity.machine.albion.TileEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.TileEntityPASource.Particle;
import dev.golem.core.gametest.GameTestHelpers;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.InputStream;

/**
 * Behavioural contracts for the Albion particle accelerator. The beam is a server-side walk rather than
 * an entity, so most of these drive one component at a time with a hand-made {@link Particle} — that is
 * the same call sequence {@link TileEntityPASource#steppy()} makes, and it keeps each test inside one
 * gametest plot.
 */
public final class AlbionGameTests {

    private AlbionGameTests() {}

    /** The direction the beam travels for a machine placed facing north. */
    private static ForgeDirection beam() {
        return ForgeDirection.NORTH.getRotation(ForgeDirection.DOWN);
    }

    private static <T extends BlockEntity> T place(GameTestHelper helper, BlockPos relative, Block block, Class<T> type) {
        FoldedTestSupport.assemble(helper, (BlockMultiblockCore) block, relative);
        return GameTestHelpers.blockEntity(helper, relative, type);
    }

    /** Puts the machine at operating temperature with a full coolant charge, ready to pass a beam. */
    private static void chill(TileEntityCooledBase machine) {
        machine.coolantTanks[0].setFill(machine.coolantTanks[0].getMaxFill());
        machine.coolantTanks[1].setFill(0);
        machine.temperature = TileEntityCooledBase.temperature_target;
    }

    private static Particle particleAt(TileEntityPASource source, BlockPos pos, ForgeDirection dir) {
        return new Particle(source, pos, dir, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    // --- structure ---

    public static void structure(GameTestHelper helper) {
        ForgeDirection beam = beam();

        TileEntityPASource source = place(helper, new BlockPos(9, 3, 9), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        BlockPos core = source.getBlockPos();
        com.hbm.blocks.machine.albion.BlockPASource block = ModMachineBlocks.pa_source.get();

        helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), core)),
                "The particle source core must resolve to itself");
        helper.assertTrue(source.getConPos().size() == 10,
                "The particle source must expose ten connection points; got " + source.getConPos().size());

        for (DirPos connection : source.getConPos()) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX,
                    -connection.getDir().offsetY, -connection.getDir().offsetZ);
            helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == block,
                    "The particle source is missing the cell behind a port at " + cell);
            helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), cell)),
                    "A particle source cell at " + cell + " does not resolve to its core");
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir()),
                    "A particle source port does not accept a cable at " + cell);
        }

        BlockPos spawn = core.offset(beam.offsetX * 5, 0, beam.offsetZ * 5);
        helper.assertTrue(helper.getLevel().getBlockState(spawn).getBlock() != block,
                "The particle must be injected outside the source's own footprint");
        helper.succeed();
    }

    public static void beamlineCarries(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(14, 3, 6), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        TileEntityPABeamline beamline = place(helper, new BlockPos(5, 3, 6), ModMachineBlocks.pa_beamline.get(), TileEntityPABeamline.class);
        BlockPos core = beamline.getBlockPos();

        BlockPos entry = core.offset(-beam.offsetX, -beam.offsetY, -beam.offsetZ);
        Particle particle = particleAt(source, entry, beam);

        helper.assertTrue(beamline.canParticleEnter(particle, beam, entry),
                "The beamline must accept a particle arriving from behind it");
        helper.assertFalse(beamline.canParticleEnter(particle, beam.getOpposite(), entry),
                "The beamline must reject a particle travelling the wrong way");

        beamline.onEnter(particle, beam);
        helper.assertTrue(particle.distanceTraveled == 3,
                "The beamline must add three to the run length; got " + particle.distanceTraveled);
        helper.assertTrue(core.offset(beam.offsetX * 2, beam.offsetY * 2, beam.offsetZ * 2)
                        .equals(beamline.getExitPos(particle)),
                "The beamline must hand the particle two cells further along");
        helper.succeed();
    }

    public static void rfcAccelerates(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(18, 3, 6), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        TileEntityPARFC rfc = place(helper, new BlockPos(6, 3, 6), ModMachineBlocks.pa_rfc.get(), TileEntityPARFC.class);
        BlockPos core = rfc.getBlockPos();
        BlockPos entry = core.offset(-beam.offsetX * 4, -beam.offsetY * 4, -beam.offsetZ * 4);

        helper.assertTrue(rfc.canParticleEnter(particleAt(source, entry, beam), beam, entry),
                "The RF cavity must accept a particle at its four-cell inlet");

        Particle warm = particleAt(source, entry, beam);
        rfc.power = rfc.getMaxPower();
        rfc.onEnter(warm, beam);
        helper.assertTrue(source.state == PAState.CRASH_NOCOOL,
                "A warm RF cavity must crash the beam; state " + source.state);

        chill(rfc);
        Particle unpowered = particleAt(source, entry, beam);
        rfc.power = 0;
        rfc.onEnter(unpowered, beam);
        helper.assertTrue(source.state == PAState.CRASH_NOPOWER,
                "An unpowered RF cavity must crash the beam; state " + source.state);

        Particle good = particleAt(source, entry, beam);
        rfc.power = rfc.getMaxPower();
        rfc.onEnter(good, beam);
        helper.assertFalse(good.invalid, "A cooled, powered RF cavity must pass the beam");
        helper.assertTrue(good.momentum == TileEntityPARFC.momentumGain,
                "The RF cavity must add its momentum; got " + good.momentum);
        helper.assertTrue(good.defocus == TileEntityPARFC.defocusGain,
                "The RF cavity must add its defocus; got " + good.defocus);
        helper.assertTrue(rfc.power == rfc.getMaxPower() - TileEntityPARFC.usage,
                "The RF cavity must spend its usage per pass; power " + rfc.power);
        helper.succeed();
    }

    public static void quadrupoleFocuses(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(14, 3, 6), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        TileEntityPAQuadrupole quad = place(helper, new BlockPos(5, 3, 6), ModMachineBlocks.pa_quadrupole.get(), TileEntityPAQuadrupole.class);
        BlockPos core = quad.getBlockPos();
        BlockPos entry = core.offset(-beam.offsetX, -beam.offsetY, -beam.offsetZ);

        chill(quad);
        quad.power = quad.getMaxPower();

        Particle noCoil = particleAt(source, entry, beam);
        quad.onEnter(noCoil, beam);
        helper.assertTrue(source.state == PAState.CRASH_NOCOIL,
                "A quadrupole with no coil must crash the beam; state " + source.state);

        quad.inventory.setStackInSlot(1, new ItemStack(ModItems.pa_coil_gold.get()));

        Particle tooFast = particleAt(source, entry, beam);
        tooFast.momentum = 100_000;
        quad.onEnter(tooFast, beam);
        helper.assertTrue(source.state == PAState.CRASH_OVERSPEED,
                "A gold coil must crash a beam past its rating; state " + source.state);

        Particle good = particleAt(source, entry, beam);
        good.defocus(250);
        good.momentum = 1_000;
        long before = quad.power;
        quad.onEnter(good, beam);
        helper.assertFalse(good.invalid, "A coiled quadrupole must pass a beam inside its rating");
        helper.assertTrue(good.defocus == 150,
                "The quadrupole must remove its focus gain; defocus " + good.defocus);
        helper.assertTrue(before - quad.power == TileEntityPAQuadrupole.usage,
                "A beam inside the coil's window must draw the base usage; drew " + (before - quad.power));

        Particle slow = particleAt(source, entry, beam);
        quad.inventory.setStackInSlot(1, new ItemStack(ModItems.pa_coil_niobium.get()));
        quad.power = quad.getMaxPower();
        before = quad.power;
        quad.onEnter(slow, beam);
        helper.assertTrue(before - quad.power == TileEntityPAQuadrupole.usage * 10,
                "A beam under the coil's minimum must cost ten times as much; drew " + (before - quad.power));
        helper.succeed();
    }

    public static void dipoleSteers(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(14, 3, 6), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        TileEntityPADipole dipole = place(helper, new BlockPos(5, 3, 6), ModMachineBlocks.pa_dipole.get(), TileEntityPADipole.class);
        BlockPos core = dipole.getBlockPos();
        BlockPos entry = core.offset(-beam.offsetX, -beam.offsetY, -beam.offsetZ);

        helper.assertTrue(dipole.canParticleEnter(particleAt(source, entry, beam), beam, entry),
                "The dipole must accept a beam from any horizontal neighbour");

        chill(dipole);
        dipole.power = dipole.getMaxPower();
        dipole.inventory.setStackInSlot(1, new ItemStack(ModItems.pa_coil_chlorophyte.get()));
        dipole.threshold = 5_000;
        dipole.dirLower = 1; // east
        dipole.dirUpper = 2; // south

        Particle slow = particleAt(source, entry, beam);
        slow.momentum = 1_000;
        BlockPos slowExit = dipole.getExitPos(slow);
        helper.assertTrue(slow.dir == ForgeDirection.EAST,
                "Below the threshold the dipole must take its lower exit; got " + slow.dir);
        helper.assertTrue(core.offset(slow.dir.offsetX * 2, 0, slow.dir.offsetZ * 2).equals(slowExit),
                "The dipole must eject two cells along the chosen direction");

        Particle fast = particleAt(source, entry, beam);
        fast.momentum = 20_000;
        dipole.getExitPos(fast);
        helper.assertTrue(fast.dir == ForgeDirection.SOUTH,
                "Above the threshold the dipole must take its upper exit; got " + fast.dir);

        Particle turning = particleAt(source, entry, beam);
        turning.momentum = 20_000;
        turning.addDistance(40);
        dipole.onEnter(turning, beam);
        helper.assertTrue(turning.distanceTraveled == 0,
                "A steered beam must reset its straight-line distance; got " + turning.distanceTraveled);

        Particle inline = particleAt(source, entry, ForgeDirection.SOUTH);
        inline.momentum = 20_000;
        inline.addDistance(40);
        dipole.onEnter(inline, ForgeDirection.SOUTH);
        helper.assertTrue(inline.distanceTraveled == 43,
                "A beam passing straight through must keep its distance; got " + inline.distanceTraveled);
        helper.succeed();
    }

    public static void detectorRuns(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(18, 4, 7), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        TileEntityPADetector detector = place(helper, new BlockPos(7, 4, 7), ModMachineBlocks.pa_detector.get(), TileEntityPADetector.class);
        BlockPos core = detector.getBlockPos();
        BlockPos entry = core.offset(-beam.offsetX * 4, -beam.offsetY * 4, -beam.offsetZ * 4);

        helper.assertTrue(detector.canParticleEnter(particleAt(source, entry, beam), beam, entry),
                "The detector must accept a beam at its four-cell inlet");
        helper.assertTrue(detector.getExitPos(particleAt(source, entry, beam)) == null,
                "The detector must end the run rather than pass the beam on");

        chill(detector);
        detector.power = detector.getMaxPower();

        Particle defocused = collision(source, entry, beam);
        defocused.defocus(1);
        detector.onEnter(defocused, beam);
        helper.assertTrue(source.state == PAState.CRASH_DEFOCUS,
                "The detector must reject a beam that is not perfectly focused; state " + source.state);

        Particle slow = collision(source, entry, beam);
        slow.momentum = 10;
        detector.onEnter(slow, beam);
        helper.assertTrue(source.state == PAState.CRASH_UNDERSPEED,
                "A beam under the recipe's momentum must crash; state " + source.state);

        detector.inventory.setStackInSlot(1, new ItemStack(ModItems.particle_empty.get()));
        Particle good = collision(source, entry, beam);
        good.momentum = 300;
        detector.onEnter(good, beam);
        helper.assertTrue(source.state == PAState.SUCCESS,
                "A focused, fast enough beam must complete the recipe; state " + source.state);
        helper.assertTrue(detector.inventory.getStackInSlot(3).is(ModItems.particle_amat.get()),
                "The detector must produce antimatter from hydrogen and copper; got "
                        + detector.inventory.getStackInSlot(3));
        helper.assertTrue(detector.inventory.getStackInSlot(1).isEmpty(),
                "The detector must spend the empty capsule the product needs");

        Particle unknown = particleAt(source, entry, beam);
        unknown.input1 = new ItemStack(net.minecraft.world.item.Items.STONE);
        unknown.input2 = new ItemStack(net.minecraft.world.item.Items.STONE);
        unknown.momentum = 100_000;
        detector.onEnter(unknown, beam);
        helper.assertTrue(source.state == PAState.CRASH_NORECIPE,
                "An unmatched pair must crash with no recipe; state " + source.state);
        helper.succeed();
    }

    private static Particle collision(TileEntityPASource source, BlockPos pos, ForgeDirection dir) {
        return new Particle(source, pos, dir,
                new ItemStack(ModItems.particle_hydrogen.get()), new ItemStack(ModItems.particle_copper.get()));
    }

    public static void sourceFires(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(9, 3, 9), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        BlockPos core = source.getBlockPos();

        source.inventory.setStackInSlot(1, new ItemStack(ModItems.particle_hydrogen.get()));
        source.inventory.setStackInSlot(2, new ItemStack(ModItems.particle_copper.get()));
        source.power = source.getMaxPower();

        source.tryRun();
        helper.assertTrue(source.particle == null,
                "A warm particle source must not fire");

        chill(source);
        source.tryRun();
        helper.assertTrue(source.particle != null, "A cooled, loaded, powered source must fire");
        helper.assertTrue(source.particle.pos.equals(core.offset(beam.offsetX * 5, 0, beam.offsetZ * 5)),
                "The beam must be injected five cells along the beamline; got " + source.particle.pos);
        helper.assertTrue(source.particle.dir == beam,
                "The beam must travel along the machine's beamline axis; got " + source.particle.dir);
        helper.assertTrue(source.inventory.getStackInSlot(1).isEmpty() && source.inventory.getStackInSlot(2).isEmpty(),
                "Firing must consume both inputs");
        helper.assertTrue(source.inventory.getStackInSlot(3).is(ModItems.particle_empty.get())
                        && source.inventory.getStackInSlot(4).is(ModItems.particle_empty.get()),
                "Firing must return both empty capsules");
        helper.assertTrue(source.power == source.getMaxPower() - TileEntityPASource.usage,
                "Firing must spend the source's usage; power " + source.power);

        source.inventory.setStackInSlot(1, new ItemStack(ModItems.particle_hydrogen.get()));
        source.inventory.setStackInSlot(2, new ItemStack(ModItems.particle_copper.get()));
        source.particle = null;
        source.tryRun();
        helper.assertTrue(source.particle == null,
                "A source whose capsule returns are full must not fire");
        helper.succeed();
    }

    /** The whole walk, driven through {@link TileEntityPASource#steppy()} rather than component by component. */
    public static void beamWalks(GameTestHelper helper) {
        ForgeDirection beam = beam();
        TileEntityPASource source = place(helper, new BlockPos(14, 3, 6), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);
        BlockPos core = source.getBlockPos();

        TileEntityPABeamline beamline = place(helper, new BlockPos(8, 3, 6),
                ModMachineBlocks.pa_beamline.get(), TileEntityPABeamline.class);
        helper.assertTrue(beamline.getBlockPos().equals(core.offset(beam.offsetX * 6, 0, beam.offsetZ * 6)),
                "The walk test needs the beamline exactly six cells down the beam from the source");

        chill(source);
        source.power = source.getMaxPower();
        source.inventory.setStackInSlot(1, new ItemStack(ModItems.particle_hydrogen.get()));
        source.inventory.setStackInSlot(2, new ItemStack(ModItems.particle_copper.get()));
        source.tryRun();
        helper.assertTrue(source.particle != null, "The source must fire before the walk starts");

        source.steppy();
        helper.assertFalse(source.particle.invalid,
                "The beam must survive its first step; state " + source.state);
        helper.assertTrue(source.particle.pos.equals(beamline.getBlockPos()
                        .offset(beam.offsetX * 2, 0, beam.offsetZ * 2)),
                "The beam must land at the beamline's exit; got " + source.particle.pos);
        helper.assertTrue(source.lastSpeed == 0, "An unaccelerated beam must report zero momentum");

        source.steppy();
        helper.assertTrue(source.particle.invalid && source.state == PAState.CRASH_DERAIL,
                "A beam leaving the last component must derail; state " + source.state);
        helper.succeed();
    }

    // --- redstone over radio ---

    public static void redstoneOverRadio(GameTestHelper helper) {
        TileEntityPASource source = place(helper, new BlockPos(9, 3, 9), ModMachineBlocks.pa_source.get(), TileEntityPASource.class);

        helper.assertTrue(source.getFunctionInfo().length == 7,
                "The particle source must advertise six values and one function");
        helper.assertTrue("IDLE".equals(source.provideRORValue(IRORValueProvider.PREFIX_VALUE + "status")),
                "The particle source must report its state over radio");
        helper.assertTrue("0".equals(source.provideRORValue(IRORValueProvider.PREFIX_VALUE + "defocus")),
                "An idle source must report zero defocus rather than failing");
        source.coolantTanks[0].setFill(1_234);
        helper.assertTrue("1234".equals(source.provideRORValue(IRORValueProvider.PREFIX_VALUE + "pfmcold")),
                "The particle source must report its cold coolant");

        chill(source);
        source.power = source.getMaxPower();
        source.inventory.setStackInSlot(1, new ItemStack(ModItems.particle_hydrogen.get()));
        source.inventory.setStackInSlot(2, new ItemStack(ModItems.particle_copper.get()));
        source.tryRun();
        source.state = PAState.RUNNING;
        source.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "cancel", new String[0]);
        helper.assertTrue(source.particle == null && source.state == PAState.IDLE,
                "Cancelling over radio must abandon the run");

        TileEntityPADipole dipole = place(helper, new BlockPos(2, 3, 2), ModMachineBlocks.pa_dipole.get(), TileEntityPADipole.class);
        dipole.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "setthreshold", new String[]{"12345"});
        helper.assertTrue(dipole.threshold == 12_345,
                "The dipole must take its threshold over radio; got " + dipole.threshold);
        boolean rejected = false;
        try {
            dipole.runRORFunction(IRORInteractive.PREFIX_FUNCTION + "setthreshold", new String[]{"-5"});
        } catch (com.hbm.api.redstoneoverradio.RORFunctionException e) {
            rejected = true;
        }
        helper.assertTrue(rejected && dipole.threshold == 12_345,
                "An out-of-range threshold must be refused and leave the setting alone; got " + dipole.threshold);
        helper.assertTrue(dipole.provideRORValue(IRORValueProvider.PREFIX_VALUE + "temperature") != null,
                "The dipole must report its temperature over radio");
        helper.succeed();
    }

    public static void coolingAndTanks(GameTestHelper helper) {
        TileEntityPAQuadrupole quad = place(helper, new BlockPos(5, 3, 6), ModMachineBlocks.pa_quadrupole.get(), TileEntityPAQuadrupole.class);

        helper.assertTrue(quad.coolantTanks[0].getTankType() == Fluids.PERFLUOROMETHYL_COLD
                        && quad.coolantTanks[1].getTankType() == Fluids.PERFLUOROMETHYL,
                "The accelerator magnets must run cold perfluoromethyl in and warm out");
        helper.assertFalse(quad.isCool(), "A freshly placed magnet must be too warm to run");

        quad.coolantTanks[0].setFill(quad.coolantTanks[0].getMaxFill());
        for (int i = 0; i < 200 && !quad.isCool(); i++) {
            TileEntityPAQuadrupole.serverTick(helper.getLevel(), quad.getBlockPos(), quad.getBlockState(), quad);
        }
        helper.assertTrue(quad.isCool(), "Running coolant must bring the magnet to operating temperature");
        helper.assertTrue(quad.coolantTanks[1].getFill() > 0,
                "Cooling must produce warm perfluoromethyl to send back out");
        helper.succeed();
    }

    // --- content ---

    public static void recipes(GameTestHelper helper) {
        ParticleAcceleratorRecipes.register();
        helper.assertTrue(ParticleAcceleratorRecipes.recipes.size() == 10,
                "The accelerator must retain its ten ported collision recipes; got "
                        + ParticleAcceleratorRecipes.recipes.size());

        var amat = ParticleAcceleratorRecipes.getOutput(new ItemStack(ModItems.particle_hydrogen.get()),
                new ItemStack(ModItems.particle_copper.get()));
        helper.assertTrue(amat != null && amat.momentum == 300 && amat.output1.is(ModItems.particle_amat.get()),
                "Hydrogen and copper must make antimatter at 300 momentum");

        var swapped = ParticleAcceleratorRecipes.getOutput(new ItemStack(ModItems.particle_copper.get()),
                new ItemStack(ModItems.particle_hydrogen.get()));
        helper.assertTrue(swapped == amat, "The collision table must not care which input is which");

        var digamma = ParticleAcceleratorRecipes.getOutput(new ItemStack(ModItems.particle_sparkticle.get()),
                new ItemStack(ModItems.particle_higgs.get()));
        helper.assertTrue(digamma != null && digamma.momentum == 70_000,
                "Digamma must stay behind the 70,000 momentum wall");
        helper.succeed();
    }

    public static void construction(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        String[][] expected = {
                {"ass.beamline", "pa_beamline"},
                {"ass.rfc", "pa_rfc"},
                {"ass.quadrupole", "pa_quadrupole"},
                {"ass.dipole", "pa_dipole"},
                {"ass.source", "pa_source"},
                {"ass.detector", "pa_detector"},
        };
        for (String[] row : expected) {
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(row[0]);
            helper.assertTrue(recipe != null, "Missing accelerator assembly recipe " + row[0]);
            helper.assertTrue(recipe.outputItem != null && recipe.outputItem.length == 1,
                    "Accelerator assembly recipe " + row[0] + " must make exactly one machine");
        }
        for (String coil : new String[]{"ass.pagold", "ass.panbti", "ass.pabscco", "ass.pachlorophyte"}) {
            helper.assertTrue(AssemblyMachineRecipes.INSTANCE.getRecipe(coil) != null,
                    "Missing accelerator coil recipe " + coil);
        }
        helper.succeed();
    }

    public static void coils(GameTestHelper helper) {
        helper.assertTrue(com.hbm.items.machine.ItemPACoil.typeOf(new ItemStack(ModItems.pa_coil_gold.get()))
                        == com.hbm.items.machine.ItemPACoil.EnumCoilType.GOLD,
                "The gold coil item must resolve to its grade");
        helper.assertTrue(com.hbm.items.machine.ItemPACoil.typeOf(new ItemStack(ModItems.particle_amat.get())) == null,
                "A non-coil item must not resolve to a coil grade");
        helper.assertTrue(com.hbm.items.machine.ItemPACoil.EnumCoilType.CHLOROPHYTE.diDistMin == 51,
                "The chlorophyte coil must keep its 51-block minimum side length");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/models/particleaccelerator/source.obj",
                "assets/hbm/models/particleaccelerator/beamline.obj",
                "assets/hbm/models/particleaccelerator/rfc.obj",
                "assets/hbm/models/particleaccelerator/quadrupole.obj",
                "assets/hbm/models/particleaccelerator/dipole.obj",
                "assets/hbm/models/particleaccelerator/detector.obj",
                "assets/hbm/textures/models/particleaccelerator/source.png",
                "assets/hbm/textures/models/particleaccelerator/beamline.png",
                "assets/hbm/textures/models/particleaccelerator/rfc.png",
                "assets/hbm/textures/models/particleaccelerator/quadrupole.png",
                "assets/hbm/textures/models/particleaccelerator/dipole.png",
                "assets/hbm/textures/models/particleaccelerator/detector.png",
                "assets/hbm/textures/gui/particleaccelerator/gui_source.png",
                "assets/hbm/textures/gui/particleaccelerator/gui_rfc.png",
                "assets/hbm/textures/gui/particleaccelerator/gui_quadrupole.png",
                "assets/hbm/textures/gui/particleaccelerator/gui_dipole.png",
                "assets/hbm/textures/gui/particleaccelerator/gui_detector.png",
                "assets/hbm/textures/items/pa_coil_gold.png",
                "assets/hbm/textures/items/pa_coil_niobium.png",
                "assets/hbm/textures/items/pa_coil_bscco.png",
                "assets/hbm/textures/items/pa_coil_chlorophyte.png",
                "assets/hbm/blockstates/pa_source.json",
                "assets/hbm/blockstates/pa_beamline.json",
                "assets/hbm/blockstates/pa_rfc.json",
                "assets/hbm/blockstates/pa_quadrupole.json",
                "assets/hbm/blockstates/pa_dipole.json",
                "assets/hbm/blockstates/pa_detector.json",
                "assets/hbm/items/pa_source.json",
                "assets/hbm/items/pa_coil_gold.json",
                "assets/hbm/models/item/pa_coil_gold.json",
                "data/hbm/loot_table/blocks/pa_source.json",
                "data/hbm/loot_table/blocks/pa_detector.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing particle accelerator asset: " + path);
        }
        helper.succeed();
    }

    private static boolean resourceExists(String path) {
        try (InputStream stream = AlbionGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Every accelerator component the beam walks through has to be reachable as an {@link IParticleUser}. */
    public static void componentsAreParticleUsers(GameTestHelper helper) {
        helper.assertTrue(place(helper, new BlockPos(5, 3, 6), ModMachineBlocks.pa_beamline.get(),
                TileEntityPABeamline.class) instanceof IParticleUser, "The beamline must carry the beam");
        helper.assertTrue(place(helper, new BlockPos(3, 3, 9), ModMachineBlocks.pa_quadrupole.get(),
                TileEntityPAQuadrupole.class) instanceof IParticleUser, "The quadrupole must carry the beam");
        helper.succeed();
    }
}

package net.neoforged.neoforge.fluids;
import java.util.function.Supplier; import net.minecraft.world.level.material.Fluid;
public abstract class BaseFlowingFluid extends Fluid {
 public static class Properties { public Properties(Supplier<? extends FluidType> t,Supplier<? extends Fluid> s,Supplier<? extends Fluid> f){} }
 public static class Source extends BaseFlowingFluid { public Source(Properties p){} }
 public static class Flowing extends BaseFlowingFluid { public Flowing(Properties p){} } }

package net.neoforged.neoforge.fluids;
public class FluidType { public FluidType(Properties p){}
 public static class Properties { public static Properties create(){return null;} public Properties density(int n){return this;} public Properties viscosity(int n){return this;} public Properties temperature(int n){return this;}
  public Properties canSwim(boolean b){return this;} public Properties canDrown(boolean b){return this;} public Properties canPushEntity(boolean b){return this;} } }

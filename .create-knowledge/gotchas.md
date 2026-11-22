[both] [verified] sandbox: no network, no Gradle, no javac binary; pure Java compiles via java -m jdk.compiler/com.sun.tools.javac.Main. Nothing Minecraft-facing has been compiled. 2026-11-13
[1.21.1] [verified] Create 6.0.2: Create's own Registrate is locked behind a caller check, addons must use their own Registrate or DeferredRegister. (modrinth changelog) 2026-11-13
[1.21.1] [hypothesis] wrench: Create 6.0.7 accepts any item in a "wrenches" tag; id unconfirmed, Wrench.java checks AllItems.WRENCH and c:tools/wrench. 2026-11-13
[1.21.1] [hypothesis] interaction order: useItemOn returning PASS_TO_DEFAULT_BLOCK_INTERACTION falls through to useWithoutItem; sneaking skips useItemOn. 2026-11-13
[1.21.1] [hypothesis] tuning: reflector, absorber, moderator and Doppler feedback (0.5) give a smooth power curve; DOPPLER=0 is the harsh HBM-style variant. (Reactor.java) 2026-11-13
[1.21.1] [verified] ui-width: a layout mock showed the hint line would overflow the 330px console; keep GUI strings under about 50 characters. 2026-11-13
[1.21.1] [hypothesis] BaseFlowingFluid with no block: believed to give air instead of crashing in createLegacyBlock; unchecked. Register block and bucket if it crashes. 2026-11-13
[1.21.1] [hypothesis] FluidStack with amount 0 in getFluidInTank: believed fine; unchecked. 2026-11-13
[1.21.1] [verified] fluids-docs: NeoForge docs confirm new BaseFlowingFluid.Properties(type, source, flowing) and FluidType.Properties.create(); docs show the fluid type registry as NeoForgeRegistries.FLUID_TYPES while our code uses NeoForgeRegistries.Keys.FLUID_TYPES; switch if one fails to compile. 2026-11-13

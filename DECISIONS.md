Chose flux-grid hall over the earlier 3x3x3 pile: HBM-like rods need a grid for flux to travel along.
Chose state-only forming (no BE on casing): cheap, unform by scan.
Chose Doppler feedback 0.5: smooth power curve, runaway only with rods out. DOPPLER=0 gives harsh HBM behaviour.
Chose rod level as insertion percent (100 = in): HBM direction unconfirmed.
Chose DeferredRegister, not Registrate: Create locks its own registrate instance (6.0.2).
Rejected: colour-group redstone, graphite-tip scram, auto rods, heat (deferred to later waves).
Chose ZIRNOX as a separate fixed machine, not part of the hall: HBM's ZIRNOX is a single built machine; the hall stays the RBMK-style neutronics side.
Chose own thermal numbers (not HBM's tables): windows tuned so natural uranium is forgiving, uranium needs 10k to 16k CO2, thorium 8k to 16k.
Chose water and steam loops as the hard operation: no water loop or steam left undrained melts a strong fuel in about 6000 ticks.
Chose CO2 canister and water bucket as stand-ins for the supply chain; compressor, turbines, condenser are wave 2.
Chose steam as one fluid (super dense) for now; HBM's five levels need turbine stages first.
Chose durability bar as rod burnup so unloading keeps the state.

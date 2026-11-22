control rod | neutron absorber, inserting lowers fission rate | rod passes (100 - insertion) percent of flux | rod level 0-100, groups, console | fast/slow neutrons faked as one flux
criticality | k = neutrons made per neutron lost; k<1 dies out, k>1 grows | fuel emits BASE + GAIN*received, GAIN 1.1, relaxation 0.2 per step | none (own sim) | xenon, burnup, delayed neutrons faked
negative feedback | hotter fuel absorbs more, power self-limits | gain scaled by (1 - 0.5*flux/CAP) | none | real cause (Doppler, voids) replaced by one constant
scram (AZ-5) | all rods in at once; decay heat remains | rods move 4 percent/step vs 1 normal, flag clears when all in | AZ-5 button | decay heat not modelled yet
shutdown margin | enough rod worth to hold core subcritical | starter hall: rods at 100 gives power at base (4) | status shows power | none
gas-cooled reactor | CO2 carries core heat to a heat exchanger that boils water; hotter gas gives more steam per heat | removal = 0.0078 * CO2/1000 * (T-120), steam = removal * (T-50)/(T+273) | ZIRNOX 800C / 30 bar meltdown, 4000 CO2 minimum | gas law pressure, one thermal mass
decay heat | fission products keep heating after shutdown (about 7 percent at once, fading) | heat = 0.07 * last power, times 0.9985 per tick | none | fixed curve

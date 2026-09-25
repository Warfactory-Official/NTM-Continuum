NTMEvents.fuels(e => {
    e.rbmk.add('gametest2', 'Gametest2 RBMK Fuel Rod', r => r
        .recipe('hbm:billet_uranium')
        .shapeless('minecraft:dirt', 'minecraft:stick'))
    e.pwr.add('gametest2', 'Gametest2 PWR Fuel Rod', f => f.recipe('#c:billets/u235'))
    e.watz.add('gametest2', 'Gametest2 Watz Pellet', w => w.passive(5).burn(f => f.linear(1)).recipe('hbm:ingot_hes'))
    e.zirnox.add('gametest2', 'Gametest2 ZIRNOX Rod', z => z.life(100).heat(1).waste('gametest2', 'Gametest2 Waste')
        .recipe('hbm:billet_u235'))
    e.researchReactor.add('gametest2', 'Gametest2 Plate', p => p
        .shaped([' P ', 'PBP', ' P '], {P: '#c:plates/iron', B: 'hbm:billet_u235'}))
    e.rtg.add('gametest2', 'Gametest2 RTG Pellet', r => r.recipe('hbm:billet_pu238'))

    e.pile.add('gametest2_fuel', 'Gametest2 Pile Rod', p => p
        .reaction(2).life(1000).heat(0.75).turnsInto('gametest2_spent').desc('Gametest2 fuel'))
    e.pile.add('gametest2_spent', 'Gametest2 Spent Pile Rod', p => p.reaction(0.5).life(50).heat(1).turnsInto('waste'))
    e.pile.add('gametest2_source', 'Gametest2 Pile Source', p => p.neutronSource(3))

    e.breeding.add('gametest2', 'Gametest2', b => b
        .breedsInto('tritium', 150)
        .radiation(4)
        .recipe('hbm:billet_u235'))

    e.rbmkColumns.controlRod('rbmk_control_gametest2', 'Gametest2 Control Rod', c => c.speed(2).moderated())
    e.rbmkColumns.moderator('rbmk_moderator_gametest2', 'Gametest2 Moderator', c => c.moderation(0.5))
    e.rbmkColumns.reflector('rbmk_reflector_gametest2', 'Gametest2 Reflector', c => c.reflection(0.25))
    e.rbmkColumns.absorber('rbmk_absorber_gametest2', 'Gametest2 Absorber', c => c.heat(3))

    e.pwrParts.heatsink('pwr_heatsink_gametest2', 'Gametest2 Heatsink', p => p.counts(2).desc('Counts twice'))
    e.pwrParts.heatex('pwr_heatex_gametest2', 'Gametest2 Heat Exchanger', p => p.counts(3))
    e.pwrParts.neutronSource('pwr_source_gametest2', 'Gametest2 Neutron Source', p => p.counts(2))
    e.pwrParts.channel('pwr_channel_gametest2', 'Gametest2 Channel', p => p.counts(4))
})

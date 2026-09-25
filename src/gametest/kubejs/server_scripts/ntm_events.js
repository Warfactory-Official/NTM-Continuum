const posKey = pos => pos.getX() + ',' + pos.getY() + ',' + pos.getZ()

NTMServerEvents.debug('server_scripts', true)

NTMServerEvents.hazard(e => {
    if (e.item != 'minecraft:dead_bush' || e.entity.getId() != NTMServerEvents.debug('hazard.entity')) return
    NTMServerEvents.debug('hazard.type', e.type)
    NTMServerEvents.debug('hazard.amount', e.amount)
    if (NTMServerEvents.debug('hazard.cancel')) e.cancel()
})

NTMServerEvents.radiation(e => {
    if (e.entity.getId() != NTMServerEvents.debug('radiation.entity')) return
    NTMServerEvents.debug('radiation.amount', e.amount)
    e.amount = e.amount / 2
})

NTMServerEvents.meltdown(e => {
    if (posKey(e.pos) != NTMServerEvents.debug('meltdown.pos')) return
    NTMServerEvents.debug('meltdown.kind', e.kind)
    e.cancel()
})

NTMServerEvents.detonation(e => {
    if (posKey(e.pos) != NTMServerEvents.debug('detonation.pos')) return
    NTMServerEvents.debug('detonation.kind', e.kind)
    NTMServerEvents.debug('detonation.radius', e.radius)
    e.cancel()
})

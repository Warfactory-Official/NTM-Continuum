// Golem Core KubeJS adapter smoke check (dev runs with -Pkubejs.run=true only). Positive signals, in logs/kubejs/startup.log:
// whether HBM's event group and binding reached the startup scripts (they do only if HBM was constructed before KubeJS).
console.info(`[golem-kubejs] startup: HBMEvents ${typeof HBMEvents}, HBM ${typeof HBM}`)
if (typeof HBMEvents !== 'undefined') console.info(`[golem-kubejs] startup: HBMEvents events ${Object.keys(HBMEvents)}`)
// The startup event: posted once after the startup scripts; the event object is the payload (properties and methods).
if (typeof HBMEvents !== 'undefined') HBMEvents.registry(event => console.info(`[golem-kubejs] startup: HBMEvents.registry posted, keys ${Object.keys(event)}`))

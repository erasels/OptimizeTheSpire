# Optimize the Spire
A mod for slightly invasive performance optimization for StS with compatability kept in mind but not guranteed.

**Current changes:**
Changes AbstractPlayer.getRelic and hasRelic to turn relic access from O(n) into O(1) by using a hashmap of relic locations.
In a heavily modded run that took about 1.5 hours this saved about 4 billion unnecessary element iterations.

Wraps loggers in a new logger which skips most unneccesary log spam for the MtS console. Error and Fatal logs are still displayed so crashes can be read.
Printing to the console is a very wasteful process, skipping most of the bloat makes things like starting a new run or the game smoother and on lower-end PCs, maybe even the combat.

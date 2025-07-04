# MobHealthBars

![Damaged irongolem with a healthbar](https://cdn.modrinth.com/data/cached_images/4b024c8e49329e02a1fb14d53269eae7c9619f1f_0.webp)

MobHealthBars is a plugin that adds healthbars to your server without interfering with the mob's name, and without revealing mobs that players can't otherwise see. 

The healthbar is a separate entity that only appears when a player looks directly at a mob, and it disapears 5 seconds later (if a mob goes 5 seconds without being directly looked at by a player, the healthbar disapears). 

The healthbar changes color based on the percentage of health that is remaining.
- Green: 90%-100% HP
- Yellow: 50%-90% HP
- Orange: 10%-50% HP
- Red: 0%-10% HP

The healthbar used to be a TextDisplay entity, but for some reason TextDisplays collide with projectiles, so healthbars were switched to being invisible armorstands (which don't collide with projectiles thanks to the marker tag).


# Sable Ragdoll: Cooking

Turn a fatal mob hit into a physical ragdoll, cook it over a campfire, and harvest extra loot. The original loot is captured once; cooking transforms that saved loot rather than rolling the mob's loot table again.

Minecraft 1.21.1 / NeoForge 21.1.249. **Ragdoll Reactions 0.7.0 is the only required companion mod**; it brings Sable and Sable Ragdolls through its own dependencies. A mob must be allowed by Sable's whitelist. KubeJS is optional. Ember Text API, Valoren Tweaks and Iron's Simple Blood are not required. If Simple Blood is installed, harvests use its blood particle; otherwise they use Minecraft's damage-indicator particle. The mod has no JEI integration.

## Features

- Fatal-hit ragdolls launched with Ragdoll Reactions' own damage physics. By default, nonfatal **mob** ragdoll reactions are disabled; player reactions are untouched. Set `mobRagdollsOnlyOnDeath=false` to restore them.
- Campfire cooking with distance-sensitive progress, persistent bodies and cooked appearance. Cooking pauses when the fire goes out and resumes when it returns.
- Extra cooked drops without duplicate loot-table rolls. The original loot, progress and completed harvest cuts are saved.
- Server-side harvest actions, physical carrying and lead transport. Uncooked bodies eventually return their original loot if left still and unheated.
- Built-in vanilla mob recipes, optional Farmer's Delight knife/meat-cut integration, optional Simple Blood particles, and configurable fuel-burning campfires.
- Datapack JSON and KubeJS definitions for mobs, tools, heat sources, yields, effects, and timing.

## Quick start

1. Install this mod and Ragdoll Reactions 0.7.0 on the client and server. Let Ragdoll Reactions install its own required dependencies.
2. Kill a supported mob. Its final hit uses Ragdoll Reactions' launch formula and leaves a physical carcass. If the mob has no Sable ragdoll model or is excluded by its whitelist, no carcass is created.
3. Move the body to a lit campfire or soul campfire. Leave it near the heat until the cooking bar fills. Right-click a physical body part to carry it; a lead can transport it too.
4. Hit a raw body to recover its original loot. Hit a cooked body with a sword twice to extract its cooked loot. Each cut is one hit. With Farmer's Delight installed, use a knife instead and collect extra meat cuts.

The campfire fuel mechanic is **off by default**. Enable `campfiresRequireFuel` in `config/sable-ragdoll-cooking-server.toml` if you want all campfires to require furnace fuel. `fuelDurationMultiplier=10` makes each fuel item last ten times its furnace duration. The new `mobRagdollsOnlyOnDeath=true` option suppresses nonfatal mob ragdolls from Ragdoll Reactions; set it to `false` if your pack wants ordinary hit/fall/explosion reactions as well.

## Built-in recipes

Bundled datapack recipes cover cow, mooshroom, pig, sheep, chicken, rabbit and hoglin. Their raw meat becomes twice as much cooked meat after campfire cooking; other captured drops, such as leather or wool, stay unchanged. A fallback recipe covers 78 vanilla mob IDs: captured meat and fish become twice as much cooked food, and other captured drops increase by 1.5 times. These are shipped as editable datapack JSON and an entity tag. Actual ragdoll conversion still depends on the installed Sable whitelist/model support; tagging a mob does not force Sable to create an unsupported model.

The body must be near a lit campfire or soul campfire. By default, two sword cuts harvest it, one actual hit per cut; the HUD shows an iron sword as the example. Attacking a raw body returns original drops. Cooked output is partitioned across cuts, with remainders on the final cut. The original loot table is not rerolled, which prevents extra rolls and keeps existing loot data. Raw bodies show no action label. A still, unheated ragdoll returns its original drops after `uncookedDespawnTicks` (6000 ticks by default); movement resets this timer and active cooking pauses it. Set the value to 0 to disable this cleanup.

The fatal strike uses Ragdoll Reactions' installed hit formula for direction, tilt, launch strength and angular motion. The source mob stays visible until Sable converts that same entity into ragdoll parts. The recipe's `impact_multiplier` scales that native fatal impulse; 0 removes the impulse while retaining the carcass.

If Farmer's Delight is installed, the bundled sword-based harvests require knives from `#c:tools/knife` instead, show an iron knife, and add meat cuts from the original captured beef, pork, chicken, mutton, cod, or salmon. This integration is optional and does not add a hard dependency. Custom recipes using a different tool keep that tool.

## Campfire fuel

The world config `sable-ragdoll-cooking-server.toml` has `campfiresRequireFuel=false` and `fuelDurationMultiplier=10` by default. When enabled, **every** lit campfire burns fuel, including vanilla campfires with no carcass nearby. Right-click a campfire with a furnace fuel item to add its burn time times the multiplier; ordinary campfire food input retains priority. Fuel is saved on the campfire block entity, and the campfire extinguishes when empty. The fuel bar has a thin outline and shows the item currently burning; queued fuels retain their order. Use a normal fire-lighting method after refueling an extinguished campfire. An existing KubeJS `Carcasses.campfireFuel(0..1000)` call overrides the config until scripts are reloaded; remove the call to return to config control.

## Datapacks

Place files at `data/<namespace>/sable_ragdoll_cooking/<name>.json`. Higher-priority packs can replace a bundled file at the same path, including `{"enabled":false}` to disable it. `/reload` reloads datapack definitions. Individual invalid files are logged and skipped. KubeJS server-script definitions are a separate layer; scripts with the same ID override datapack entries, and scripts are checked first for mob selectors. Exact mob selectors take precedence over tags.

```json
{
  "mob": "minecraft:cow",
  "required_mods": ["sable_player_ragdoll"],
  "adapter": "sable_player_ragdoll",
  "tool": "#minecraft:swords",
  "harvest_count": 2,
  "harvest_input": "LEFT",
  "action_ticks": 6,
  "action_cooldown": 8,
  "heat": ["minecraft:campfire", "minecraft:soul_campfire"],
  "lit_only": true,
  "cooking_ticks": 400,
  "heat_distance": {"near": 0.5, "range": 3.5, "maximum": 1.5, "curve": 1.2},
  "cooked_drops": [
    {"source": "minecraft:beef", "result": "minecraft:cooked_beef", "multiplier": 2}
  ],
  "cooking_feedback": {
    "particles": {"id": "minecraft:small_flame", "count": 1, "spread": 0.18, "speed": 0.005}
  },
  "cooked_feedback": {
    "sound": {"id": "minecraft:block.campfire.crackle", "volume": 0.7, "pitch": 1.2}
  }
}
```

`mob`, `tool`, and each `heat` entry accept IDs or `#namespace:tag`. `cooked_drops` maps only matching captured items; unmatched items keep their original form and count unless `cooked_loot_multiplier` is also set. Multipliers are bounded to 0..10, and counts round up per captured stack. Set `harvest_input` to `LEFT`, `RIGHT` or `BOTH`. Optional fields include `durability`, `consume`, `hud_range`, `impact_multiplier`, `passive_resolution`, `settle_ticks`, `settle_tolerance`, `cooking_mount`, `accelerators`, and `harvest_feedback`/`removal_feedback`. `feedback` can supply `display_item`, `sound` and `particles` with the shapes shown above. A disabled or absent optional mod listed in `required_mods` skips that definition. Use the bundled recipes as runnable examples.

## KubeJS

In a `server_scripts` file:

```js
Carcasses.create('example:venison', body => {
  body.mob('example:deer')
      .adapter('sable_player_ragdoll')
      .tool('#minecraft:swords', 3)
      .heat('minecraft:campfire')
      .heat('minecraft:soul_campfire')
      .cookingTicks(500)
      .cookedDrop('example:raw_venison', 'example:cooked_venison', 2)
      .harvestInput('LEFT')
})
```

`cookedLootMultiplier(1.5)` boosts all otherwise unmapped captured drops. `onHarvest(context)` remains available for custom output logic; use `clearOutputs()` and `addOutput(stack)` to replace the generated cooked partition. `resolveLoot()` is a defensive copy of original drops; `resolveHarvestLoot()` returns its original partition. Changes to server-script registrations require the KubeJS server-script reload; startup listeners may require a full restart. Existing bodies keep their saved recipe ID, loot and progress. Avoid removing active recipe IDs while bodies are in the world.

## Validation boundary

Dedicated-server GameTests verify recipe loading and saved loot/fuel behavior. Physical Sable models, shaders, world indicators, particle placement and client interaction still need in-game visual testing.

## Build from source

Use Java 21. Place the required development JARs in `libs/`, or pass `-PmodsDir=/absolute/path/to/mods` to point Gradle at an existing Minecraft instance's `mods` folder. The build expects `ragdoll_reactions-1.21.1-0.7.0.jar`, `sable_player_ragdoll-1.21.1-0.7.5.jar`, `sable-neoforge-1.21.1-2.0.5.jar`, `kubejs-neoforge-2101.7.2-build.377.jar`, and `rhino-2101.2.8-build.91.jar` for compilation. KubeJS and Rhino remain optional at runtime. Run `./gradlew build`; the installable JAR appears in `build/libs/`.

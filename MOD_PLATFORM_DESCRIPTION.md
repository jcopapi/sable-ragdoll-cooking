# Sable Ragdoll: Cooking

Turn fallen mobs into physical carcasses, cook them over a campfire, and harvest more loot.

Sable Ragdoll: Cooking connects **Ragdoll Reactions** to an in-world cooking system. A fatal hit leaves a ragdoll behind, with the mob's original drops captured and saved. Cooking transforms those drops instead of rolling the loot table again.

## Features

- Physical mob carcasses launched with Ragdoll Reactions' hit physics
- Campfire and soul campfire cooking with visible progress
- Extra cooked loot without duplicate loot-table rolls
- Saved cooking progress and harvest actions
- Carrying and lead transport for carcasses
- Built-in recipes for vanilla mobs
- Datapack and optional KubeJS recipe support
- Optional Farmer's Delight knives and meat cuts
- Optional Simple Blood particles and configurable campfire fuel

## How It Works

Kill a supported mob and move its ragdoll near a lit campfire. When the cooking bar fills, strike the body with a sword to extract its cooked loot. Farmer's Delight changes the built-in harvest tool to a knife and adds meat cuts. A raw body can be harvested for its original drops instead.

Cooking pauses if the fire goes out and continues when heat returns. Unheated carcasses eventually return their original loot if left still. By default, Ragdoll Reactions' **nonfatal mob ragdolls** are disabled; player reactions are unaffected. This behavior and optional fuel-burning campfires can be changed in the server config.

Carcasses require a mob model supported and allowed by Sable Ragdolls. Adding a recipe alone does not make an unsupported mob ragdoll.

## Datapack Example

Recipes are data-driven. This example cooks a cow's captured beef into twice as much cooked beef and requires two sword hits to harvest:

```json
{
  "mob": "minecraft:cow",
  "tool": "#minecraft:swords",
  "harvest_count": 2,
  "harvest_input": "LEFT",
  "heat": ["minecraft:campfire", "minecraft:soul_campfire"],
  "lit_only": true,
  "cooking_ticks": 400,
  "cooked_drops": [
    {"source": "minecraft:beef", "result": "minecraft:cooked_beef", "multiplier": 2}
  ]
}
```

Place the file at `data/<namespace>/sable_ragdoll_cooking/<name>.json`. Recipes can also define heat distance, other tools, timing, effects, loot multipliers, and more.

## KubeJS Example

KubeJS is optional. In a `server_scripts` file, the same idea can be registered in JavaScript:

```js
Carcasses.create('example:cow_campfire', body => {
  body.mob('minecraft:cow')
      .tool('#minecraft:swords', 2)
      .heat('minecraft:campfire')
      .cookingTicks(400)
      .cookedDrop('minecraft:beef', 'minecraft:cooked_beef', 2)
      .harvestInput('LEFT')
})
```

## Requirements

Minecraft **1.21.1**, **NeoForge**, and **Ragdoll Reactions 0.7.0**. Ragdoll Reactions brings its own required dependencies. Farmer's Delight, KubeJS, and Simple Blood are optional.

For configuration, integrations, and the complete recipe reference, see the [project documentation](https://github.com/jcopapi/sable-ragdoll-cooking#readme).

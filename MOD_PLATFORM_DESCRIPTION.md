# Sable Ragdoll: Cooking

Sable Ragdoll: Cooking is an addon for **Ragdoll Reactions** that lets you cook mob ragdolls over campfires.

Instead of mobs simply disappearing after death, you can move their bodies around, bring them near a campfire and cook them for food and other drops.

## Features

- Cook supported mob ragdolls over campfires
- Works with Ragdoll Reactions physics
- Carry and move carcasses around
- Raw and cooked mob drops
- Support for vanilla mobs
- Custom recipes through datapacks
- Optional KubeJS support
- Optional Farmer's Delight integration
- Optional Simple Blood integration

Built-in recipes focus on passive mobs that provide meat: cows, mooshrooms, pigs, sheep, chickens, rabbits, cod and salmon. Modpacks can add other mobs with their own recipes.

## Requirements

- Minecraft **1.21.1**
- **NeoForge**
- **Ragdoll Reactions 0.7.0**

## Datapack Example

This example cooks a cow over a lit campfire and doubles its beef after two sword hits:

```json
{
  "mob": "minecraft:cow",
  "tool": "#minecraft:swords",
  "harvest_count": 2,
  "harvest_input": "LEFT",
  "heat": ["minecraft:campfire"],
  "lit_only": true,
  "cooking_ticks": 400,
  "cooked_drops": [
    {"source": "minecraft:beef", "result": "minecraft:cooked_beef", "multiplier": 2}
  ]
}
```

Save it as `data/<namespace>/sable_ragdoll_cooking/<name>.json` in a datapack.

## KubeJS Example

With KubeJS installed, add this to a `server_scripts` file:

```js
rcooking.create('example:cow_campfire', body => {
  body.mob('minecraft:cow')
      .tool('#minecraft:swords', 2)
      .heat('minecraft:campfire')
      .cookingTicks(400)
      .cookedDrop('minecraft:beef', 'minecraft:cooked_beef', 2)
      .harvestInput('LEFT')
})
```

More customization options and recipe documentation can be found on the [GitHub page](https://github.com/jcopapi/sable-ragdoll-cooking). See the [configuration guide](https://github.com/jcopapi/sable-ragdoll-cooking/blob/main/docs/CONFIGURATION.md) for all fields and integrations.

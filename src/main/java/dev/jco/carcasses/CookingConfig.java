package dev.jco.carcasses;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned campfire rules. They apply to every vanilla campfire. */
public final class CookingConfig {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue CAMPFIRES_REQUIRE_FUEL=BUILDER
        .comment("If true, all campfires consume fuel and extinguish when empty.")
        .define("campfiresRequireFuel",false);
    public static final ModConfigSpec.IntValue FUEL_DURATION_MULTIPLIER=BUILDER
        .comment("Campfire fuel duration relative to the same fuel in a furnace.")
        .defineInRange("fuelDurationMultiplier",10,1,1000);
    public static final ModConfigSpec.IntValue UNCOOKED_DESPAWN_TICKS=BUILDER
        .comment("Ticks an unheated, still carcass may remain before returning its captured drops. Set to 0 to disable. Movement resets the timer.")
        .defineInRange("uncookedDespawnTicks",6000,0,1200000);
    public static final ModConfigSpec.BooleanValue MOB_RAGDOLLS_ONLY_ON_DEATH=BUILDER
        .comment("Suppress Ragdoll Reactions for nonfatal mob hits, falls, explosions and impacts. Fatal Cooking ragdolls still use its native hit physics. Players are unaffected.")
        .define("mobRagdollsOnlyOnDeath",true);
    public static final ModConfigSpec SPEC=BUILDER.build();
    private CookingConfig(){}
    public static void register(ModContainer container){container.registerConfig(ModConfig.Type.SERVER,SPEC,"sable-ragdoll-cooking-server.toml");}
    public static int multiplier(){return CAMPFIRES_REQUIRE_FUEL.get()?FUEL_DURATION_MULTIPLIER.get():0;}
}

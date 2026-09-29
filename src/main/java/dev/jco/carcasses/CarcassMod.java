package dev.jco.carcasses;

import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod("jco_carcasses")
public final class CarcassMod {
    public CarcassMod(IEventBus bus,net.neoforged.fml.ModContainer container) {
        CookingConfig.register(container);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,CarcassTransport::attachBlock);
        if(ModList.get().isLoaded("sable_player_ragdoll")) Carcasses.registerAdapter(new dev.jco.carcasses.integration.SableAdapter());
        bus.addListener((RegisterPayloadHandlersEvent e)->e.registrar("4").playToServer(CarryPayload.TYPE,CarryPayload.CODEC,(p,c)->c.enqueueWork(()->CarryPayload.handle((net.minecraft.server.level.ServerPlayer)c.player(),p))).playToClient(FuelPayload.TYPE,FuelPayload.CODEC,(p,c)->c.enqueueWork(()->dev.jco.carcasses.client.FuelOverlay.accept(p))).playToClient(CarcassPayload.TYPE,CarcassPayload.CODEC,
            (p,c)->c.enqueueWork(()->dev.jco.carcasses.client.CarcassOverlay.accept(p))));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,CarcassRuntime::drops);
        NeoForge.EVENT_BUS.addListener(CarcassRuntime::damage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,dev.jco.carcasses.integration.ReactionBridge::damage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,CarcassRuntime::death);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,CarcassRuntime::experience);
        NeoForge.EVENT_BUS.addListener(CarcassRuntime::attackEntity);
        NeoForge.EVENT_BUS.addListener(CarcassRuntime::entityInteract);
        NeoForge.EVENT_BUS.addListener(CarcassRuntime::tick);
        NeoForge.EVENT_BUS.addListener(CampfireFuel::interact);NeoForge.EVENT_BUS.addListener(CampfireFuel::playerTick);
        NeoForge.EVENT_BUS.addListener(CarcassTransport::leave);
        NeoForge.EVENT_BUS.addListener(CarcassCommands::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent e)->e.addListener(new CarcassJsonLoader()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e)->{Carcasses.clearSources();CampfireFuel.multiplier=-1;CarcassTransport.clear();});
    }
}

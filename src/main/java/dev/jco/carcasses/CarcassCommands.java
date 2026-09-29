package dev.jco.carcasses;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CarcassCommands {
    public static void register(RegisterCommandsEvent e) {
        var root=Commands.literal("jco_carcasses").requires(s->s.hasPermission(2));
        root.then(Commands.literal("status").executes(c->{
            var data=CarcassData.get(c.getSource().getLevel());
            c.getSource().sendSuccess(()->Component.literal("Definitions: "+Carcasses.definitions().keySet()+"; adapters: "+Carcasses.adapters().stream().map(CarcassAdapter::id).toList()+"; carcasses: "+data.entries.size()),false);
            data.entries.values().stream().limit(20).forEach(v->c.getSource().sendSuccess(()->Component.literal(v.carrier+" "+v.definition+" actions="+v.actions+" cooking="+v.cooking+" failed="+v.failed+" finishing="+v.finishing),false)); return data.entries.size();
        }));
        for(String operation:new String[]{"retry","refund"}) root.then(Commands.literal(operation).then(Commands.argument("id",UuidArgument.uuid()).executes(c->{
            var data=CarcassData.get(c.getSource().getLevel()); var entry=data.entries.get(UuidArgument.getUuid(c,"id"));
            if(entry==null) { c.getSource().sendFailure(Component.literal("Unknown carcass in this dimension")); return 0; }
            if(operation.equals("refund")) CarcassRuntime.refund(entry);
            else { entry.failed=false; entry.worker=null; entry.workTicks=0; }
            data.setDirty(); c.getSource().sendSuccess(()->Component.literal("Carcass "+operation+" queued; its chunk must be loaded"),false); return 1;
        })));
        e.getDispatcher().register(root);
    }
}

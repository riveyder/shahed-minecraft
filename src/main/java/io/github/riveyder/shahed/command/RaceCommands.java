package io.github.riveyder.shahed.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.*;
import io.github.riveyder.shahed.control.DroneRacing;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class RaceCommands {
    private static boolean admin(CommandSourceStack s){return s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    private static int help(CommandSourceStack s){
        s.sendSuccess(()->Component.translatable("race.command_help"),false);
        if(admin(s))s.sendSuccess(()->Component.translatable("race.editor_help"),false);return 1;
    }
    public static LiteralArgumentBuilder<CommandSourceStack> tree(){
        var root=Commands.literal("race").executes(c->help(c.getSource()))
            .then(Commands.literal("help").executes(c->help(c.getSource())))
            .then(Commands.literal("list").executes(c->DroneRacing.list(c.getSource().getPlayerOrException())))
            .then(Commands.literal("cancel").executes(c->DroneRacing.cancel(c.getSource().getPlayerOrException(),"cancelled")))
            .then(Commands.literal("join").then(Commands.argument("course",StringArgumentType.word()).executes(c->DroneRacing.join(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course")))))
            .then(Commands.literal("preview").then(Commands.argument("course",StringArgumentType.word()).executes(c->DroneRacing.preview(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course")))))
            .then(recordsTree())
            .then(Commands.literal("save").requires(RaceCommands::admin).executes(c->DroneRacing.save(c.getSource().getPlayerOrException())))
            .then(Commands.literal("edit").requires(RaceCommands::admin).then(Commands.argument("course",StringArgumentType.word()).executes(c->DroneRacing.edit(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course")))))
            .then(Commands.literal("delete").requires(RaceCommands::admin).then(Commands.argument("course",StringArgumentType.word()).executes(c->DroneRacing.delete(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course")))))
            .then(Commands.literal("enabled").requires(RaceCommands::admin).then(Commands.argument("course",StringArgumentType.word()).then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->DroneRacing.enabled(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course"),BoolArgumentType.getBool(c,"enabled"))))));
        var name=Commands.argument("course",StringArgumentType.word());
        String[] kinds={"time_trial","precision","obstacle"};
        for(int i=0;i<kinds.length;i++){int kind=i;name.then(Commands.literal(kinds[i]).executes(c->DroneRacing.create(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course"),kind)));}
        root.then(Commands.literal("create").requires(RaceCommands::admin).then(name));
        var gates=Commands.literal("gate").requires(RaceCommands::admin);
        for(String operation:new String[]{"start","add","finish"})gates.then(Commands.literal(operation)
                .executes(c->DroneRacing.gate(c.getSource().getPlayerOrException(),operation,0,0))
                .then(Commands.argument("radius",FloatArgumentType.floatArg(1,16)).executes(c->DroneRacing.gate(c.getSource().getPlayerOrException(),operation,0,FloatArgumentType.getFloat(c,"radius")))));
        gates.then(Commands.literal("set").then(Commands.argument("index",IntegerArgumentType.integer(1,64))
                .executes(c->DroneRacing.gate(c.getSource().getPlayerOrException(),"set",IntegerArgumentType.getInteger(c,"index"),0))
                .then(Commands.argument("radius",FloatArgumentType.floatArg(1,16)).executes(c->DroneRacing.gate(c.getSource().getPlayerOrException(),"set",IntegerArgumentType.getInteger(c,"index"),FloatArgumentType.getFloat(c,"radius"))))));
        gates.then(Commands.literal("remove").then(Commands.argument("index",IntegerArgumentType.integer(1,64))
                .executes(c->DroneRacing.gate(c.getSource().getPlayerOrException(),"remove",IntegerArgumentType.getInteger(c,"index"),0))));
        return root.then(gates);
    }
    private static LiteralArgumentBuilder<CommandSourceStack> recordsTree(){
        var name=Commands.argument("course",StringArgumentType.word()).executes(c->DroneRacing.records(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course")));
        for(var kind:io.github.riveyder.shahed.entity.AircraftKind.values())name.then(Commands.literal(kind.id)
                .executes(c->DroneRacing.records(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"course"),kind.id)));
        return Commands.literal("records").then(name);
    }
    private RaceCommands(){}
}

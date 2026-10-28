package dev.mikaaah.tnkprogression;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class TitleCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> tree(){
        return Commands.literal("title")
            .executes(c->list(c.getSource(),false,1))
            .then(Commands.literal("list").executes(c->list(c.getSource(),false,1))
                .then(Commands.argument("page",IntegerArgumentType.integer(1)).executes(c->list(c.getSource(),false,IntegerArgumentType.getInteger(c,"page")))))
            .then(Commands.literal("catalog").executes(c->list(c.getSource(),true,1))
                .then(Commands.argument("page",IntegerArgumentType.integer(1)).executes(c->list(c.getSource(),true,IntegerArgumentType.getInteger(c,"page")))))
            .then(Commands.literal("select").then(Commands.argument("id",StringArgumentType.greedyString())
                .suggests((c,b)->SharedSuggestionProvider.suggest(TitleConfig.current.titles().stream().filter(t->c.getSource().getEntity() instanceof ServerPlayer p && Titles.unlocked(p,t.id())).map(TitleConfig.Definition::id),b))
                .executes(c->{var p=c.getSource().getPlayerOrException();String id=StringArgumentType.getString(c,"id").trim();if(!Titles.select(p,id)){c.getSource().sendFailure(Component.literal("You have not unlocked that title."));return 0;}c.getSource().sendSuccess(()->Component.literal("Selected title: "+TitleConfig.get(id).orElseThrow().label()),false);return 1;})))
            .then(Commands.literal("clear").executes(c->{Titles.select(c.getSource().getPlayerOrException(),"");c.getSource().sendSuccess(()->Component.literal("Title hidden. Your unlocked titles are kept."),false);return 1;}))
            .then(Commands.literal("grant").requires(s->s.hasPermission(2))
                .then(Commands.argument("player",EntityArgument.players()).then(Commands.argument("id",StringArgumentType.greedyString())
                    .suggests((c,b)->SharedSuggestionProvider.suggest(TitleConfig.current.titles().stream().map(TitleConfig.Definition::id),b))
                    .executes(c->{String id=StringArgumentType.getString(c,"id").trim();if(TitleConfig.get(id).isEmpty()){c.getSource().sendFailure(Component.literal("Unknown title ID: "+id));return 0;}int count=0;for(var p:EntityArgument.getPlayers(c,"player"))if(Titles.grant(p,id))count++;return count;}))))
            .then(Commands.literal("earn").requires(s->s.hasPermission(2))
                .then(Commands.argument("player",EntityArgument.players()).then(Commands.argument("id",StringArgumentType.greedyString())
                    .suggests((c,b)->SharedSuggestionProvider.suggest(TitleConfig.current.titles().stream().map(TitleConfig.Definition::id),b))
                    .executes(c->{String id=StringArgumentType.getString(c,"id").trim();if(TitleConfig.get(id).isEmpty()){c.getSource().sendFailure(Component.literal("Unknown title ID: "+id));return 0;}int count=0;for(var p:EntityArgument.getPlayers(c,"player"))if(Titles.earn(p,id))count++;return count;}))))
            .then(Commands.literal("revoke").requires(s->s.hasPermission(2))
                .then(Commands.argument("player",EntityArgument.players()).then(Commands.argument("id",StringArgumentType.greedyString())
                    .suggests((c,b)->SharedSuggestionProvider.suggest(TitleConfig.current.titles().stream().map(TitleConfig.Definition::id),b))
                    .executes(c->{int count=0;for(var p:EntityArgument.getPlayers(c,"player"))if(Titles.revoke(p,StringArgumentType.getString(c,"id").trim()))count++;return count;}))))
            .then(Commands.literal("reload").requires(s->s.hasPermission(2)).executes(c->{if(!TitleConfig.reload()){c.getSource().sendFailure(Component.literal("Title catalog rejected; previous catalog kept."));return 0;}for(var p:c.getSource().getServer().getPlayerList().getPlayers()){Titles.checkSkills(p);LevelBadges.changed(p);}c.getSource().sendSuccess(()->Component.literal("Title catalog reloaded."),false);return 1;}));
    }
    private static int list(CommandSourceStack source,boolean catalog,int page)throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p=source.getPlayerOrException();var defs=TitleConfig.current.titles().stream().filter(t->catalog || Titles.unlocked(p,t.id())).toList();
        int pages=Math.max(1,(defs.size()+7)/8),current=Math.min(page,pages);
        source.sendSuccess(()->Component.literal((catalog?"Title catalog":"Unlocked titles")+" — "+current+"/"+pages+" | selected: "+TitleConfig.get(Titles.selected(p)).map(TitleConfig.Definition::label).orElse("none")),false);
        for(var d:defs.subList(Math.min(defs.size(),(current-1)*8),Math.min(defs.size(),current*8))){
            boolean earned=Titles.unlocked(p,d.id());var line=Component.literal(earned?"✓ ":"Locked: ").append(Titles.badge(d)).append(Component.literal(" — "+d.description()));
            if(earned)line.withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tnkprogress title select "+d.id())));
            source.sendSuccess(()->line,false);
        }
        source.sendSuccess(()->Component.literal("/tnkprogress title select <id> · /tnkprogress title clear").withStyle(net.minecraft.ChatFormatting.GRAY),false);return defs.size();
    }
}

package dev.mikaaah.tnkprogression;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public final class OriginCommands {
    private OriginCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("origin").requires(source -> source.hasPermission(2))
            .then(Commands.literal("get")
                .then(Commands.argument("players", EntityArgument.players()).executes(context -> {
                    var players = EntityArgument.getPlayers(context, "players");
                    for (var player : players) context.getSource().sendSuccess(() -> Component.literal(
                        player.getGameProfile().getName() + " — " + (Origins.selected(player).isEmpty() ? "No origin chosen" : Origins.selected(player))), false);
                    return players.size();
                })))
            .then(Commands.literal("set")
                .then(Commands.argument("players", EntityArgument.players())
                    .then(Commands.argument("id", StringArgumentType.greedyString())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(OriginRules.all().stream().map(OriginRules.Origin::id), builder))
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id").trim();
                            if (OriginRules.find(id).isEmpty()) {
                                context.getSource().sendFailure(Component.literal("Unknown origin: " + id));
                                return 0;
                            }
                            var players = EntityArgument.getPlayers(context, "players");
                            for (var player : players) Origins.adminSet(player, id);
                            context.getSource().sendSuccess(() -> Component.literal("Set origin " + id + " for " + players.size() + " player(s)."), true);
                            return players.size();
                        }))))
            .then(Commands.literal("reset")
                .then(Commands.argument("players", EntityArgument.players()).executes(context -> {
                    var players = EntityArgument.getPlayers(context, "players");
                    for (var player : players) Origins.adminReset(player);
                    context.getSource().sendSuccess(() -> Component.literal("Cleared origin for " + players.size() + " player(s). They can choose again on their next Character visit."), true);
                    return players.size();
                })));
    }
}

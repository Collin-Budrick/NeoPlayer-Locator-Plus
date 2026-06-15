package sh.sit.plp.color;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import sh.sit.plp.BarUpdater;
import sh.sit.plp.config.ConfigManager;
import sh.sit.plp.config.ModConfig;

import java.util.Locale;
import java.util.stream.Stream;

public final class PLPCommand {
    private static final SimpleCommandExceptionType WRONG_COLOR_MODE = new SimpleCommandExceptionType(
            Component.translatable("commands.player-locator-plus.color.wrong-color-mode")
    );

    private PLPCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("plp")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ConfigManager.reload(true);
                            BarUpdater.fullResend(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal("Player Locator Plus config reloaded"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("random")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            BarUpdater.sendFakePlayers(context.getSource().getPlayerOrException());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("color")
                        .then(Commands.argument("color", StringArgumentType.word())
                                .requires(CommandSourceStack::isPlayer)
                                .suggests((context, builder) -> {
                                    if (builder.getRemaining().isBlank()) {
                                        builder.suggest("#");
                                    }
                                    if (builder.getRemaining().startsWith("#") && builder.getRemaining().length() < 7) {
                                        for (char c : "0123456789abcdef".toCharArray()) {
                                            builder.suggest(builder.getRemaining() + c);
                                        }
                                    }
                                    return SharedSuggestionProvider.suggest(colorNames(), builder);
                                })
                                .executes(context -> setColor(context.getSource(), context.getSource().getPlayerOrException(), parseColor(StringArgumentType.getString(context, "color"))))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> setColor(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                parseColor(StringArgumentType.getString(context, "color"))
                                        ))))));
    }

    private static int setColor(CommandSourceStack source, ServerPlayer player, int color) throws CommandSyntaxException {
        if (ConfigManager.getConfig().colorMode != ModConfig.ColorMode.CUSTOM) {
            throw WRONG_COLOR_MODE.create();
        }

        PlayerColorStore.set(player.getUUID(), color);
        Component colorText = Component.literal(String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF))
                .withStyle(style -> style.withColor(color & 0xFFFFFF));

        if (source.getEntity() == player) {
            source.sendSuccess(() -> Component.translatable("commands.player-locator-plus.color.self", colorText), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.player-locator-plus.color.other", player.getDisplayName(), colorText), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int parseColor(String raw) throws CommandSyntaxException {
        String value = raw.trim();
        ChatFormatting formatting = ChatFormatting.getByName(value);
        if (formatting != null && formatting.isColor() && formatting.getColor() != null) {
            return formatting.getColor() & 0xFFFFFF;
        }

        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() != 6) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().create(raw);
        }

        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().create(raw);
        }
    }

    private static Stream<String> colorNames() {
        return Stream.of(ChatFormatting.values())
                .filter(ChatFormatting::isColor)
                .map(ChatFormatting::getName);
    }
}

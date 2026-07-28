package com.rtc.amethystTools;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AmethystToolsFabric implements ModInitializer {
    public static final AmethystToolConfig CONFIG = new AmethystToolConfig("amethysttools.properties");
    private static final ToolSystems SYSTEMS = new ToolSystems();

    @Override
    public void onInitialize() {
        CONFIG.load();
        SYSTEMS.register();
        CommandRegistrationCallback.EVENT.register(AmethystToolsFabric::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("amethysttools")
                .executes(AmethystToolsFabric::openRootCommand)
                .then(argument("tier", net.minecraft.command.argument.StringArgumentType.word())
                        .suggests((context, builder) -> suggest(builder, ToolDefinitions.tierIds()))
                        .executes(AmethystToolsFabric::tierOnlyCommand)
                        .then(argument("type", net.minecraft.command.argument.StringArgumentType.word())
                                .suggests((context, builder) -> suggest(builder, ToolDefinitions.typeIds()))
                                .executes(AmethystToolsFabric::giveSelfCommand))));

        dispatcher.register(literal("amethysttoolsreload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(AmethystToolsFabric::reloadCommand));

        dispatcher.register(literal("amethysttoolsgive")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("player", net.minecraft.command.argument.StringArgumentType.word())
                        .suggests((context, builder) -> suggest(builder, context.getSource().getServer().getPlayerManager().getPlayerList().stream().map(player -> player.getGameProfile().getName()).toList()))
                        .executes(AmethystToolsFabric::needTierForTarget)
                        .then(argument("tier", net.minecraft.command.argument.StringArgumentType.word())
                                .suggests((context, builder) -> suggest(builder, ToolDefinitions.tierIds()))
                                .executes(AmethystToolsFabric::needTypeForTarget)
                                .then(argument("type", net.minecraft.command.argument.StringArgumentType.word())
                                        .suggests((context, builder) -> suggest(builder, ToolDefinitions.typeIds()))
                                        .executes(AmethystToolsFabric::giveTargetCommand)))));
    }

    private static int openRootCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        openMenu(player, AmethystMenuScreenHandler.MenuKind.ROOT);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0F, 1.0F);
        return 1;
    }

    private static int tierOnlyCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(context.getSource(), net.minecraft.command.argument.StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendError(context.getSource(), message("messages.wrong-material", "Please enter a valid material."));
            return 0;
        }

        sendFeedback(context.getSource(), message("messages.enter-type", "Please enter a tool type (pickaxe, axe, shovel)."));
        return 1;
    }

    private static int giveSelfCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(context.getSource(), net.minecraft.command.argument.StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendError(context.getSource(), message("messages.wrong-material", "Please enter a valid material."));
            return 0;
        }

        ToolDefinitions.Type type = parseType(context.getSource(), net.minecraft.command.argument.StringArgumentType.getString(context, "type"));
        if (type == null) {
            sendError(context.getSource(), message("messages.wrong-tool", "Please enter a valid tool."));
            return 0;
        }

        if (!giveTool(player, tier, type)) {
            return 0;
        }

        return 1;
    }

    private static int reloadCommand(CommandContext<ServerCommandSource> context) {
        try {
            CONFIG.load();
            sendFeedback(context.getSource(), message("messages.reloadplugin", "Plugin reload is successful."));
            return 1;
        } catch (RuntimeException exception) {
            sendError(context.getSource(), exception.getMessage() == null ? "Reload failed." : exception.getMessage());
            return 0;
        }
    }

    private static int needTierForTarget(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity target = resolveTarget(context.getSource(), net.minecraft.command.argument.StringArgumentType.getString(context, "player"));
        if (target == null) {
            sendError(context.getSource(), format(message("messages.player-notfound", "{player} not found."), "", "", net.minecraft.command.argument.StringArgumentType.getString(context, "player")));
            return 0;
        }

        sendFeedback(context.getSource(), message("messages.enter-tier", "Please enter a tool tier."));
        return 1;
    }

    private static int needTypeForTarget(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity target = resolveTarget(context.getSource(), net.minecraft.command.argument.StringArgumentType.getString(context, "player"));
        if (target == null) {
            sendError(context.getSource(), format(message("messages.player-notfound", "{player} not found."), "", "", net.minecraft.command.argument.StringArgumentType.getString(context, "player")));
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(context.getSource(), net.minecraft.command.argument.StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendError(context.getSource(), message("messages.wrong-material", "Please enter a valid material."));
            return 0;
        }

        sendFeedback(context.getSource(), message("messages.enter-type", "Please enter a tool type (pickaxe, axe, shovel)."));
        return 1;
    }

    private static int giveTargetCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = net.minecraft.command.argument.StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = resolveTarget(source, playerName);
        if (target == null) {
            sendError(source, format(message("messages.player-notfound", "{player} not found."), "", "", playerName));
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(source, net.minecraft.command.argument.StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendError(source, message("messages.wrong-material", "Please enter a valid material."));
            return 0;
        }

        ToolDefinitions.Type type = parseType(source, net.minecraft.command.argument.StringArgumentType.getString(context, "type"));
        if (type == null) {
            sendError(source, message("messages.wrong-tool", "Please enter a valid tool."));
            return 0;
        }

        if (!giveTool(target, tier, type)) {
            return 0;
        }

        sendFeedback(source, message("messages.reloadplugin", "Tool given."));
        return 1;
    }

    public static void openMenu(ServerPlayerEntity player, AmethystMenuScreenHandler.MenuKind kind) {
        Text title = Text.literal(menuTitle(kind));
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory((syncId, inventory, ignoredPlayer) -> new AmethystMenuScreenHandler(syncId, inventory, kind), title);
        player.openHandledScreen(factory);
    }

    public static boolean giveTool(ServerPlayerEntity target, ToolDefinitions.Tier tier, ToolDefinitions.Type type) {
        if (target.getInventory().getEmptySlot() == -1) {
            String material = tier.displayName();
            String tool = type.displayName();
            sendPlayerMessage(target, format(message("messages.invfull", "{material} {tool} could not be added to your inventory because it is full."), material, tool, target.getGameProfile().getName()));
            return false;
        }

        ItemStack stack = ToolDefinitions.createToolStack(CONFIG, tier, type);
        target.giveItemStack(stack);
        target.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 2.0F);
        return true;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return player;
        }

        sendError(source, message("messages.only-players", "Only players can use this command."));
        return null;
    }

    private static ServerPlayerEntity resolveTarget(ServerCommandSource source, String playerName) {
        return source.getServer().getPlayerManager().getPlayer(playerName);
    }

    private static ToolDefinitions.Tier parseTier(ServerCommandSource source, String raw) {
        return ToolDefinitions.Tier.fromId(raw);
    }

    private static ToolDefinitions.Type parseType(ServerCommandSource source, String raw) {
        return ToolDefinitions.Type.fromId(raw);
    }

    private static String menuTitle(AmethystMenuScreenHandler.MenuKind kind) {
        return switch (kind) {
            case ROOT -> message("menus.root", "Amethyst Tools");
            case WOODEN -> message("menus.wooden", "Wooden Tools");
            case STONE -> message("menus.stone", "Stone Tools");
            case IRON -> message("menus.iron", "Iron Tools");
            case COPPER -> message("menus.copper", "Copper Tools");
            case GOLDEN -> message("menus.golden", "Golden Tools");
            case DIAMOND -> message("menus.diamond", "Diamond Tools");
            case NETHERITE -> message("menus.netherite", "Netherite Tools");
        };
    }

    private static void sendFeedback(ServerCommandSource source, String text) {
        source.sendFeedback(() -> Text.literal(text), false);
    }

    private static void sendError(ServerCommandSource source, String text) {
        source.sendError(Text.literal(text));
    }

    private static void sendPlayerMessage(ServerPlayerEntity player, String text) {
        player.sendMessage(Text.literal(text), false);
    }

    private static String message(String key, String fallback) {
        return CONFIG.get(key, fallback);
    }

    private static String format(String template, String material, String tool, String playerName) {
        return template.replace("{material}", material).replace("{tool}", tool).replace("{player}", playerName);
    }

    private static SuggestionsBuilder suggest(SuggestionsBuilder builder, Iterable<String> values) {
        for (String value : values) {
            builder.suggest(value);
        }
        return builder;
    }
}

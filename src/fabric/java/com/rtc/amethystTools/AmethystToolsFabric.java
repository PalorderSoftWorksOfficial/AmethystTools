package com.palordersoftworks.amethysttools;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.StringArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AmethystToolsFabric implements ModInitializer {
    public static final AmethystToolConfig CONFIG = new AmethystToolConfig("amethysttools.properties");
    public static final UpdateChecker UPDATE_CHECKER = new UpdateChecker();
    private static final ToolSystems SYSTEMS = new ToolSystems();

    @Override
    public void onInitialize() {
        CONFIG.load();
        SYSTEMS.register();
        CommandRegistrationCallback.EVENT.register(AmethystToolsFabric::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> UPDATE_CHECKER.checkAsync(server));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            if (!player.hasPermissionLevel(2)) {
                return;
            }
            String latestVersion = UPDATE_CHECKER.latestVersion;
            if (latestVersion == null || !UPDATE_CHECKER.isUpdateAvailable(UPDATE_CHECKER.currentVersion, latestVersion)) {
                return;
            }

            String prefix = "[AmethystTools]";
            player.sendMessage(Text.literal(prefix + " There is a newer plugin version available: " + latestVersion + ", you're on: " + UPDATE_CHECKER.currentVersion).formatted(Formatting.LIGHT_PURPLE), false);
            player.sendMessage(Text.literal(prefix + " Download link: PLACEHOLDER").formatted(Formatting.LIGHT_PURPLE), false);
        });
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("amethysttools")
                .executes(AmethystToolsFabric::openMenuCommand)
                .then(argument("tier", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(ToolDefinitions.tierIds(), builder))
                        .executes(AmethystToolsFabric::tierOnlyCommand)
                        .then(argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(ToolDefinitions.typeIds(), builder))
                                .executes(AmethystToolsFabric::giveSelfCommand))));

        dispatcher.register(literal("amethysttoolsreload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(AmethystToolsFabric::reloadCommand));

        dispatcher.register(literal("amethysttoolsgive")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(AmethystToolsFabric::showGiveUsage)
                .then(argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerManager().getPlayerList().stream().map(player -> player.getGameProfile().getName()).toList(), builder))
                        .executes(AmethystToolsFabric::needTierForTarget)
                        .then(argument("tier", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(ToolDefinitions.tierIds(), builder))
                                .executes(AmethystToolsFabric::needTypeForTarget)
                                .then(argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(ToolDefinitions.typeIds(), builder))
                                        .executes(AmethystToolsFabric::giveTargetCommand)))));
    }

    private static int showGiveUsage(CommandContext<ServerCommandSource> context) {
        String text = message("messages.use-give", "Use: /amethysttoolsgive <player> <tier> <type>");
        sendChatAndActionBar(context.getSource(), text, true);
        return 1;
    }

    private static int openMenuCommand(CommandContext<ServerCommandSource> context) {
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

        String tierInput = StringArgumentType.getString(context, "tier").toLowerCase();
        if (ToolDefinitions.Tier.fromId(tierInput) != null) {
            sendChatAndActionBar(player, message("messages.empty-arg2", "Please enter a tool type (pickaxe, axe, shovel)."), true);
        } else {
            sendChatAndActionBar(player, message("messages.wrong-material", "Please enter a valid material."), true);
        }
        return 1;
    }

    private static int giveSelfCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendChatAndActionBar(player, message("messages.wrong-material", "Please enter a valid material."), true);
            return 0;
        }

        ToolDefinitions.Type type = parseType(StringArgumentType.getString(context, "type"));
        if (type == null) {
            sendChatAndActionBar(player, message("messages.wrong-tool", "Please enter a valid tool."), true);
            return 0;
        }

        return giveTool(player, tier, type) ? 1 : 0;
    }

    private static int reloadCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            CONFIG.load();
            if (source.getEntity() instanceof ServerPlayerEntity player) {
                sendChatAndActionBar(player, message("messages.reloadplugin", "Plugin reload is succesfuly complete."), true);
            } else {
                source.sendFeedback(() -> Text.literal(message("messages.reloadplugin", "Plugin reload is succesfuly complete.")), false);
            }
            return 1;
        } catch (RuntimeException exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Reload failed." : exception.getMessage()));
            return 0;
        }
    }

    private static int needTierForTarget(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = resolveTarget(context.getSource(), playerName);
        if (target == null) {
            sendChatAndActionBar(context.getSource(), format(message("messages.player-notfound", "{player} not found."), "", "", playerName), true);
            return 0;
        }

        sendChatAndActionBar(context.getSource(), message("messages.enter-tier", "Please enter a tool tier."), false);
        return 1;
    }

    private static int needTypeForTarget(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = resolveTarget(context.getSource(), playerName);
        if (target == null) {
            sendChatAndActionBar(context.getSource(), format(message("messages.player-notfound", "{player} not found."), "", "", playerName), true);
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendChatAndActionBar(context.getSource(), message("messages.wrong-material", "Please enter a valid material."), true);
            return 0;
        }

        sendChatAndActionBar(context.getSource(), message("messages.empty-arg2", "Please enter a tool type (pickaxe, axe, shovel)."), false);
        return 1;
    }

    private static int giveTargetCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = resolveTarget(source, playerName);
        if (target == null) {
            sendChatAndActionBar(source, format(message("messages.player-notfound", "{player} not found."), "", "", playerName), true);
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendChatAndActionBar(source, message("messages.wrong-material", "Please enter a valid material."), true);
            return 0;
        }

        ToolDefinitions.Type type = parseType(StringArgumentType.getString(context, "type"));
        if (type == null) {
            sendChatAndActionBar(source, message("messages.wrong-tool", "Please enter a valid tool."), true);
            return 0;
        }

        giveTool(target, tier, type);
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
            String text = format(message("messages.invfull", "{material} {tool} could not be added to your inventory because it is full."), material, tool, target.getGameProfile().getName());
            sendChatAndActionBar(target, text, true);
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

        source.sendError(Text.literal(message("messages.only-players", "Only players can use this command.")));
        return null;
    }

    private static ServerPlayerEntity resolveTarget(ServerCommandSource source, String playerName) {
        return source.getServer().getPlayerManager().getPlayer(playerName);
    }

    private static ToolDefinitions.Tier parseTier(String raw) {
        return ToolDefinitions.Tier.fromId(raw);
    }

    private static ToolDefinitions.Type parseType(String raw) {
        return ToolDefinitions.Type.fromId(raw);
    }

    private static String menuTitle(AmethystMenuScreenHandler.MenuKind kind) {
        return switch (kind) {
            case ROOT -> message("menus.menu-main", "Amethyst Tools");
            case WOODEN -> message("menus.menu-wooden", "Wooden Tools");
            case STONE -> message("menus.menu-stone", "Stone Tools");
            case IRON -> message("menus.menu-iron", "Iron Tools");
            case COPPER -> message("menus.menu-copper", "Copper Tools");
            case GOLDEN -> message("menus.menu-golden", "Golden Tools");
            case DIAMOND -> message("menus.menu-diamond", "Diamond Tools");
            case NETHERITE -> message("menus.menu-netherite", "Netherite Tools");
        };
    }

    private static void sendChatAndActionBar(ServerPlayerEntity player, String text, boolean actionBar) {
        player.sendMessage(Text.literal(text), actionBar);
    }

    private static void sendChatAndActionBar(ServerCommandSource source, String text, boolean actionBar) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.literal(text), actionBar);
            return;
        }

        source.sendFeedback(() -> Text.literal(text), false);
    }

    private static String message(String key, String fallback) {
        return CONFIG.get(key, fallback);
    }

    private static String format(String template, String material, String tool, String playerName) {
        return template.replace("{material}", material).replace("{tool}", tool).replace("{player}", playerName);
    }
}

package com.palordersoftworks.amethysttools;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class AmethystToolsFabric implements ModInitializer {
    public static final String MOD_ID = "amethysttools";
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
            ServerPlayer player = handler.getPlayer();
            if (!isAdmin(player)) {
                return;
            }
            String latestVersion = UPDATE_CHECKER.latestVersion;
            if (latestVersion == null || !UPDATE_CHECKER.isUpdateAvailable(UPDATE_CHECKER.currentVersion, latestVersion)) {
                return;
            }

            String prefix = "[AmethystTools]";
            player.sendSystemMessage(Component.literal(prefix + " There is a newer plugin version available: " + latestVersion + ", you're on: " + UPDATE_CHECKER.currentVersion).withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.literal(prefix + " Download link: PLACEHOLDER").withStyle(ChatFormatting.LIGHT_PURPLE));
        });
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("amethysttools")
                .executes(AmethystToolsFabric::openMenuCommand)
                .requires(AmethystToolsFabric::isAdminSource)
                .then(argument("tier", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ToolDefinitions.tierIds(), builder))
                        .executes(AmethystToolsFabric::tierOnlyCommand)
                        .then(argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ToolDefinitions.typeIds(), builder))
                                .executes(AmethystToolsFabric::giveSelfCommand))));

        dispatcher.register(literal("amethysttoolsreload")
                .requires(AmethystToolsFabric::isAdminSource)
                .executes(AmethystToolsFabric::reloadCommand));

        dispatcher.register(literal("amethysttoolsgive")
                .requires(AmethystToolsFabric::isAdminSource)
                .executes(AmethystToolsFabric::showGiveUsage)
                .then(argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().getPlayerList().getPlayers().stream().map(player -> player.getName().getString()).toList(),
                                builder))
                        .executes(AmethystToolsFabric::needTierForTarget)
                        .then(argument("tier", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ToolDefinitions.tierIds(), builder))
                                .executes(AmethystToolsFabric::needTypeForTarget)
                                .then(argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ToolDefinitions.typeIds(), builder))
                                        .executes(AmethystToolsFabric::giveTargetCommand)))));
    }

    private static boolean isAdminSource(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && player.canUseGameMasterBlocks();
    }

    private static boolean isAdmin(ServerPlayer player) {
        return player != null && player.canUseGameMasterBlocks();
    }

    private static int showGiveUsage(CommandContext<CommandSourceStack> context) {
        String text = message("messages.use-give", "Use: /amethysttoolsgive <player> <tier> <type>");
        sendChatAndActionBar(context.getSource(), text, true);
        return 1;
    }

    private static int openMenuCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = requirePlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        openMenu(player, AmethystMenuScreenHandler.MenuKind.ROOT);
        player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.0F);
        return 1;
    }

    private static int tierOnlyCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = requirePlayer(context.getSource());
        if (player == null) {
            return 0;
        }

        String tierInput = StringArgumentType.getString(context, "tier").toLowerCase();
        if (ToolDefinitions.Tier.fromId(tierInput) != null) {
            sendChatAndActionBar(player, message("messages.empty-arg2", "Please enter a tool type (pickaxe, axe, shovel, hoe, sword)."), true);
        } else {
            sendChatAndActionBar(player, message("messages.wrong-material", "Please enter a valid material."), true);
        }
        return 1;
    }

    private static int giveSelfCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = requirePlayer(context.getSource());
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

    private static int reloadCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            CONFIG.load();
            if (source.getEntity() instanceof ServerPlayer player) {
                sendChatAndActionBar(player, message("messages.reloadplugin", "Plugin reload is succesfuly complete."), true);
            } else {
                source.sendSuccess(() -> Component.literal(message("messages.reloadplugin", "Plugin reload is succesfuly complete.")), false);
            }
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal(exception.getMessage() == null ? "Reload failed." : exception.getMessage()));
            return 0;
        }
    }

    private static int needTierForTarget(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer target = resolveTarget(context.getSource(), playerName);
        if (target == null) {
            sendChatAndActionBar(context.getSource(), format(message("messages.player-notfound", "{player} not found."), "", "", playerName), true);
            return 0;
        }

        sendChatAndActionBar(context.getSource(), message("messages.enter-tier", "Please enter a tool tier."), false);
        return 1;
    }

    private static int needTypeForTarget(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer target = resolveTarget(context.getSource(), playerName);
        if (target == null) {
            sendChatAndActionBar(context.getSource(), format(message("messages.player-notfound", "{player} not found."), "", "", playerName), true);
            return 0;
        }

        ToolDefinitions.Tier tier = parseTier(StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            sendChatAndActionBar(context.getSource(), message("messages.wrong-material", "Please enter a valid material."), true);
            return 0;
        }

        sendChatAndActionBar(context.getSource(), message("messages.empty-arg2", "Please enter a tool type (pickaxe, axe, shovel, hoe, sword)."), false);
        return 1;
    }

    private static int giveTargetCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer target = resolveTarget(source, playerName);
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

    public static void openMenu(ServerPlayer player, AmethystMenuScreenHandler.MenuKind kind) {
        Component title = Component.literal(menuTitle(kind));
        MenuProvider factory = new SimpleMenuProvider((syncId, inventory, ignoredPlayer) -> new AmethystMenuScreenHandler(syncId, inventory, kind), title);
        player.openMenu(factory);
    }

    public static boolean giveTool(ServerPlayer target, ToolDefinitions.Tier tier, ToolDefinitions.Type type) {
        if (target.getInventory().getFreeSlot() == -1) {
            String material = tier.displayName();
            String tool = type.displayName();
            String text = format(message("messages.invfull", "{material} {tool} could not be added to your inventory because it is full."), material, tool, "");
            sendChatAndActionBar(target, text, true);
            return false;
        }

        ItemStack stack = ToolDefinitions.createToolStack(CONFIG, tier, type);
        target.getInventory().add(stack);
        target.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 2.0F);
        return true;
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }

        source.sendFailure(Component.literal(message("messages.only-players", "Only players can use this command.")));
        return null;
    }

    private static ServerPlayer resolveTarget(CommandSourceStack source, String playerName) {
        return source.getServer().getPlayerList().getPlayer(playerName);
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

    private static void sendChatAndActionBar(ServerPlayer player, String text, boolean actionBar) {
        player.sendSystemMessage(Component.literal(text), actionBar);
    }

    private static void sendChatAndActionBar(CommandSourceStack source, String text, boolean actionBar) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal(text), actionBar);
            return;
        }

        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static String message(String key, String fallback) {
        return CONFIG.get(key, fallback);
    }

    private static String format(String template, String material, String tool, String playerName) {
        return template.replace("{material}", material).replace("{tool}", tool).replace("{player}", playerName);
    }
}

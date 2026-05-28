package com.rtc.amethystTools.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rtc.amethystTools.AmethystTools;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings({"deprecation", "unused", "SpellCheckingInspection"})
public class AmethystToolReloadCommand {

    private final AmethystTools plugin;

    public AmethystToolReloadCommand(AmethystTools plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("UnstableApiUsage")
    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("amethysttoolsreload")
                .requires(source -> source.getSender().hasPermission("amethysttools.admin"))
                .executes(this::executeReload);
    }


    @SuppressWarnings("UnstableApiUsage")
    private int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        plugin.reloadConfig();

        if (sender instanceof Player player) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reloadplugin", "&aPlugin reload is succesfuly complete.")));
            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reloadplugin-actionbar", "&aReloaded Plugin")));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reloadplugin", "&aPlugin reload is succesfuly complete.")));
        }
        return Command.SINGLE_SUCCESS;
    }
}
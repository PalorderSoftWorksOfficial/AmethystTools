package com.rtc.amethystTools.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rtc.amethystTools.AmethystTools;
import com.rtc.amethystTools.utils.ToolUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"deprecation", "unused", "SpellCheckingInspection", "SameReturnValue", "UnstableApiUsage"})
public class AmethystToolsGiveCommand {

    private static final List<String> TIER = new ArrayList<>();
    static {
        TIER.add("wooden");
        TIER.add("stone");
        TIER.add("iron");
        TIER.add("golden");
        TIER.add("diamond");
        TIER.add("netherite");

        if (Material.matchMaterial("COPPER_PICKAXE") != null) {
            TIER.add("copper");
        }
    }
    private static final List<String> TYPE = Arrays.asList(
            "pickaxe", "axe", "shovel"
    );

    private final AmethystTools plugin;

    public AmethystToolsGiveCommand(AmethystTools plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("amethysttoolsgive")
                .requires(source -> source.getSender().hasPermission("amethysttools.give"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.use-give", "&cUse: &7/amethysttoolsgive (player) (tier) (type)")));
                    if (sender instanceof Player player) {
                        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.use-give", "&cUse: &7/amethysttoolsgive (player) (tier) (type)")));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String remaining = builder.getRemaining().toLowerCase();
                            Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(remaining))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.enter-tier", "&cPlease enter a tool tier.")));
                            if (sender instanceof Player player) {
                                player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.enter-tier", "&cPlease enter a tool tier.")));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("tier", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    TIER.stream().filter(tier -> tier.startsWith(remaining)).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    CommandSender sender = context.getSource().getSender();
                                    String tierInput = StringArgumentType.getString(context, "tier").toLowerCase();

                                    if (!TIER.contains(tierInput)) {
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.wrong-material", "&cPlease enter a valid material.")));
                                        if (sender instanceof Player player) {
                                            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.wrong-material", "&cPlease enter a valid material.")));
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.empty-arg2", "&cPlease enter a tool type (pickaxe, axe, shovel).")));
                                        if (sender instanceof Player player) {
                                            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.empty-arg2", "&cPlease enter a tool type (pickaxe, axe, shovel).")));
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String remaining = builder.getRemaining().toLowerCase();
                                            TYPE.stream().filter(type -> type.startsWith(remaining)).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::executeGive)
                                )
                        )
                );
    }

    private int executeGive(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        String targetName = StringArgumentType.getString(context, "player");
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-notfound", "&c{player} not found.").replace("{player}", targetName)));
            if (sender instanceof Player player) {
                player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-notfound-actionbar", "&cPlayer not found")));
            }
            return Command.SINGLE_SUCCESS;
        }

        String argtier = StringArgumentType.getString(context, "tier").toLowerCase();
        String argtype = StringArgumentType.getString(context, "type").toLowerCase();

        if (!TIER.contains(argtier)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.wrong-material", "&cPlease enter a valid material.")));
            if (sender instanceof Player player) {
                player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.wrong-material", "&cPlease enter a valid material.")));
            }
            return Command.SINGLE_SUCCESS;
        }

        if (!TYPE.contains(argtype)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.wrong-tool", "&cPlease enter a valid alet.")));
            if (sender instanceof Player player) {
                player.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.wrong-tool", "&cPlease enter a valid alet.")));
            }
            return Command.SINGLE_SUCCESS;
        }

        if (target.getInventory().firstEmpty() == -1) {
            String invfullmsgtier = ToolUtils.getTierName(argtier);
            String invfullmsgtool = ToolUtils.getTypeName(argtype);

            target.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invfull", "&c{material} {tool} could not be added to your inventory because it is full.")).replace("{material}", invfullmsgtier).replace("{tool}", invfullmsgtool));
            target.sendActionBar(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invfull-actionbar", "&cYour inventory is full!")));
            return Command.SINGLE_SUCCESS;
        }

        Material mat = ToolUtils.getMaterial(argtier, argtype);
        if (mat == null) return Command.SINGLE_SUCCESS;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Command.SINGLE_SUCCESS;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(AmethystTools.KEY_TOOL, PersistentDataType.BYTE, (byte) 1);
        pdc.set(AmethystTools.KEY_TIER, PersistentDataType.STRING, argtier);
        pdc.set(AmethystTools.KEY_TYPE, PersistentDataType.STRING, argtype);

        String toolDisplayName = switch (argtype) {
            case "pickaxe" -> plugin.getConfig().getString("item.item-pickaxe", "&5ᴘɪᴄᴋᴀхᴇ");
            case "axe" -> plugin.getConfig().getString("item.item-axe", "&5ᴀхᴇ");
            case "shovel" -> plugin.getConfig().getString("item.item-shovel", "&5ѕʜᴏᴠᴇʟ");
            default -> "&cERROR";
        };

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("item.item-name", "&5ᴀᴍᴇᴛʜʏѕᴛ") + " " + toolDisplayName));
        meta.setLore(List.of("§7Breaks 9 Blocks at Once"));
        meta.setCustomModelData(2235897);
        item.setItemMeta(meta);

        target.getInventory().addItem(item);
        target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 2.0f);

        return Command.SINGLE_SUCCESS;
    }
}
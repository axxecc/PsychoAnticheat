package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.player.PsychoPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AlertsCommand implements SubCommand {
    private final Psycho plugin;

    public AlertsCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "alerts";
    }

    @Override
    public String getPermission() {
        return "psycho.command.alerts";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return;
        }

        PsychoPlayer psychoPlayer = plugin.getConnectionListener().getPlayer(player.getUniqueId());

        if (psychoPlayer == null) {
            sender.sendMessage("§cPlayer not registered.");
            return;
        }

        psychoPlayer.setSendAlerts(!psychoPlayer.isSendAlerts());
        plugin.getPlayerTrackerService().trackSnapshot(psychoPlayer);

        if (psychoPlayer.isSendAlerts()) {
            sender.sendMessage("§aAlerts enabled.");
        } else {
            sender.sendMessage("§cAlerts disabled.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

package com.projectkorra.probending.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.projectkorra.probending.PBMessenger;
import com.projectkorra.probending.PBMessenger.PBMessage;
import com.projectkorra.probending.enums.GamePlayerMode;

import net.md_5.bungee.api.ChatColor;

public class QueueJoinCommand extends PBCommand {

	public QueueJoinCommand() {
		super("queue", "Join the queue for 1v1 or 3v3 matches", "/probending queue {1/3}", new String[] {"queue", "q"});
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if (!sender.hasPermission("probending.command.join")) {
			sender.sendMessage(ChatColor.RED + "Insufficient Permissions");
			return;
		}
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args.size() > 0) {
				if (args.get(0).equalsIgnoreCase("1")) {
					Commands.getQueueManager().quePlayer(player, GamePlayerMode.SINGLE);
				} else if (args.get(0).equalsIgnoreCase("3")) {
					Commands.getQueueManager().quePlayer(player, GamePlayerMode.TRIPLE);
				} else {
					PBMessenger.sendMessage(player, PBMessage.ERROR);
				}
			} else {
				Commands.getQueueManager().quePlayer(player, GamePlayerMode.ANY);
			}
		} else {
			sender.sendMessage(ChatColor.RED + "Only players can use this command!");
		}
	}
}
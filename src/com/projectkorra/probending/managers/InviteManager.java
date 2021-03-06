package com.projectkorra.probending.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.projectkorra.probending.PBMessenger;
import com.projectkorra.probending.Probending;
import com.projectkorra.probending.libraries.database.Callback;
import com.projectkorra.probending.objects.PBTeam;
import com.projectkorra.probending.objects.PBTeam.TeamMemberRole;
import com.projectkorra.probending.storage.DBProbendingTeam;

import net.md_5.bungee.api.ChatColor;

public class InviteManager {
	
	private DBProbendingTeam teamDB;
	private Map<UUID, List<Invitation>> invites = new HashMap<>();

	public InviteManager(DBProbendingTeam teamDB) {
		this.teamDB = teamDB;
	}
	
	public void handleJoin(final Player player) {
		teamDB.getInvitationsByUUIDAsync(player.getUniqueId(), new Callback<List<Invitation>>() {
			public void run(List<Invitation> list) {
				invites.put(player.getUniqueId(), list);
			}
		});
	}
	
	public void handleQuit(Player player) {
		invites.remove(player.getUniqueId());
	}
	
	public void handleTeamDeletion(PBTeam team) {
		Iterator<Entry<UUID, List<Invitation>>> inviteIterator = invites.entrySet().iterator();
		while (inviteIterator.hasNext()) {
			List<Invitation> invites = inviteIterator.next().getValue();
			Iterator<Invitation> invitesIterator = invites.iterator();
			while (invitesIterator.hasNext()) {
				int teamID = invitesIterator.next().getTeamID();
				if (teamID == team.getID()) {
					invitesIterator.remove();
				}
			}
			if (invites.isEmpty()) {
				inviteIterator.remove();
			}
		}
	}
	
	public boolean hasInvitations(Player player) {
		return invites.containsKey(player.getUniqueId());
	}
	
	/**
	 * 
	 * @param player Player checked for invitations
	 * @return A list of invitations
	 */
	public List<Invitation> getInvitations(Player player) {
		return hasInvitations(player) ? invites.get(player.getUniqueId()) : new ArrayList<Invitation>();
	}
	
	public void removeInvitation(Player player, Invitation invite) {
		if (hasInvitations(player)) {
			invites.get(player.getUniqueId()).remove(invite);
		}
		PBTeam team = Probending.get().getTeamManager().getTeamFromID(invite.getTeamID());
		final UUID playerUUID = player.getUniqueId();
		final String teamName = team.getTeamName();
		teamDB.removeInvitationAsync(player.getUniqueId(), Probending.get().getTeamManager().getTeamFromName(team.getTeamName()), new Runnable() {
			public void run() {
				if (Bukkit.getPlayer(playerUUID) != null) {
					PBMessenger.sendMessage(Bukkit.getPlayer(playerUUID), ChatColor.translateAlternateColorCodes('&', ChatColor.GOLD + "Your invitation to join team " + teamName + " has been revoked!"), true);
				}
			}
		});
	}
	
	public void sendInvitation(Player player, int id, String role) {
		final Invitation invite = new Invitation(id, role);
		if (!invites.containsKey(player.getUniqueId())) {
			invites.put(player.getUniqueId(), new ArrayList<Invitation>());
		}
		invites.get(player.getUniqueId()).add(invite);
		final UUID playerUUID = player.getUniqueId();
		teamDB.addInvitationAsync(player.getUniqueId(), Probending.get().getTeamManager().getTeamFromID(id), role, new Runnable() {
			public void run() {
				if (Bukkit.getPlayer(playerUUID) != null) {
					notifyPlayerSpecific(Bukkit.getPlayer(playerUUID), invite);
				}
			}
		});
	}
	
	public void notifyPlayerSpecific(Player player, Invitation invite) {
		PBTeam team = Probending.get().getTeamManager().getTeamFromID(invite.getTeamID());
		TeamMemberRole role = TeamMemberRole.parseRole(invite.getRole());
		String message = ChatColor.GOLD + "You have an invitation to join " + ChatColor.GREEN + team.getTeamName() + ChatColor.GOLD + " as their " + role.getDisplay() + "\n"
				   + ChatColor.GOLD + "Use /pb team join " + ChatColor.GREEN + team.getTeamName() + ChatColor.GOLD + " to accept the invitation! Notice: You must leave your current team if you have one!";
		PBMessenger.sendMessage(player, ChatColor.translateAlternateColorCodes('&', message), true);
	}
	
	public void notifyPlayer(Player player, boolean showAll) {
		if (!hasInvitations(player)) {
			return;
		}
		
		List<Invitation> invitations = invites.get(player.getUniqueId());
		if (!showAll) {
			String message = ChatColor.GOLD + "You have invitation" + (invitations.size() > 1 ? "s " : " ") + "to join " + invitations.size() + " team" + (invitations.size() > 1 ? "s!" : "!");
			PBMessenger.sendMessage(player, ChatColor.translateAlternateColorCodes('&', message), true);
		} else {
			PBMessenger.sendMessage(player, ChatColor.translateAlternateColorCodes('&', ChatColor.GOLD + "You have active invitations to the following teams:"), true);
			for (Invitation invite : invitations) {
				PBTeam team = Probending.get().getTeamManager().getTeamFromID(invite.getTeamID());
				TeamMemberRole role = TeamMemberRole.parseRole(invite.getRole());
				String message = ChatColor.GOLD + "- " + ChatColor.GREEN + team.getTeamName() + ChatColor.GOLD + " as their " + role.getDisplay();
				PBMessenger.sendMessage(player, ChatColor.translateAlternateColorCodes('&', message), true);
			}
			PBMessenger.sendMessage(player, ChatColor.translateAlternateColorCodes('&', ChatColor.GOLD + "Use /pb join <Team> to accept an invitation! Notice: You must leave your current team if you have one!"), true);
		}
	}
	
	public static class Invitation {
		
		private int teamID;
		private String role;
		
		public Invitation(int teamId, String role) {
			this.teamID = teamId;
			this.role = role;
		}
		
		public int getTeamID() {
			return teamID;
		}
		
		public String getRole() {
			return role;
		}
	}
}
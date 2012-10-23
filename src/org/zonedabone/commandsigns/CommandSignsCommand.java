package org.zonedabone.commandsigns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class CommandSignsCommand implements CommandExecutor {

	private CommandSigns plugin;

	public CommandSignsCommand(CommandSigns plugin) {
		this.plugin = plugin;
	}

	protected boolean add(final CommandSender sender, Player player,
			int lineNumber, String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.create.regular")) {
			if (lineNumber <= 0) {
				Messaging.sendMessage(player, "failure.invalid_line");
			}
			if (plugin.playerStates.get(player) == CommandSignsPlayerState.EDIT_SELECT) {
				Messaging.sendMessage(player, "failure.must_select");
			}
			CommandSignsText text = plugin.playerText.get(player);
			if (text == null) {
				text = new CommandSignsText(player.getName(), false);
				plugin.playerText.put(player, text);
			}
			String line = StringUtils.join(args, " ", 1, args.length);
			if (line.startsWith("/*")
					&& !plugin.hasPermission(player,
							"commandsigns.create.super", false)) {
				Messaging.sendMessage(player, "failure.no_super");
			}
			if ((line.startsWith("/^") || line.startsWith("/#"))
					&& !plugin.hasPermission(player, "commandsigns.create.op",
							false)) {
				Messaging.sendMessage(player, "failure.no_op");
			}
			text.setLine(lineNumber, line);
			text.trim();
			String display = line.replace("$", "\\$");
			Messaging.sendRaw(player, "success.line_print", "n", ""
					+ lineNumber, "l", display);
			if (plugin.playerStates.get(player) != CommandSignsPlayerState.EDIT) {
				plugin.playerStates.put(player, CommandSignsPlayerState.ENABLE);
				Messaging.sendMessage(player, "progress.add");
			}
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean batch(final CommandSender sender, Player player,
			String[] args) {
		CommandSignsPlayerState ps = plugin.playerStates.get(player);
		switch (ps) {
		case REMOVE:
			player.sendMessage("Switched to batch remove mode.");
			ps = CommandSignsPlayerState.BATCH_REMOVE;
			break;
		case BATCH_REMOVE:
			player.sendMessage("Switched to single remove mode.");
			ps = CommandSignsPlayerState.REMOVE;
			break;
		case ENABLE:
			player.sendMessage("Switched to batch enable mode.");
			ps = CommandSignsPlayerState.BATCH_ENABLE;
			break;
		case BATCH_ENABLE:
			player.sendMessage("Switched to single enable mode.");
			ps = CommandSignsPlayerState.ENABLE;
			break;
		case READ:
			player.sendMessage("Switched to batch read mode.");
			ps = CommandSignsPlayerState.BATCH_READ;
			break;
		case BATCH_READ:
			player.sendMessage("Switched to single read mode.");
			ps = CommandSignsPlayerState.READ;
			break;
		case TOGGLE:
			player.sendMessage("Switched to batch toggle mode.");
			ps = CommandSignsPlayerState.BATCH_TOGGLE;
			break;
		case BATCH_TOGGLE:
			player.sendMessage("Switched to single toggle mode.");
			ps = CommandSignsPlayerState.TOGGLE;
			break;
		case REDSTONE:
			player.sendMessage("Switched to batch redstone mode.");
			ps = CommandSignsPlayerState.BATCH_REDSTONE;
			break;
		case BATCH_REDSTONE:
			player.sendMessage("Switched to single redstone mode.");
			ps = CommandSignsPlayerState.REDSTONE;
			break;
		default:
			player.sendMessage(ChatColor.RED
					+ "The mode you are in doesn't support batch processing.");
		}
		plugin.playerStates.put(player, ps);
		return true;
	}

	protected boolean clear(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.remove")) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT
					|| ps == CommandSignsPlayerState.EDIT_SELECT) {
				finishEditing(player);
			}
			plugin.playerStates.remove(player);
			plugin.playerText.remove(player);
			Messaging.sendMessage(player, "success.cleared");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean copy(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.create.regular")) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT
					|| ps == CommandSignsPlayerState.EDIT_SELECT) {
				finishEditing(player);
			}
			plugin.playerStates.put(player, CommandSignsPlayerState.COPY);
			Messaging.sendMessage(player, "progress.copy");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean edit(final CommandSender sender, Player player,
			String[] args) {
		if (plugin.hasPermission(sender, "commandsigns.edit", false)) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT_SELECT
					|| ps == CommandSignsPlayerState.EDIT) {
				finishEditing(player);
			} else {
				plugin.playerStates.put(player,
						CommandSignsPlayerState.EDIT_SELECT);
				plugin.playerText.remove(player);
				Messaging.sendMessage(player, "progress.select_sign");
			}
		}
		return true;
	}

	public void finishEditing(Player player) {
		plugin.playerStates.remove(player);
		plugin.playerText.remove(player);
		Messaging.sendMessage(player, "success.done_editing");
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("commandsigns")) {
			if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
				// Messaging.sendMessage(sender, "usage");
				return false;
			}
			Player tp = null;
			if (sender instanceof Player) {
				tp = (Player) sender;
			}
			final Player player = tp;
			String command = args[0].toLowerCase();
			Pattern pattern = Pattern.compile("(line|l|)(\\d+)");
			Matcher matcher = pattern.matcher(command);
			if (matcher.matches()) {
				return add(sender, player, Integer.parseInt(matcher.group(2)),
						args);
			} else if (command.equals("batch")) {
				return batch(sender, player, args);
			} else if (args[0].equalsIgnoreCase("clear")) {
				return clear(sender, player, args);
			} else if (args[0].equalsIgnoreCase("copy")) {
				return copy(sender, player, args);
			} else if (args[0].equalsIgnoreCase("edit")) {
				return edit(sender, player, args);
			} else if (args[0].equalsIgnoreCase("read")) {
				return read(sender, player, args);
			} else if (args[0].equalsIgnoreCase("redstone")) {
				return redstone(sender, player, args);
			} else if (args[0].equalsIgnoreCase("reload")) {
				return reload(sender, player, args);
			} else if (args[0].equalsIgnoreCase("remove")) {
				return remove(sender, player, args);
			} else if (args[0].equalsIgnoreCase("save")) {
				return save(sender, player, args);
			} else if (args[0].equalsIgnoreCase("toggle")) {
				return toggle(sender, player, args);
			} else if (args[0].equalsIgnoreCase("update")) {
				return update(sender, player, args);
			} else if (args[0].equalsIgnoreCase("view")) {
				return view(sender, player, args);
			} else {
				Messaging.sendMessage(sender, "failure.wrong_syntax");
				return true;
			}
		}
		return false;
	}

	protected boolean read(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.create.regular")) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT
					|| ps == CommandSignsPlayerState.EDIT_SELECT) {
				finishEditing(player);
			}
			plugin.playerStates.put(player, CommandSignsPlayerState.READ);
			Messaging.sendMessage(player, "progress.read");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean redstone(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.create.redstone")) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT
					|| ps == CommandSignsPlayerState.EDIT_SELECT) {
				finishEditing(player);
			}
			plugin.playerStates.put(player, CommandSignsPlayerState.REDSTONE);
			Messaging.sendMessage(player, "progress.redstone");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean reload(final CommandSender sender, Player player,
			String[] args) {
		if (plugin.hasPermission(sender, "commandsigns.reload", false)) {
			Messaging.loadMessages(plugin);
			plugin.loadFile();
			plugin.startMetrics();
			plugin.setupPermissions();
			plugin.setupEconomy();
			Messaging.sendMessage(sender, "success.reloaded");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean remove(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.remove")) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT
					|| ps == CommandSignsPlayerState.EDIT_SELECT) {
				finishEditing(player);
			}
			plugin.playerStates.put(player, CommandSignsPlayerState.REMOVE);
			Messaging.sendMessage(player, "progress.remove");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean save(final CommandSender sender, Player player,
			String[] args) {
		if (plugin.hasPermission(sender, "commandsigns.save", false)) {
			plugin.saveFile();
			Messaging.sendMessage(sender, "success.saved");
		}
		return true;
	}

	protected boolean toggle(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.toggle")) {
			CommandSignsPlayerState ps = plugin.playerStates.get(player);
			if (ps == CommandSignsPlayerState.EDIT
					|| ps == CommandSignsPlayerState.EDIT_SELECT) {
				finishEditing(player);
			}
			plugin.playerStates.put(player, CommandSignsPlayerState.TOGGLE);
			Messaging.sendMessage(player, "progress.toggle");
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}

	protected boolean update(final CommandSender sender, Player player,
			String[] args) {
		if (sender.hasPermission("commandsigns.update")) {
			if (args.length == 2 && args[1].equalsIgnoreCase("force")) {
				// Force-only. Does no check.
				Messaging.sendMessage(sender, "update.force");
				plugin.updateHandler.new Updater(sender).start();
			} else {
				// Preliminary check
				Messaging.sendMessage(sender, "update.check");
				new Thread() {

					@Override
					public void run() {
						plugin.updateHandler.new Checker().run();
						if (plugin.updateHandler.newAvailable) {
							Messaging.sendMessage(sender, "update.notify", "v",
									plugin.updateHandler.newestVersion
											.toString());
						} else {
							Messaging.sendMessage(sender,
									"update.confirm_up_to_date", "v",
									plugin.updateHandler.currentVersion
											.toString());
						}
					}
				}.start();

				// If command was 'update check', stop here. Enough has been
				// done.
				if (!(args.length == 2 && args[1].equalsIgnoreCase("check"))) {
					if (plugin.updateHandler.newAvailable) {
						if (!plugin.getUpdateFile().exists()) {
							Messaging.sendMessage(sender, "update.start", "v",
									plugin.updateHandler.newestVersion
											.toString());
							plugin.updateHandler.new Updater(sender).start();
						} else {
							Messaging.sendMessage(sender,
									"update.already_downloaded");
						}
					}
				}
			}
		}
		return true;
	}

	protected boolean view(final CommandSender sender, Player player,
			String[] args) {
		if (player == null) {
			Messaging.sendMessage(sender, "failure.player_only");
		}
		if (plugin.hasPermission(player, "commandsigns.create.regular")) {
			CommandSignsText text = plugin.playerText.get(player);
			if (text == null) {
				player.sendMessage("No text in clipboard");
			}
			int i = 0;
			for (String s : text.getText()) {
				if (!s.equals("")) {
					player.sendMessage(i + ": " + s);
				}
				i++;
			}
			plugin.playerStates.remove(player);
		} else {
			Messaging.sendMessage(player, "failure.no_perms");
		}
		return true;
	}
}

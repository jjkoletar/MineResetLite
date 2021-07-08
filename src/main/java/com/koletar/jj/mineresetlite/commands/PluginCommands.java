package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.Command;
import com.koletar.jj.mineresetlite.MineResetLite;
import org.bukkit.command.CommandSender;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class PluginCommands {
    private MineResetLite plugin;

    public PluginCommands(MineResetLite plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"about"},
            description = "List version and project information about MRL",
            permissions = {},
            help = {"Show version information about this installation of MRL, in addition", "to the authors of the plugin."},
            min = 0, max = 0, onlyPlayers = false)
    public void about(CommandSender sender, String[] args) {
        sender.sendMessage(phrase("aboutTitle"));
        sender.sendMessage(phrase("aboutAuthors"));
        sender.sendMessage(phrase("aboutVersion", plugin.getDescription().getVersion()));
    }
}

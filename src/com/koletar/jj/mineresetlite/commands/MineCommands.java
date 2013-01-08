package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.Command;
import com.koletar.jj.mineresetlite.InvalidCommandArgumentsException;
import com.koletar.jj.mineresetlite.Mine;
import com.koletar.jj.mineresetlite.MineResetLite;
import com.koletar.jj.mineresetlite.SerializableBlock;
import com.koletar.jj.mineresetlite.StringTools;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class MineCommands {
    private MineResetLite plugin;
    private Map<Player, Location> point1;
    private Map<Player, Location> point2;

    public MineCommands(MineResetLite plugin) {
        this.plugin = plugin;
        point1 = new HashMap<Player, Location>();
        point2 = new HashMap<Player, Location>();
    }

    @Command(aliases = {"list", "l"},
            description = "List the names of all Mines",
            permissions = {"mineresetlite.mine.list"},
            help = {"List the names of all Mines currently created, across all worlds."},
            min = 0, max = 0, onlyPlayers = false)
    public void listMines(CommandSender sender, String[] args) {
        sender.sendMessage(phrase("mineList", StringTools.buildList(plugin.mines, "&c", "&d, ")));
    }

    @Command(aliases = {"pos1", "p1"},
            description = "Change your first selection point",
            help = {"Run this command to set your first selection point to the block you are looking at.",
                    "Use /mrl pos1 -feet to set your first point to the location you are standing on."},
            usage = "(-feet)",
            permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
            min = 0, max = 1, onlyPlayers = true)
    public void setPoint1(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
        Player player = (Player) sender;
        if (args.length == 0) {
            //Use block being looked at
            point1.put(player, player.getTargetBlock(null, 100).getLocation());
            player.sendMessage(phrase("firstPointSet"));
            return;
        } else if (args[0].equalsIgnoreCase("-feet")) {
            //Use block being stood on
            point1.put(player, player.getLocation());
            player.sendMessage(phrase("firstPointSet"));
            return;
        }
        //Args weren't empty or -feet, bad args
        throw new InvalidCommandArgumentsException();
    }

    @Command(aliases = {"pos2", "p2"},
            description = "Change your first selection point",
            help = {"Run this command to set your second selection point to the block you are looking at.",
                    "Use /mrl pos2 -feet to set your second point to the location you are standing on."},
            usage = "(-feet)",
            permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
            min = 0, max = 1, onlyPlayers = true)
    public void setPoint2(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
        Player player = (Player) sender;
        if (args.length == 0) {
            //Use block being looked at
            point2.put(player, player.getTargetBlock(null, 100).getLocation());
            player.sendMessage(phrase("secondPointSet"));
            return;
        } else if (args[0].equalsIgnoreCase("-feet")) {
            //Use block being stood on
            point2.put(player, player.getLocation());
            player.sendMessage(phrase("secondPointSet"));
            return;
        }
        //Args weren't empty or -feet, bad args
        throw new InvalidCommandArgumentsException();
    }

    @Command(aliases = {"create", "save"},
            description = "Create a mine from either your WorldEdit selection or by manually specifying the points",
            help = {"Provided you have a selection made via either WorldEdit or selecting the points using MRL,",
                    "an empty mine will be created. This mine will have no composition and default settings."},
            usage = "<mine name>",
            permissions = {"mineresetlite.mine.create"},
            min = 1, max = -1, onlyPlayers = true)
    public void createMine(CommandSender sender, String[] args) {
        //Find out how they selected the region
        Player player = (Player) sender;
        World world = null;
        Vector p1 = null;
        Vector p2 = null;
        //Native selection techniques?
        if (point1.containsKey(player) && point2.containsKey(player)) {
            world = point1.get(player).getWorld();
            if (!world.equals(point2.get(player).getWorld())) {
                player.sendMessage(phrase("crossWorldSelection"));
                return;
            }
            p1 = point1.get(player).toVector();
            p2 = point2.get(player).toVector();
        }
        //WorldEdit?
        if (plugin.hasWorldEdit() && plugin.getWorldEdit().getSelection(player) != null) {
            WorldEditPlugin worldEdit = plugin.getWorldEdit();
            Selection selection = worldEdit.getSelection(player);
            world = selection.getWorld();
            p1 = selection.getMinimumPoint().toVector();
            p2 = selection.getMaximumPoint().toVector();
        }
        if (p1 == null) {
            player.sendMessage(phrase("emptySelection"));
            return;
        }
        //Construct mine name
        String name = StringTools.buildSpacedArgument(args);
        //Verify uniqueness of mine name
        Mine[] mines = plugin.matchMines(name);
        if (mines.length > 0) {
            player.sendMessage(phrase("nameInUse", name));
            return;
        }
        //Sort coordinates
        if (p1.getX() > p2.getX()) {
            //Swap
            double x = p1.getX();
            p1.setX(p2.getX());
            p2.setX(x);
        }
        if (p1.getY() > p2.getY()) {
            double y = p1.getY();
            p1.setY(p2.getY());
            p2.setY(y);
        }
        if (p1.getZ() > p2.getZ()) {
            double z = p1.getZ();
            p1.setZ(p2.getZ());
            p2.setZ(z);
        }
        //Create!
        Mine newMine = new Mine(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(), name, world);
        plugin.mines.add(newMine);
        player.sendMessage(phrase("mineCreated", newMine));
        plugin.buffSave();
    }

    @Command(aliases = {"info", "i"},
            description = "List information about a mine",
            usage = "<mine name>",
            permissions = {"mineresetlite.mine.info"},
            min = 1, max = -1, onlyPlayers = false)
    public void mineInfo(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        sender.sendMessage(phrase("mineInfoName", mines[0]));
        sender.sendMessage(phrase("mineInfoWorld", mines[0].getWorld()));
        //Build composition list
        StringBuilder csb = new StringBuilder();
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            csb.append(entry.getValue().doubleValue() * 100);
            csb.append("% ");
            csb.append(Material.getMaterial(entry.getKey().getBlockId()).toString());
            if (entry.getKey().getData() != 0) {
                csb.append(":");
                csb.append(entry.getKey().getData());
            }
            csb.append(", ");
        }
        if (csb.length() > 2) {
            csb.delete(csb.length() - 2, csb.length() - 1);
        }
        sender.sendMessage(phrase("mineInfoComposition", csb));
        if (mines[0].getResetDelay() != 0) {
            sender.sendMessage(phrase("mineInfoResetDelay", mines[0].getResetDelay()));
        }
        if (mines[0].getResetWarnings().size() > 0) {
            sender.sendMessage(phrase("mineInfoWarningTimes", StringTools.buildList(mines[0].getResetWarnings(), "", ", ")));
        }
        if (mines[0].getSurface() != null) {
            sender.sendMessage(phrase("mineInfoSurface", mines[0].getSurface()));
        }
        if (mines[0].getFillMode()) {
            sender.sendMessage(phrase("mineInfoFillMode"));
        }
    }

    @Command(aliases = {"set", "add", "+"},
            description = "Set the percentage of a block in the mine",
            help = {"This command will always overwrite the current percentage for the specified block,",
                    "if a percentage has already been set. You cannot set the percentage of any specific",
                    "block, such that the percentage would then total over 100%."},
            usage = "<mine name> <block>:(data) <percentage>%",
            permissions = {"mineresetlite.mine.composition"},
            min = 3, max = -1, onlyPlayers = false)
    public void setComposition(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        //Match material
        String[] bits = args[args.length - 2].split(":");
        Material m = plugin.matchMaterial(bits[0]);
        if (m == null) {
            sender.sendMessage(phrase("unknownBlock"));
            return;
        }
        if (!m.isBlock()) {
            sender.sendMessage(phrase("notABlock"));
            return;
        }
        byte data = 0;
        if (bits.length == 2) {
            try {
                data = Byte.valueOf(bits[1]);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(phrase("unknownBlock"));
                return;
            }
        }
        //Parse percentage
        String percentageS = args[args.length - 1];
        if (!percentageS.endsWith("%")) {
            sender.sendMessage(phrase("badPercentage"));
            return;
        }
        StringBuilder psb = new StringBuilder(percentageS);
        psb.deleteCharAt(psb.length() - 1);
        double percentage = 0;
        try {
            percentage = Double.valueOf(psb.toString());
        } catch (NumberFormatException nfe) {
            sender.sendMessage(phrase("badPercentage"));
            return;
        }
        if (percentage > 100 || percentage <= 0) {
            sender.sendMessage(phrase("badPercentage"));
            return;
        }
        percentage = percentage / 100; //Make it a programmatic percentage
        SerializableBlock block = new SerializableBlock(m.getId(), data);
        Double oldPercentage = mines[0].getComposition().get(block);
        double total = 0;
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            if (!entry.getKey().equals(block)) {
                total += entry.getValue().doubleValue();
            } else {
                block = entry.getKey();
            }
        }
        total += percentage;
        if (total > 1) {
            sender.sendMessage(phrase("insaneCompositionChange"));
            if (oldPercentage == null) {
                mines[0].getComposition().remove(block);
            } else {
                mines[0].getComposition().put(block, oldPercentage);
            }
            return;
        }
        mines[0].getComposition().put(block, percentage);
        sender.sendMessage(phrase("mineCompositionSet", mines[0], percentage * 100, block));
        plugin.buffSave();
    }

    @Command(aliases = {"unset", "remove", "-"},
            description = "Remove a block from the composition of a mine",
            usage = "<mine name> <block>:(data)",
            permissions = {"mineresetlite.mine.composition"},
            min = 2, max = -1, onlyPlayers = false)
    public void unsetComposition(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        //Match material
        String[] bits = args[args.length - 1].split(":");
        Material m = plugin.matchMaterial(bits[0]);
        if (m == null) {
            sender.sendMessage(phrase("unknownBlock"));
            return;
        }
        if (!m.isBlock()) {
            sender.sendMessage(phrase("notABlock"));
            return;
        }
        byte data = 0;
        if (bits.length == 2) {
            try {
                data = Byte.valueOf(bits[1]);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(phrase("unknownBlock"));
                return;
            }
        }
        //Does the mine contain this block?
        SerializableBlock block = new SerializableBlock(m.getId(), data);
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            if (entry.getKey().equals(block)) {
                mines[0].getComposition().remove(entry.getKey());
                sender.sendMessage(phrase("blockRemovedFromMine", mines[0], block));
                return;
            }
        }
        sender.sendMessage(phrase("blockNotInMine", mines[0], block));
        plugin.buffSave();
    }

    @Command(aliases = {"reset", "r"},
            description = "Reset a mine",
            help = {"If you supply the -s argument, the mine will silently reset. Resets triggered via",
                    "this command will not show a 1 minute warning, unless this mine is flagged to always",
                    "have a warning. If the mine's composition doesn't equal 100%, the composition will be",
                    "padded with air until the total equals 100%."},
            usage = "<mine name> (-s)",
            permissions = {"mineresetlite.mine.reset"},
            min = 1, max = -1, onlyPlayers = false)
    public void resetMine(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        if (args[args.length - 1].equalsIgnoreCase("-s")) {
            //Silent reset
            mines[0].reset();
        } else {
            mines[0].reset();
            plugin.getServer().broadcastMessage(phrase("mineResetBroadcast", mines[0], sender));
        }
    }

    @Command(aliases = {"flag", "f"},
            description = "Set various properties of a mine, including automatic resets",
            help = {"Available flags:",
                    "resetDelay: An integer number of minutes specifying the time between automatic resets. Set to 0 to disable automatic resets.",
                    "resetWarnings: A comma separated list of integer minutes to warn before the automatic reset. Warnings must be less than the reset delay.",
                    "surface: A block that will cover the entire top surface of the mine when reset, obscuring surface ores. Set surface to air to clear the value.",
                    "fillMode: An alternate reset algorithm that will only \"reset\" air blocks inside your mine. Set to true or false."},
            usage = "<mine name> <setting> <value>",
            permissions = {"mineresetlite.mine.flag"},
            min = 3, max = -1, onlyPlayers = false)
    public void flag(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        String setting = args[args.length - 2];
        String value = args[args.length - 1];
        if (setting.equalsIgnoreCase("resetEvery") || setting.equalsIgnoreCase("resetDelay")) {
            int delay;
            try {
                delay = Integer.valueOf(value);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(phrase("badResetDelay"));
                return;
            }
            if (delay < 0) {
                sender.sendMessage(phrase("badResetDelay"));
                return;
            }
            mines[0].setResetDelay(delay);
            if (delay == 0) {
                sender.sendMessage(phrase("resetDelayCleared", mines[0]));
            } else {
                sender.sendMessage(phrase("resetDelaySet", mines[0], delay));
            }
            plugin.buffSave();
            return;
        } else if (setting.equalsIgnoreCase("resetWarnings") || setting.equalsIgnoreCase("resetWarning")) {
            String[] bits = value.split(",");
            List<Integer> warnings = mines[0].getResetWarnings();
            List<Integer> oldList = new LinkedList<Integer>(warnings);
            warnings.clear();
            for (String bit : bits) {
                try {
                    warnings.add(Integer.valueOf(bit));
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(phrase("badWarningList"));
                    return;
                }
            }
            //Validate warnings
            for (Integer warning : warnings) {
                if (warning >= mines[0].getResetDelay()) {
                    sender.sendMessage(phrase("badWarningList"));
                    mines[0].setResetWarnings(oldList);
                    return;
                }
            }
            if (warnings.contains(0) && warnings.size() == 1) {
                warnings.clear();
                sender.sendMessage(phrase("warningListCleared", mines[0]));
                return;
            } else if (warnings.contains(0)) {
                sender.sendMessage(phrase("badWarningList"));
                mines[0].setResetWarnings(oldList);
                return;
            }
            sender.sendMessage(phrase("warningListSet", mines[0]));
            plugin.buffSave();
            return;
        } else if (setting.equalsIgnoreCase("surface")) {
            //Match material
            String[] bits = value.split(":");
            Material m = plugin.matchMaterial(bits[0]);
            if (m == null) {
                sender.sendMessage(phrase("unknownBlock"));
                return;
            }
            if (!m.isBlock()) {
                sender.sendMessage(phrase("notABlock"));
                return;
            }
            byte data = 0;
            if (bits.length == 2) {
                try {
                    data = Byte.valueOf(bits[1]);
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(phrase("unknownBlock"));
                    return;
                }
            }
            if (m.equals(Material.AIR)) {
                mines[0].setSurface(null);
                sender.sendMessage(phrase("surfaceBlockCleared", mines[0]));
                plugin.buffSave();
                return;
            }
            SerializableBlock block = new SerializableBlock(m.getId(), data);
            mines[0].setSurface(block);
            sender.sendMessage(phrase("surfaceBlockSet", mines[0]));
            plugin.buffSave();
            return;
        } else if (setting.equalsIgnoreCase("fill") || setting.equalsIgnoreCase("fillMode")) {
            //Match true or false
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
                mines[0].setFillMode(true);
                sender.sendMessage(phrase("fillModeEnabled"));
                plugin.buffSave();
                return;
            } else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
                mines[0].setFillMode(false);
                sender.sendMessage(phrase("fillModeDisabled"));
                plugin.buffSave();
                return;
            }
            sender.sendMessage(phrase("invalidFillMode"));
        }
        sender.sendMessage(phrase("unknownFlag"));
    }
    @Command(aliases = {"erase"},
            description = "Completely erase a mine",
            help = {"Like most erasures of data, be sure you don't need to recover anything from this mine before you delete it."},
            usage = "<mine name>",
            permissions = {"mineresetlite.mine.erase"},
            min = 1, max = -1, onlyPlayers = false)
    public void erase(CommandSender sender, String[] args) {
        Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        plugin.mines.remove(mines[0]);
        plugin.buffSave();
        sender.sendMessage(phrase("mineErased", mines[0]));
    }
    @Command(aliases = {"reschedule"},
            description = "Synchronize all automatic mine resets",
            help = {"This command will set the 'start time' of the mine resets to the same point."},
            usage = "",
            permissions = {"mineresetlite.mine.flag"},
            min = 0, max = 0, onlyPlayers = false)
    public void reschedule(CommandSender sender, String[] args) {
        for (Mine mine : plugin.mines) {
            mine.setResetDelay(mine.getResetDelay());
        }
        plugin.buffSave();
        sender.sendMessage(phrase("rescheduled"));
    }
}

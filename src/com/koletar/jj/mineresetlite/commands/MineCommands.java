package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.Command;
import com.koletar.jj.mineresetlite.Mine;
import com.koletar.jj.mineresetlite.MineResetLite;
import com.koletar.jj.mineresetlite.SerializableBlock;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
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
            min = 0, max = 0, onlyPlayers = false)
    public void listMines(CommandSender sender, String[] args) {
        StringBuilder response = new StringBuilder();
        for (Mine mine : plugin.mines) {
            response.append("&e");
            response.append(mine.getName());
            response.append("&d, ");
        }
        if (response.length() != 0) {
            response.delete(response.length() - 2, response.length());
        }
        sender.sendMessage(phrase("mineList", response));
    }

    @Command(aliases = {"pos1", "p1"},
            description = "Change your first selection point",
            usage = "(feet)",
            permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
            min = 0, max = 1, onlyPlayers = true)
    public void setPoint1(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            //Use block being looked at
            point1.put(player, player.getEyeLocation());
            player.sendMessage(phrase("firstPointSet"));
            return;
        } else if (args[0].equalsIgnoreCase("feet")) {
            //Use block being stood on
            point1.put(player, player.getLocation());
            player.sendMessage(phrase("firstPointSet"));
            return;
        }
    }

    @Command(aliases = {"pos2", "p2"},
            description = "Change your first selection point",
            usage = "(feet)",
            permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
            min = 0, max = 1, onlyPlayers = true)
    public void setPoint2(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            //Use block being looked at
            point2.put(player, player.getEyeLocation());
            player.sendMessage(phrase("secondPointSet"));
            return;
        } else if (args[0].equalsIgnoreCase("feet")) {
            //Use block being stood on
            point2.put(player, player.getLocation());
            player.sendMessage(phrase("secondPointSet"));
            return;
        }
    }

    @Command(aliases = {"create", "save"},
            description = "Create a mine from either your WorldEdit selection or by manually specifying the points",
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
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            sb.append(s);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        String name = sb.toString();
        //Verify uniqueness of mine name
        for (Mine mine : plugin.mines) {
            if (mine.getName().equalsIgnoreCase(name)) {
                player.sendMessage(phrase("nameInUse", name));
                return;
            }
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
    }

    @Command(aliases = {"info", "i"},
            description = "List information about a mine",
            usage = "<mine name>",
            permissions = {"mineresetlite.mine.info"},
            min = 1, max = -1, onlyPlayers = false)
    public void mineInfo(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        Mine[] mines = plugin.matchMines(sb.toString());
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
    }

    @Command(aliases = {"set", "add", "+"},
            description = "Set the percentage of a block in the mine",
            usage = "<mine name> <block>:(data) <percentage>%",
            permissions = {"mineresetlite.mine.composition"},
            min = 3, max = -1, onlyPlayers = false)
    public void setComposition(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length - 2; i++) {
            sb.append(args[i]);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        Mine[] mines = plugin.matchMines(sb.toString());
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        //Match material
        String[] bits = args[args.length - 2].split(":");
        Material m = Material.matchMaterial(bits[0]);
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
        mines[0].getComposition().put(block, percentage);
        double total = 0;
        for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
            total += entry.getValue().doubleValue();
        }
        if (total > 1) {
            sender.sendMessage(phrase("insaneCompositionChange"));
            if (oldPercentage == null) {
                mines[0].getComposition().remove(block);
            } else {
                mines[0].getComposition().put(block, oldPercentage);
            }
            return;
        }
        sender.sendMessage(phrase("mineCompositionSet", mines[0], percentage * 100, block));
    }

    @Command(aliases = {"unset", "remove", "-"},
            description = "Remove a block from the composition of a mine",
            usage = "<mine name> <block>:(data)",
            permissions = {"mineresetlite.mine.composition"},
            min = 2, max = -1, onlyPlayers = false)
    public void unsetComposition(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length - 1; i++) {
            sb.append(args[i]);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        Mine[] mines = plugin.matchMines(sb.toString());
        if (mines.length > 1) {
            sender.sendMessage(phrase("tooManyMines"));
            return;
        } else if (mines.length == 0) {
            sender.sendMessage(phrase("noMinesMatched"));
            return;
        }
        //Match material
        String[] bits = args[args.length - 1].split(":");
        Material m = Material.matchMaterial(bits[0]);
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
    }

    @Command(aliases = {"reset", "r"},
            description = "Reset a mine",
            usage = "<mine name> (-s)",
            permissions = {"mineresetlite.mine.reset"},
            min = 1, max = -1, onlyPlayers = false)
    public void resetMine(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].equalsIgnoreCase("-s")) {
                sb.append(args[i]);
                sb.append(" ");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        Mine[] mines = plugin.matchMines(sb.toString());
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
}

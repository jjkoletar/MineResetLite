package com.koletar.jj.mineresetlite;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Specific command manager. Far less genericizied than sk89q's.
 * </p>
 * MRL's command system is very much based on sk89q's command system for WorldEdit.
 * @author jjkoletar
 */
public class CommandManager {
    private Map<String, Method> commands;
    private Map<Method, Object> instances;

    public CommandManager() {
        commands = new HashMap<String, Method>();
        instances = new HashMap<Method, Object>();
    }

    public void register(Class<?> cls, Object obj) {
        for (Method method : cls.getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) {
                continue;
            }

            Command command = method.getAnnotation(Command.class);

            for (String alias : command.aliases()) {
                commands.put(alias, method);
            }
            instances.put(method, obj);
        }
    }

    @Command(aliases = {"help", "?"},
            description = "Provide information about MineResetLite commands",
            min = 0, max = -1)
    public void help(CommandSender sender, String[] args) {
        List<Method> seenMethods = new LinkedList<Method>();
        for (Map.Entry<String, Method> entry : commands.entrySet()) {
            if (!seenMethods.contains(entry.getValue())) {
                seenMethods.add(entry.getValue());
                Command command = entry.getValue().getAnnotation(Command.class);
                //Only show help if the sender can use the command anyway
               // if ((sender instanceof Player && command.onlyPlayers() && ) )
            }
        }
    }

    public void callCommand(String cmdName, CommandSender sender, String[] args) {
        //Do we have the command?
        Method method = commands.get(cmdName.toLowerCase());
        if (method == null) {
            sender.sendMessage(ChatColor.DARK_RED + "Unknown command.");
            return;
        }
        //Get annotation
        Command command = method.getAnnotation(Command.class);

        //Validate arguments
        if (!(command.min() <= args.length && (command.max() == -1 || command.max() >= args.length))) {
            sender.sendMessage(ChatColor.DARK_RED + "Invalid arguments.");
            sender.sendMessage(ChatColor.DARK_RED + "/mrl " + command.aliases()[0] + " " + command.usage());
            return;
        }

        //Player or console?
        if (command.onlyPlayers() && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED + "You must be a player to use this command.");
            return;
        }

        //Permission checks
        boolean may = false;
        if (command.permissions().length == 0) {
            may = true;
        }
        for (String perm : command.permissions()) {
            if (sender.hasPermission(perm)) {
                may = true;
            }
        }
        if (!may) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
        }

        //Run command
        Object[] methodArgs = {sender, args};
        try {
            method.invoke(instances.get(method), methodArgs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid methods on command!");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid methods on command!");
        }
    }
}

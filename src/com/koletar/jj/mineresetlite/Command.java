package com.koletar.jj.mineresetlite;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Command annotation to label commands.
 * </p>
 * MRL's command system is very much based on sk89q's command system for WorldEdit.
 * @author jjkoletar
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * Aliases for the command. The first alias indicates the command's name.
     * @return Aliases
     */
    String[] aliases();

    /**
     * Usage string for arguments passed to the command.
     * @return
     */
    String usage() default "";

    /**
     * @return Short description of the command's function.
     */
    String description();

    /**
     * @return Multilined help text describing the command in full
     */
    String[] help() default {};

    /**
     * Permissions necessary for the execution of the command.
     * An empty array (the default) means the command requires no permissions.
     * @return Permissions array
     */
    String[] permissions() default {};

    /**
     * @return Minimum number of arguments
     */
    int min() default 0;

    /**
     * @return Max number of arguments. -1 for unlimited
     */
    int max() default -1;

    /**
     * @return Does the command require a player to execute it or not?
     */
    boolean onlyPlayers() default false;
}

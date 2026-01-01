package org.leavesmc.leaves.command;

import net.minecraft.Util;
import org.leavesmc.leaves.command.subcommands.ConfigCommand;
import org.leavesmc.leaves.command.subcommands.ReloadCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class LeavesCommand extends LeavesRootCommand {

    public static final String BASE_PERM = "bukkit.command.leaves.";

    // subcommand label -> subcommand
    private static final Map<String, LeavesSubcommand> SUBCOMMANDS = Util.make(() -> {
        final Map<Set<String>, LeavesSubcommand> commands = new HashMap<>();
        commands.put(Set.of("config"), new ConfigCommand());
        commands.put(Set.of("reload"), new ReloadCommand());

        return commands.entrySet().stream()
            .flatMap(entry -> entry.getKey().stream().map(s -> Map.entry(s, entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    public LeavesCommand() {
        super("leaves", "Leaves related commands", "bukkit.command.leaves", SUBCOMMANDS);
    }
}

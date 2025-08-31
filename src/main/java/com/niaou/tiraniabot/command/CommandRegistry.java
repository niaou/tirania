package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.module.BotModule;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CommandRegistry {

  private static CommandRegistry instance;
  private final Map<String, Map<String, Command>> commands = new HashMap<>();

  public CommandRegistry(List<Command> commandList) {
    for (Command cmd : commandList) {
      for (BotModule module : cmd.getModules()) {
        Map<String, Command> moduleCommands =
            commands.computeIfAbsent(module.getValue(), _ -> new HashMap<>());

        moduleCommands.put(cmd.getName().toLowerCase(), cmd);

        for (String alias : cmd.getAliases()) {
          moduleCommands.put(alias.toLowerCase(), cmd);
        }
      }
    }
    instance = this;
  }

  public Command getCommand(BotModule module, String name) {
    Map<String, Command> moduleCommands = commands.get(module.getValue());
    return moduleCommands != null ? moduleCommands.get(name.toLowerCase()) : null;
  }

  public Collection<Command> getAllModuleCommands(BotModule module) {
    Map<String, Command> moduleCommands = commands.get(module.getValue());
    return moduleCommands == null ? List.of() : new HashSet<>(moduleCommands.values());
  }

  public Collection<Command> getAllCommands() {
    return commands.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());
  }

  public static CommandRegistry getInstance() {
    if (instance == null) {
      throw new IllegalStateException("CommandRegistry is not initialized yet!");
    }
    return instance;
  }
}

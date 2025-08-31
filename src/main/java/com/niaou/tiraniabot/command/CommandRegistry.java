package com.niaou.tiraniabot.command;

import com.niaou.tiraniabot.context.Context;
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
      for (Context context : cmd.getContexts()) {
        Map<String, Command> contextCommands =
            commands.computeIfAbsent(context.getValue(), _ -> new HashMap<>());

        contextCommands.put(cmd.getName().toLowerCase(), cmd);

        for (String alias : cmd.getAliases()) {
          contextCommands.put(alias.toLowerCase(), cmd);
        }
      }
    }
    instance = this;
  }

  public Command getCommand(Context context, String name) {
    Map<String, Command> contextCommands = commands.get(context.getValue());
    return contextCommands != null ? contextCommands.get(name.toLowerCase()) : null;
  }

  public Collection<Command> getAllContextCommands(Context context) {
    Map<String, Command> contextCommands = commands.get(context.getValue());
    return contextCommands == null ? List.of() : new HashSet<>(contextCommands.values());
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

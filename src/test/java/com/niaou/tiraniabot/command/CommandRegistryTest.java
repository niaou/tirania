package com.niaou.tiraniabot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.niaou.tiraniabot.context.Context;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CommandRegistryTest {

  @Mock private Command command;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    when(command.getName()).thenReturn("!ping");
    when(command.getAliases()).thenReturn(List.of("!p"));
    when(command.getContexts()).thenReturn(List.of(Context.GLOBAL));
    when(command.getDescription()).thenReturn("Ping test");
  }

  @Test
  void registersAndRetrievesCommand() {
    CommandRegistry registry = new CommandRegistry(List.of(command));

    Command found = registry.getCommand(Context.GLOBAL, "!ping");
    Command alias = registry.getCommand(Context.GLOBAL, "!p");

    assertThat(found).isSameAs(command);
    assertThat(alias).isSameAs(command);
  }

  @Test
  void getAllContextCommands_returnsCommandsForContext() {
    CommandRegistry registry = new CommandRegistry(List.of(command));

    Collection<Command> cmds = registry.getAllContextCommands(Context.GLOBAL);
    assertThat(cmds).containsExactly(command);
  }

  @Test
  void getAllCommands_returnsAllCommands() {
    CommandRegistry registry = new CommandRegistry(List.of(command));
    assertThat(registry.getAllCommands()).contains(command);
  }

  @Test
  void getInstance_returnsSingleton() {
    CommandRegistry registry = new CommandRegistry(List.of(command));
    assertThat(CommandRegistry.getInstance()).isSameAs(registry);
  }
}

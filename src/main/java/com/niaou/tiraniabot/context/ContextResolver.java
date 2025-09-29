package com.niaou.tiraniabot.context;

import static com.niaou.tiraniabot.context.Context.GLOBAL;
import static java.util.Arrays.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class ContextResolver {

  private final Map<String, Context> channelToContext =
      stream(Context.values()).collect(Collectors.toMap(Context::getValue, Function.identity()));

  public Context resolveContext(MessageChannel channel) {
    return channelToContext.getOrDefault(channel.getName().toLowerCase(), GLOBAL);
  }

  public Context resolveContext(MessageChannel channel, String args) {
    if (args != null && args.equalsIgnoreCase("all")) {
      return Context.GLOBAL;
    }
    return resolveContext(channel);
  }
}

package com.niaou.tiraniabot.context;

import static com.niaou.tiraniabot.context.Context.ADMIN;
import static com.niaou.tiraniabot.context.Context.GLOBAL;
import static com.niaou.tiraniabot.context.Context.MUSIC;

import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class ContextResolver {

  private final Map<String, Context> channelToContext = new HashMap<>();

  public ContextResolver() {
    channelToContext.put("music", MUSIC);
    channelToContext.put("admin", ADMIN);
    channelToContext.put("general", GLOBAL);
  }

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

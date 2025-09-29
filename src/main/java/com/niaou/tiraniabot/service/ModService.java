package com.niaou.tiraniabot.service;

import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.stereotype.Service;

@Service
public class ModService {

  private static final List<String> MOD_ROLE_NAMES =
      List.of("mod", "mods", "moderator", "moderators");

  public boolean isModerator(Member member) {
    if (member == null) return false;

    boolean hasModPermission =
        member.hasPermission(Permission.KICK_MEMBERS)
            || member.hasPermission(Permission.BAN_MEMBERS)
            || member.hasPermission(Permission.MANAGE_SERVER);

    boolean hasModRole =
        member.getRoles().stream()
            .anyMatch(
                r -> MOD_ROLE_NAMES.stream().anyMatch(name -> name.equalsIgnoreCase(r.getName())));

    return hasModPermission || hasModRole;
  }
}

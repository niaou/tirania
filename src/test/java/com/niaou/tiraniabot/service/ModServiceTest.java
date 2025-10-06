package com.niaou.tiraniabot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModServiceTest {
  private ModService modService;

  @BeforeEach
  void setup() {
    modService = new ModService();
  }

  @Test
  void isModerator_nullMember_returnsFalse() {
    assertThat(modService.isModerator(null)).isFalse();
  }

  @Test
  void isModerator_hasKickPermission_returnsTrue() {
    Member member = mock(Member.class);
    when(member.hasPermission(Permission.KICK_MEMBERS)).thenReturn(true);
    when(member.getRoles()).thenReturn(List.of());

    assertThat(modService.isModerator(member)).isTrue();
  }

  @Test
  void isModerator_hasBanPermission_returnsTrue() {
    Member member = mock(Member.class);
    when(member.hasPermission(Permission.BAN_MEMBERS)).thenReturn(true);
    when(member.getRoles()).thenReturn(List.of());

    assertThat(modService.isModerator(member)).isTrue();
  }

  @Test
  void isModerator_hasManageServerPermission_returnsTrue() {
    Member member = mock(Member.class);
    when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true);
    when(member.getRoles()).thenReturn(List.of());

    assertThat(modService.isModerator(member)).isTrue();
  }

  @Test
  void isModerator_hasModRole_returnsTrue() {
    Member member = mock(Member.class);
    Role modRole = mock(Role.class);
    when(modRole.getName()).thenReturn("mod");
    when(member.getRoles()).thenReturn(List.of(modRole));

    assertThat(modService.isModerator(member)).isTrue();
  }

  @Test
  void isModerator_hasModeratorRoleDifferentCase_returnsTrue() {
    Member member = mock(Member.class);
    Role modRole = mock(Role.class);
    when(modRole.getName()).thenReturn("Moderators");
    when(member.getRoles()).thenReturn(List.of(modRole));

    assertThat(modService.isModerator(member)).isTrue();
  }

  @Test
  void isModerator_hasNoPermissionOrRole_returnsFalse() {
    Member member = mock(Member.class);
    when(member.hasPermission(any(Permission.class))).thenReturn(false);
    when(member.getRoles()).thenReturn(List.of());

    assertThat(modService.isModerator(member)).isFalse();
  }

  @Test
  void isModerator_hasIrrelevantRole_returnsFalse() {
    Member member = mock(Member.class);
    Role role = mock(Role.class);
    when(role.getName()).thenReturn("user");
    when(member.getRoles()).thenReturn(List.of(role));
    when(member.hasPermission(any(Permission.class))).thenReturn(false);

    assertThat(modService.isModerator(member)).isFalse();
  }

  @Test
  void isModerator_hasBothPermissionAndRole_returnsTrue() {
    Member member = mock(Member.class);
    Role role = mock(Role.class);
    when(role.getName()).thenReturn("moderator");
    when(member.getRoles()).thenReturn(List.of(role));
    when(member.hasPermission(Permission.KICK_MEMBERS)).thenReturn(true);

    assertThat(modService.isModerator(member)).isTrue();
  }
}

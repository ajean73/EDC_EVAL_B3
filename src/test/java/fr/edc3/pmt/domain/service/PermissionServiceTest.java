package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.model.TeamMember;
import fr.edc3.pmt.domain.repository.TeamMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void requireRole_shouldPass_whenRoleIsAllowed() {
        when(teamMemberRepository.findByWorkspaceIdAndAccountId(1L, 2L))
                .thenReturn(Optional.of(TeamMember.builder().workspaceId(1L).accountId(2L).role(MemberRole.ADMIN).build()));

        assertDoesNotThrow(() -> permissionService.requireRole(1L, 2L, MemberRole.ADMIN, MemberRole.MEMBER));
    }

    @Test
    void requireRole_shouldFail_whenRoleIsNotAllowed() {
        when(teamMemberRepository.findByWorkspaceIdAndAccountId(1L, 2L))
                .thenReturn(Optional.of(TeamMember.builder().workspaceId(1L).accountId(2L).role(MemberRole.OBSERVER).build()));

        ApiException ex = assertThrows(ApiException.class,
                () -> permissionService.requireRole(1L, 2L, MemberRole.ADMIN, MemberRole.MEMBER));

        assertEquals("Permission denied", ex.getMessage());
    }

    @Test
    void requireRole_shouldFail_whenAccountIsNotMember() {
        when(teamMemberRepository.findByWorkspaceIdAndAccountId(1L, 99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> permissionService.requireRole(1L, 99L, MemberRole.ADMIN));

        assertEquals("Account is not a member of workspace", ex.getMessage());
    }
}

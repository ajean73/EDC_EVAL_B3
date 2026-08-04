import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  const createComponent = () => {
    const api = {
      getWorkspaces: jest.fn(),
      createWorkspace: jest.fn(),
      getWorkItems: jest.fn(),
      getWorkspaceMembers: jest.fn(),
      inviteToWorkspace: jest.fn(),
      updateMemberRole: jest.fn(),
      getNotifications: jest.fn(),
      getMyInvitations: jest.fn(),
      acceptInvitation: jest.fn(),
      declineInvitation: jest.fn(),
      createWorkItem: jest.fn(),
      updateWorkItem: jest.fn(),
      getWorkItem: jest.fn(),
      getWorkItemHistory: jest.fn()
    };

    const authSession = {
      getUser: jest.fn(),
      clear: jest.fn()
    };

    const router = {
      navigate: jest.fn().mockResolvedValue(true)
    };

    const component = new HomeComponent(api as never, authSession as never, router as never);
    return { component, api, authSession, router };
  };

  const unauthorizedError = () =>
    new HttpErrorResponse({
      status: 400,
      error: { message: 'Permission denied' }
    });

  const workspaceMemberError = () =>
    new HttpErrorResponse({
      status: 400,
      error: { message: 'Account is not a member of workspace' }
    });

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('ngOnInit redirects to auth when session is missing', () => {
    const { component, authSession, router } = createComponent();
    authSession.getUser.mockReturnValue(null);

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/auth']);
  });

  it('ngOnInit initializes actor and refreshes data when session exists', () => {
    const { component, authSession } = createComponent();
    authSession.getUser.mockReturnValue({ accountId: 7, username: 'alice', email: 'alice@pmt.local' });

    const refreshWorkspacesSpy = jest.spyOn(component, 'refreshWorkspaces').mockImplementation(() => {});
    const refreshNotificationsSpy = jest.spyOn(component, 'refreshNotifications').mockImplementation(() => {});
    const refreshInvitationsSpy = jest.spyOn(component, 'refreshMyInvitations').mockImplementation(() => {});

    component.ngOnInit();

    expect(component.actorAccountId).toBe(7);
    expect(component.currentUsername).toBe('alice');
    expect(refreshWorkspacesSpy).toHaveBeenCalled();
    expect(refreshNotificationsSpy).toHaveBeenCalled();
    expect(refreshInvitationsSpy).toHaveBeenCalled();
  });

  it('refreshWorkspaces loads workspaces and auto-selects first one', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 5;
    const selectWorkspaceSpy = jest.spyOn(component, 'selectWorkspace').mockImplementation(() => {});
    api.getWorkspaces.mockReturnValue(of([
      { id: 11, name: 'A', description: 'd', startDate: '2026-08-01', ownerAccountId: 5, createdAt: '2026-08-01T10:00:00' }
    ]));

    component.refreshWorkspaces();

    expect(component.workspaces).toHaveLength(1);
    expect(selectWorkspaceSpy).toHaveBeenCalledWith(11);
  });

  it('refreshWorkspaces shows error and logs out when actor is missing', () => {
    const { component } = createComponent();
    const logoutSpy = jest.spyOn(component, 'logout').mockImplementation(() => {});

    component.refreshWorkspaces();

    expect(component.errorMessage).toContain('Session invalide');
    jest.advanceTimersByTime(900);
    expect(logoutSpy).toHaveBeenCalled();
  });

  it('selectWorkspace loads work items successfully', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 5;
    const refreshMembersSpy = jest.spyOn(component, 'refreshMembers').mockImplementation(() => {});
    api.getWorkItems.mockReturnValue(of([
      {
        id: 1,
        workspaceId: 3,
        title: 'Task',
        description: 'Desc',
        dueDate: '2026-08-12',
        priority: 'HIGH',
        status: 'TODO',
        creatorAccountId: 5,
        assignedAccountId: null,
        completedAt: null,
        createdAt: '2026-08-10T10:00:00',
        updatedAt: '2026-08-10T10:00:00'
      }
    ]));

    component.selectWorkspace(3);

    expect(component.selectedWorkspaceId).toBe(3);
    expect(refreshMembersSpy).toHaveBeenCalled();
    expect(component.workItems).toHaveLength(1);
    expect(component.loading).toBe(false);
  });

  it('selectWorkspace shows member error message on forbidden workspace access', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 5;
    jest.spyOn(component, 'refreshMembers').mockImplementation(() => {});
    api.getWorkItems.mockReturnValue(throwError(() => workspaceMemberError()));

    component.selectWorkspace(3);

    expect(component.errorMessage).toContain('Tu n\'es pas membre');
    expect(component.loading).toBe(false);
  });

  it('inviteMember blocks when current user is not admin', () => {
    const { component } = createComponent();
    component.selectedWorkspaceId = 1;
    component.actorAccountId = 5;
    component.currentWorkspaceRole = 'MEMBER';

    component.inviteMember();

    expect(component.errorMessage).toContain('Tu dois être administrateur');
  });

  it('updateRole surfaces permission error from API', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 1;
    component.actorAccountId = 5;
    component.currentWorkspaceRole = 'ADMIN';
    api.updateMemberRole.mockReturnValue(throwError(() => unauthorizedError()));

    component.updateRole({
      id: 1,
      workspaceId: 1,
      accountId: 9,
      username: 'bob',
      role: 'MEMBER',
      joinedAt: '2026-08-01T10:00:00'
    }, 'OBSERVER');

    expect(component.errorMessage).toContain('Seul un administrateur peut modifier les rôles.');
  });

  it('getAssigneeDisplay returns username when member exists', () => {
    const { component } = createComponent();
    component.members = [{
      id: 1,
      workspaceId: 1,
      accountId: 9,
      username: 'bob',
      role: 'MEMBER',
      joinedAt: '2026-08-01T10:00:00'
    }];

    expect(component.getAssigneeDisplay(9)).toBe('bob');
    expect(component.getAssigneeDisplay(null)).toBe('Non assigné');
    expect(component.getAssigneeDisplay(10)).toBe('Compte #10');
  });

  it('dropWorkItem reverts optimistic update when API call fails', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 1;
    component.actorAccountId = 5;

    const item = {
      id: 55,
      workspaceId: 1,
      title: 'Task',
      description: 'Desc',
      dueDate: '2026-08-12',
      priority: 'HIGH' as const,
      status: 'TODO' as const,
      creatorAccountId: 5,
      assignedAccountId: null,
      completedAt: null,
      createdAt: '2026-08-10T10:00:00',
      updatedAt: '2026-08-10T10:00:00'
    };

    api.updateWorkItem.mockReturnValue(throwError(() => unauthorizedError()));

    component.dropWorkItem({
      previousContainer: { data: 'TODO' },
      container: { data: 'DONE' },
      item: { data: item }
    } as never);

    expect(item.status).toBe('TODO');
    expect(component.errorMessage).toContain('Tu dois être admin ou membre');
  });

  it('logout clears session and redirects to auth page', () => {
    const { component, authSession, router } = createComponent();

    component.logout();

    expect(authSession.clear).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth']);
  });

  it('createWorkspace success resets form and refreshes selection', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 3;
    component.createWorkspaceForm = { name: 'Projet', description: 'Desc', startDate: '2026-08-01' };
    api.createWorkspace.mockReturnValue(of({ id: 66 }));
    const refreshSpy = jest.spyOn(component, 'refreshWorkspaces').mockImplementation(() => {});
    const selectSpy = jest.spyOn(component, 'selectWorkspace').mockImplementation(() => {});

    component.createWorkspace();

    expect(api.createWorkspace).toHaveBeenCalledWith({
      name: 'Projet',
      description: 'Desc',
      startDate: '2026-08-01',
      ownerAccountId: 3
    });
    expect(component.createWorkspaceForm).toEqual({ name: '', description: '', startDate: '' });
    expect(refreshSpy).toHaveBeenCalled();
    expect(selectSpy).toHaveBeenCalledWith(66);
  });

  it('createWorkspace triggers logout timer on owner/account missing backend message', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 3;
    const logoutSpy = jest.spyOn(component, 'logout').mockImplementation(() => {});
    api.createWorkspace.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Owner account not found' } })));

    component.createWorkspace();

    expect(component.errorMessage).toContain('session obsolète');
    jest.advanceTimersByTime(900);
    expect(logoutSpy).toHaveBeenCalled();
  });

  it('createWorkspace shows generic error when backend message is unknown', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 3;
    api.createWorkspace.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Other' } })));

    component.createWorkspace();

    expect(component.errorMessage).toBe('Création du projet impossible.');
  });

  it('createWorkItem returns early when no workspace is selected', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = null;

    component.createWorkItem();

    expect(api.createWorkItem).not.toHaveBeenCalled();
  });

  it('createWorkItem resets form and reloads workspace on success', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 4;
    component.actorAccountId = 7;
    component.createWorkItemForm = {
      title: 'T',
      description: 'D',
      dueDate: '2026-08-10',
      priority: 'HIGH',
      assignedAccountId: 9
    };
    api.createWorkItem.mockReturnValue(of({ id: 1 }));
    const selectSpy = jest.spyOn(component, 'selectWorkspace').mockImplementation(() => {});

    component.createWorkItem();

    expect(api.createWorkItem).toHaveBeenCalled();
    expect(component.createWorkItemForm).toEqual({
      title: '',
      description: '',
      dueDate: '',
      priority: 'MEDIUM',
      assignedAccountId: null
    });
    expect(selectSpy).toHaveBeenCalledWith(4);
  });

  it('createWorkItem shows error on failure', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 4;
    component.actorAccountId = 7;
    api.createWorkItem.mockReturnValue(throwError(() => new Error('x')));

    component.createWorkItem();

    expect(component.errorMessage).toBe('Création de la tâche impossible.');
  });

  it('refreshMembers maps current role and handles error', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 9;
    component.actorAccountId = 2;
    api.getWorkspaceMembers.mockReturnValue(of([
      { id: 1, workspaceId: 9, accountId: 2, username: 'me', role: 'ADMIN', joinedAt: 'x' },
      { id: 2, workspaceId: 9, accountId: 3, username: 'you', role: 'MEMBER', joinedAt: 'x' }
    ]));

    component.refreshMembers();
    expect(component.currentWorkspaceRole).toBe('ADMIN');

    api.getWorkspaceMembers.mockReturnValue(throwError(() => new Error('x')));
    component.refreshMembers();
    expect(component.members).toEqual([]);
    expect(component.currentWorkspaceRole).toBeNull();
  });

  it('inviteMember success updates latest invitation and resets form', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 3;
    component.actorAccountId = 7;
    component.currentWorkspaceRole = 'ADMIN';
    component.inviteForm = { inviteeEmail: 'u@pmt.local', role: 'OBSERVER' };
    api.inviteToWorkspace.mockReturnValue(of({ id: 1, workspaceId: 3, inviteeEmail: 'u@pmt.local', role: 'OBSERVER', state: 'PENDING', invitedBy: 7, invitedByUsername: 'me', createdAt: 'x', respondedAt: null }));

    component.inviteMember();

    expect(component.latestInvitation?.id).toBe(1);
    expect(component.inviteForm).toEqual({ inviteeEmail: '', role: 'MEMBER' });
  });

  it('inviteMember shows generic error when API fails with non-permission error', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 3;
    component.actorAccountId = 7;
    component.currentWorkspaceRole = 'ADMIN';
    api.inviteToWorkspace.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Boom' } })));

    component.inviteMember();

    expect(component.errorMessage).toBe('Invitation impossible.');
  });

  it('updateRole updates member role on success and handles generic error', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 1;
    component.actorAccountId = 5;
    component.currentWorkspaceRole = 'ADMIN';
    const member = { id: 1, workspaceId: 1, accountId: 9, username: 'bob', role: 'MEMBER' as const, joinedAt: 'x' };

    api.updateMemberRole.mockReturnValue(of({ ...member, role: 'OBSERVER' }));
    component.updateRole(member, 'OBSERVER');
    expect(member.role).toBe('OBSERVER');

    api.updateMemberRole.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Nope' } })));
    component.updateRole(member, 'MEMBER');
    expect(component.errorMessage).toBe('Mise à jour du rôle impossible.');
  });

  it('refreshNotifications handles no actor and backend error', () => {
    const { component, api } = createComponent();
    const logoutSpy = jest.spyOn(component, 'logout').mockImplementation(() => {});

    component.refreshNotifications();
    jest.advanceTimersByTime(900);
    expect(logoutSpy).toHaveBeenCalled();

    component.actorAccountId = 8;
    api.getNotifications.mockReturnValue(throwError(() => new Error('x')));
    component.refreshNotifications();
    expect(component.errorMessage).toBe('Chargement des notifications impossible.');
  });

  it('acceptInvitation handles success and all error branches', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 5;
    const refreshWSpy = jest.spyOn(component, 'refreshWorkspaces').mockImplementation(() => {});
    const refreshISpy = jest.spyOn(component, 'refreshMyInvitations').mockImplementation(() => {});

    api.acceptInvitation.mockReturnValue(of({ id: 1 }));
    component.acceptInvitation(1);
    expect(refreshWSpy).toHaveBeenCalled();
    expect(refreshISpy).toHaveBeenCalled();

    api.acceptInvitation.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Invitation does not belong to this account' } })));
    component.acceptInvitation(1);
    expect(component.errorMessage).toContain('ne t\'appartient pas');

    api.acceptInvitation.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Invitation already processed' } })));
    component.acceptInvitation(1);
    expect(component.errorMessage).toContain('déjà été traitée');

    api.acceptInvitation.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Other' } })));
    component.acceptInvitation(1);
    expect(component.errorMessage).toContain('Acceptation de l\'invitation impossible.');
  });

  it('declineInvitation handles success and all error branches', () => {
    const { component, api } = createComponent();
    component.actorAccountId = 5;
    const refreshISpy = jest.spyOn(component, 'refreshMyInvitations').mockImplementation(() => {});

    api.declineInvitation.mockReturnValue(of({ id: 1 }));
    component.declineInvitation(1);
    expect(refreshISpy).toHaveBeenCalled();

    api.declineInvitation.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Invitation does not belong to this account' } })));
    component.declineInvitation(1);
    expect(component.errorMessage).toContain('ne t\'appartient pas');

    api.declineInvitation.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Invitation already processed' } })));
    component.declineInvitation(1);
    expect(component.errorMessage).toContain('déjà été traitée');

    api.declineInvitation.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Other' } })));
    component.declineInvitation(1);
    expect(component.errorMessage).toContain('Refus de l\'invitation impossible.');
  });

  it('utility methods return expected values', () => {
    const { component } = createComponent();
    component.workItems = [
      { id: 1, workspaceId: 1, title: 'A', description: 'D', dueDate: '2026-01-01', priority: 'LOW', status: 'TODO', creatorAccountId: 1, assignedAccountId: null, completedAt: null, createdAt: 'x', updatedAt: 'x' },
      { id: 2, workspaceId: 1, title: 'B', description: 'D', dueDate: '2026-01-01', priority: 'LOW', status: 'DONE', creatorAccountId: 1, assignedAccountId: null, completedAt: null, createdAt: 'x', updatedAt: 'x' }
    ];

    expect(component.getItemsByStatus('DONE')).toHaveLength(1);
    expect(component.getStatusLabel('TODO')).toBe('A FAIRE');
    expect(component.getStatusLabel('IN_PROGRESS')).toBe('EN COURS');
    expect(component.getStatusLabel('IN_REVIEW')).toBe('A VALIDER');
    expect(component.getStatusLabel('DONE')).toBe('TERMINE');

    component.showSettingsSection();
    expect(component.activeLeftSection).toBe('settings');
    component.showBoardSection();
    expect(component.activeLeftSection).toBe('board');
    expect(component.getDropListId('IN_PROGRESS')).toBe('kanban-in_progress');
  });

  it('dropWorkItem handles guard cases and success flow', () => {
    const { component, api } = createComponent();
    const item = {
      id: 5,
      workspaceId: 1,
      title: 'Task',
      description: 'Desc',
      dueDate: '2026-08-12',
      priority: 'HIGH' as const,
      status: 'TODO' as const,
      creatorAccountId: 5,
      assignedAccountId: null,
      completedAt: null,
      createdAt: '2026-08-10T10:00:00',
      updatedAt: '2026-08-10T10:00:00'
    };

    component.dropWorkItem({ previousContainer: { data: 'TODO' }, container: { data: 'DONE' }, item: { data: item } } as never);
    expect(api.updateWorkItem).not.toHaveBeenCalled();

    component.selectedWorkspaceId = 1;
    component.actorAccountId = 5;
    component.dropWorkItem({ previousContainer: { data: 'TODO' }, container: { data: 'TODO' }, item: { data: item } } as never);
    expect(api.updateWorkItem).not.toHaveBeenCalled();

    api.updateWorkItem.mockReturnValue(of({ ...item, status: 'DONE', completedAt: '2026-08-10T11:00:00', updatedAt: '2026-08-10T11:00:00' }));
    component.dropWorkItem({ previousContainer: { data: 'TODO' }, container: { data: 'DONE' }, item: { data: item } } as never);
    expect(item.status).toBe('DONE');
    expect(item.updatedAt).toBe('2026-08-10T11:00:00');
  });

  it('dropWorkItem shows generic error on non-permission backend message', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 1;
    component.actorAccountId = 5;
    const item = {
      id: 55,
      workspaceId: 1,
      title: 'Task',
      description: 'Desc',
      dueDate: '2026-08-12',
      priority: 'HIGH' as const,
      status: 'TODO' as const,
      creatorAccountId: 5,
      assignedAccountId: null,
      completedAt: null,
      createdAt: '2026-08-10T10:00:00',
      updatedAt: '2026-08-10T10:00:00'
    };
    api.updateWorkItem.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Other' } })));

    component.dropWorkItem({ previousContainer: { data: 'TODO' }, container: { data: 'DONE' }, item: { data: item } } as never);

    expect(component.errorMessage).toBe('Déplacement de la tâche impossible.');
  });

  it('work item details/history flows are handled', () => {
    const { component, api } = createComponent();
    const baseItem = {
      id: 7,
      workspaceId: 2,
      title: 'Task',
      description: 'Desc',
      dueDate: '2026-08-12',
      priority: 'MEDIUM' as const,
      status: 'IN_PROGRESS' as const,
      creatorAccountId: 1,
      assignedAccountId: 2,
      completedAt: '2026-08-10T10:10:00',
      createdAt: '2026-08-10T10:00:00',
      updatedAt: '2026-08-10T10:00:00'
    };

    component.openWorkItemDetails(baseItem as never);
    expect(api.getWorkItem).not.toHaveBeenCalled();

    component.selectedWorkspaceId = 2;
    component.actorAccountId = 1;
    api.getWorkItem.mockReturnValue(of(baseItem));
    component.openWorkItemDetails(baseItem as never);
    expect(component.selectedWorkItem?.id).toBe(7);
    expect(component.editWorkItemForm.completedAt).toBe('2026-08-10T10:10');

    api.getWorkItem.mockReturnValue(throwError(() => new Error('x')));
    component.openWorkItemDetails(baseItem as never);
    expect(component.errorMessage).toContain('Chargement du détail');

    api.getWorkItemHistory.mockReturnValue(of([{ id: 1 }]));
    component.openWorkItemHistory(baseItem as never);
    expect(component.historyLoading).toBe(false);
    expect(component.selectedWorkItemHistory).toHaveLength(1);

    api.getWorkItemHistory.mockReturnValue(throwError(() => new Error('x')));
    component.openWorkItemHistory(baseItem as never);
    expect(component.historyLoading).toBe(false);
    expect(component.selectedWorkItemHistory).toEqual([]);

    component.closeWorkItemDetails();
    component.closeWorkItemHistory();
    expect(component.selectedWorkItem).toBeNull();
    expect(component.selectedHistoryWorkItem).toBeNull();
  });

  it('saveSelectedWorkItem handles success and history fallback', () => {
    const { component, api } = createComponent();
    component.saveSelectedWorkItem();
    expect(api.updateWorkItem).not.toHaveBeenCalled();

    component.selectedWorkspaceId = 2;
    component.actorAccountId = 1;
    component.selectedWorkItem = {
      id: 8,
      workspaceId: 2,
      title: 'Task',
      description: 'Desc',
      dueDate: '2026-08-12',
      priority: 'MEDIUM',
      status: 'TODO',
      creatorAccountId: 1,
      assignedAccountId: 2,
      completedAt: null,
      createdAt: 'x',
      updatedAt: 'x'
    };
    component.workItems = [component.selectedWorkItem];
    component.editWorkItemForm = {
      title: 'Updated',
      description: 'New',
      dueDate: '2026-08-13',
      priority: 'HIGH',
      status: 'DONE',
      assignedAccountId: null,
      completedAt: '2026-08-13T10:20'
    };

    const updated = { ...component.selectedWorkItem, title: 'Updated', status: 'DONE' as const, completedAt: '2026-08-13T10:20:00' };
    api.updateWorkItem.mockReturnValue(of(updated));
    api.getWorkItemHistory.mockReturnValue(throwError(() => new Error('x')));

    component.saveSelectedWorkItem();

    expect(component.selectedWorkItem?.title).toBe('Updated');
    expect(component.workItems[0].title).toBe('Updated');
    expect(component.selectedWorkItemHistory).toEqual([]);
  });

  it('saveSelectedWorkItem handles backend error messages', () => {
    const { component, api } = createComponent();
    component.selectedWorkspaceId = 2;
    component.actorAccountId = 1;
    component.selectedWorkItem = {
      id: 8,
      workspaceId: 2,
      title: 'Task',
      description: 'Desc',
      dueDate: '2026-08-12',
      priority: 'MEDIUM',
      status: 'TODO',
      creatorAccountId: 1,
      assignedAccountId: 2,
      completedAt: null,
      createdAt: 'x',
      updatedAt: 'x'
    };

    api.updateWorkItem.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Assigned account is not member of workspace' } })));
    component.saveSelectedWorkItem();
    expect(component.errorMessage).toContain('membre assigné doit appartenir');

    api.updateWorkItem.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Permission denied' } })));
    component.saveSelectedWorkItem();
    expect(component.errorMessage).toContain('Tu dois être admin ou membre');

    api.updateWorkItem.mockReturnValue(throwError(() => new HttpErrorResponse({ error: { message: 'Other' } })));
    component.saveSelectedWorkItem();
    expect(component.errorMessage).toContain('Mise à jour de la tâche impossible.');
  });

  it('dismissMessageOnOutsideClick keeps message when clicking inside alert', () => {
    const { component } = createComponent();
    component.errorMessage = 'Err';

    component.dismissMessageOnOutsideClick({
      target: {
        closest: () => ({})
      }
    } as unknown as MouseEvent);

    expect(component.errorMessage).toBe('Err');
  });

  it('dismissMessageOnOutsideClick returns when no message and timer clears error', () => {
    const { component } = createComponent();

    component.dismissMessageOnOutsideClick({ target: null } as unknown as MouseEvent);
    expect(component.errorMessage).toBe('');

    component.refreshWorkspaces();
    expect(component.errorMessage).toContain('Session invalide');
    jest.advanceTimersByTime(7000);
    expect(component.errorMessage).toBe('');
  });
});

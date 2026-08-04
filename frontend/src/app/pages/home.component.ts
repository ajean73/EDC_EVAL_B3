import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Invitation, MemberRole, Notification, TeamMember, WorkItem, WorkItemHistoryEntry, Workspace } from '../core/models';
import { AuthSessionService } from '../core/auth-session.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  readonly statuses: Array<WorkItem['status']> = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
  activeLeftSection: 'board' | 'settings' = 'board';

  workspaces: Workspace[] = [];
  workItems: WorkItem[] = [];
  notifications: Notification[] = [];
  members: TeamMember[] = [];
  myInvitations: Invitation[] = [];
  latestInvitation: Invitation | null = null;
  selectedWorkItem: WorkItem | null = null;
  selectedHistoryWorkItem: WorkItem | null = null;
  selectedWorkItemHistory: WorkItemHistoryEntry[] = [];
  historyLoading = false;

  selectedWorkspaceId: number | null = null;
  actorAccountId: number | null = null;
  currentUsername = '';
  currentWorkspaceRole: MemberRole | null = null;

  createWorkspaceForm = {
    name: '',
    description: '',
    startDate: ''
  };

  createWorkItemForm = {
    title: '',
    description: '',
    dueDate: '',
    priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH',
    assignedAccountId: null as number | null
  };

  editWorkItemForm = {
    title: '',
    description: '',
    dueDate: '',
    priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH',
    status: 'TODO' as WorkItem['status'],
    assignedAccountId: null as number | null,
    completedAt: ''
  };

  inviteForm = {
    inviteeEmail: '',
    role: 'MEMBER' as MemberRole
  };

  loading = false;
  errorMessage = '';
  private messageTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly authSession: AuthSessionService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authSession.getUser();
    if (!user) {
      void this.router.navigate(['/auth']);
      return;
    }

    this.actorAccountId = user.accountId;
    this.currentUsername = user.username;
    this.refreshWorkspaces();
    this.refreshNotifications();
    this.refreshMyInvitations();
  }

  ngOnDestroy(): void {
    this.clearMessageTimer();
  }

  refreshWorkspaces(): void {
    if (!this.actorAccountId) {
      this.showError('Session invalide. Reconnecte-toi.');
      setTimeout(() => this.logout(), 900);
      return;
    }

    this.api.getWorkspaces(this.actorAccountId!).subscribe({
      next: (data) => {
        this.workspaces = data;

        if (this.selectedWorkspaceId && !data.some(workspace => workspace.id === this.selectedWorkspaceId)) {
          this.selectedWorkspaceId = null;
          this.workItems = [];
          this.members = [];
          this.currentWorkspaceRole = null;
        }

        if (!this.selectedWorkspaceId && data.length > 0) {
          this.selectWorkspace(data[0].id);
        }
      },
      error: () => {
        this.showError('Impossible de charger les projets.');
      }
    });
  }

  createWorkspace(): void {
    this.clearErrorMessage();
    this.api.createWorkspace({
      ...this.createWorkspaceForm,
      ownerAccountId: this.actorAccountId!
    }).subscribe({
      next: (createdWorkspace) => {
        this.createWorkspaceForm = { name: '', description: '', startDate: '' };
        this.refreshWorkspaces();
        this.selectWorkspace(createdWorkspace.id);
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);

        if (backendMessage === 'Owner account not found' || backendMessage === 'Account not found') {
          this.showError('Ton compte courant n\'existe plus en base (session obsolète). Reconnecte-toi pour continuer.');
          setTimeout(() => this.logout(), 900);
          return;
        }

        this.showError('Création du projet impossible.');
      }
    });
  }

  selectWorkspace(workspaceId: number): void {
    this.selectedWorkspaceId = workspaceId;
    this.loading = true;
    this.currentWorkspaceRole = null;
    this.members = [];
    this.latestInvitation = null;

    this.refreshMembers();

    this.api.getWorkItems(workspaceId, this.actorAccountId!).subscribe({
      next: (data) => {
        this.workItems = data;
        this.selectedWorkItem = null;
        this.selectedWorkItemHistory = [];
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);
        this.workItems = [];
        this.currentWorkspaceRole = null;

        if (backendMessage === 'Account is not a member of workspace') {
          this.showError('Tu n\'es pas membre de ce projet. Demande une invitation ou fais-toi ajouter par un admin.');
        } else if (backendMessage === 'Permission denied') {
          this.showError('Tu n\'as pas les droits nécessaires pour voir les tâches de ce projet.');
        } else {
          this.showError('Chargement des tâches impossible.');
        }

        this.loading = false;
      }
    });
  }

  createWorkItem(): void {
    if (!this.selectedWorkspaceId) {
      return;
    }

    this.api.createWorkItem(this.selectedWorkspaceId, {
      title: this.createWorkItemForm.title,
      description: this.createWorkItemForm.description,
      dueDate: this.createWorkItemForm.dueDate,
      priority: this.createWorkItemForm.priority,
      creatorAccountId: this.actorAccountId!,
      assignedAccountId: this.createWorkItemForm.assignedAccountId ?? undefined
    }).subscribe({
      next: () => {
        this.createWorkItemForm = {
          title: '',
          description: '',
          dueDate: '',
          priority: 'MEDIUM',
          assignedAccountId: null
        };
        this.selectWorkspace(this.selectedWorkspaceId!);
      },
      error: () => {
        this.showError('Création de la tâche impossible.');
      }
    });
  }

  refreshMembers(): void {
    if (!this.selectedWorkspaceId) {
      return;
    }

    this.api.getWorkspaceMembers(this.selectedWorkspaceId).subscribe({
      next: (data) => {
        this.members = data;
        const me = data.find(member => member.accountId === this.actorAccountId);
        this.currentWorkspaceRole = me ? me.role : null;
      },
      error: () => {
        this.members = [];
        this.currentWorkspaceRole = null;
      }
    });
  }

  inviteMember(): void {
    if (!this.selectedWorkspaceId || !this.actorAccountId) {
      return;
    }

    if (this.currentWorkspaceRole !== 'ADMIN') {
      this.showError('Tu dois être administrateur du projet sélectionné pour inviter des membres.');
      return;
    }

    this.api.inviteToWorkspace(this.selectedWorkspaceId, {
      inviteeEmail: this.inviteForm.inviteeEmail,
      role: this.inviteForm.role,
      actorAccountId: this.actorAccountId
    }).subscribe({
      next: (invitation) => {
        this.latestInvitation = invitation;
        this.inviteForm.inviteeEmail = '';
        this.inviteForm.role = 'MEMBER';
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);
        if (backendMessage === 'Permission denied') {
          this.showError('Seul un administrateur peut inviter des membres.');
        } else {
          this.showError('Invitation impossible.');
        }
      }
    });
  }

  updateRole(member: TeamMember, role: MemberRole): void {
    if (!this.selectedWorkspaceId || !this.actorAccountId) {
      return;
    }

    if (this.currentWorkspaceRole !== 'ADMIN') {
      this.showError('Tu dois être administrateur du projet sélectionné pour modifier les rôles.');
      return;
    }

    this.api.updateMemberRole(this.selectedWorkspaceId, member.accountId, {
      role,
      actorAccountId: this.actorAccountId
    }).subscribe({
      next: (updated) => {
        member.role = updated.role;
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);
        if (backendMessage === 'Permission denied') {
          this.showError('Seul un administrateur peut modifier les rôles.');
        } else {
          this.showError('Mise à jour du rôle impossible.');
        }
      }
    });
  }

  refreshNotifications(): void {
    if (!this.actorAccountId) {
      this.showError('Session invalide. Reconnecte-toi.');
      setTimeout(() => this.logout(), 900);
      return;
    }

    this.api.getNotifications(this.actorAccountId!).subscribe({
      next: (data) => {
        this.notifications = data;
      },
      error: () => {
        this.showError('Chargement des notifications impossible.');
      }
    });
  }

  refreshMyInvitations(): void {
    if (!this.actorAccountId) {
      return;
    }

    this.api.getMyInvitations(this.actorAccountId).subscribe({
      next: (data) => {
        this.myInvitations = data;
      },
      error: () => {
        this.myInvitations = [];
      }
    });
  }

  acceptInvitation(invitationId: number): void {
    if (!this.actorAccountId) {
      return;
    }

    this.api.acceptInvitation(invitationId, this.actorAccountId).subscribe({
      next: () => {
        this.refreshMyInvitations();
        this.refreshWorkspaces();
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);
        if (backendMessage === 'Invitation does not belong to this account') {
          this.showError('Cette invitation ne t\'appartient pas.');
        } else if (backendMessage === 'Invitation already processed') {
          this.showError('Cette invitation a déjà été traitée.');
          this.refreshMyInvitations();
        } else {
          this.showError('Acceptation de l\'invitation impossible.');
        }
      }
    });
  }

  declineInvitation(invitationId: number): void {
    if (!this.actorAccountId) {
      return;
    }

    this.api.declineInvitation(invitationId, this.actorAccountId).subscribe({
      next: () => {
        this.refreshMyInvitations();
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);
        if (backendMessage === 'Invitation does not belong to this account') {
          this.showError('Cette invitation ne t\'appartient pas.');
        } else if (backendMessage === 'Invitation already processed') {
          this.showError('Cette invitation a déjà été traitée.');
          this.refreshMyInvitations();
        } else {
          this.showError('Refus de l\'invitation impossible.');
        }
      }
    });
  }

  getItemsByStatus(status: WorkItem['status']): WorkItem[] {
    return this.workItems.filter(item => item.status === status);
  }

  getAssigneeDisplay(assignedAccountId: number | null): string {
    if (!assignedAccountId) {
      return 'Non assigné';
    }

    const member = this.members.find(m => m.accountId === assignedAccountId);
    if (member?.username) {
      return member.username;
    }

    return `Compte #${assignedAccountId}`;
  }

  getStatusLabel(status: WorkItem['status']): string {
    if (status === 'TODO') {
      return 'A FAIRE';
    }
    if (status === 'IN_PROGRESS') {
      return 'EN COURS';
    }
    if (status === 'IN_REVIEW') {
      return 'A VALIDER';
    }
    return 'TERMINE';
  }

  showBoardSection(): void {
    this.activeLeftSection = 'board';
  }

  showSettingsSection(): void {
    this.activeLeftSection = 'settings';
  }

  logout(): void {
    this.authSession.clear();
    void this.router.navigate(['/auth']);
  }

  getDropListId(status: WorkItem['status']): string {
    return `kanban-${status.toLowerCase()}`;
  }

  dropWorkItem(event: CdkDragDrop<WorkItem['status'], WorkItem['status'], WorkItem>): void {
    if (!this.selectedWorkspaceId || !this.actorAccountId) {
      return;
    }

    const previousStatus = event.previousContainer.data;
    const targetStatus = event.container.data;
    const item = event.item.data;

    if (!item || previousStatus === targetStatus) {
      return;
    }

    const oldStatus = item.status;
    const oldCompletedAt = item.completedAt;

    item.status = targetStatus;
    item.completedAt = targetStatus === 'DONE' ? new Date().toISOString() : null;

    this.api.updateWorkItem(this.selectedWorkspaceId, item.id, {
      status: targetStatus,
      actorAccountId: this.actorAccountId
    }).subscribe({
      next: (updated) => {
        item.status = updated.status;
        item.completedAt = updated.completedAt;
        item.updatedAt = updated.updatedAt;
      },
      error: (error: HttpErrorResponse) => {
        item.status = oldStatus;
        item.completedAt = oldCompletedAt;

        const backendMessage = this.extractBackendMessage(error);
        if (backendMessage === 'Permission denied') {
          this.showError('Tu dois être admin ou membre du projet pour déplacer cette tâche.');
        } else {
          this.showError('Déplacement de la tâche impossible.');
        }
      }
    });
  }

  openWorkItemDetails(item: WorkItem): void {
    if (!this.selectedWorkspaceId || !this.actorAccountId) {
      return;
    }

    this.api.getWorkItem(this.selectedWorkspaceId, item.id, this.actorAccountId).subscribe({
      next: (workItem) => {
        this.selectedWorkItem = workItem;
        this.editWorkItemForm = {
          title: workItem.title,
          description: workItem.description ?? '',
          dueDate: workItem.dueDate,
          priority: workItem.priority,
          status: workItem.status,
          assignedAccountId: workItem.assignedAccountId,
          completedAt: this.toDateTimeLocal(workItem.completedAt)
        };
      },
      error: () => {
        this.showError('Chargement du détail de la tâche impossible.');
      }
    });
  }

  openWorkItemHistory(item: WorkItem): void {
    if (!this.selectedWorkspaceId || !this.actorAccountId) {
      return;
    }

    this.selectedHistoryWorkItem = item;
    this.selectedWorkItemHistory = [];
    this.historyLoading = true;

    this.api.getWorkItemHistory(this.selectedWorkspaceId, item.id, this.actorAccountId).subscribe({
      next: (history) => {
        this.selectedWorkItemHistory = history;
        this.historyLoading = false;
      },
      error: () => {
        this.selectedWorkItemHistory = [];
        this.historyLoading = false;
        this.showError('Chargement de l\'historique impossible.');
      }
    });
  }

  closeWorkItemDetails(): void {
    this.selectedWorkItem = null;
  }

  closeWorkItemHistory(): void {
    this.selectedHistoryWorkItem = null;
    this.selectedWorkItemHistory = [];
    this.historyLoading = false;
  }

  saveSelectedWorkItem(): void {
    if (!this.selectedWorkspaceId || !this.actorAccountId || !this.selectedWorkItem) {
      return;
    }

    this.api.updateWorkItem(this.selectedWorkspaceId, this.selectedWorkItem.id, {
      title: this.editWorkItemForm.title,
      description: this.editWorkItemForm.description,
      dueDate: this.editWorkItemForm.dueDate,
      priority: this.editWorkItemForm.priority,
      status: this.editWorkItemForm.status,
      assignedAccountId: this.editWorkItemForm.assignedAccountId ?? undefined,
      clearAssignee: this.editWorkItemForm.assignedAccountId === null,
      completedAt: this.editWorkItemForm.completedAt || undefined,
      actorAccountId: this.actorAccountId
    }).subscribe({
      next: (updated) => {
        this.selectedWorkItem = updated;
        this.workItems = this.workItems.map(item => item.id === updated.id ? updated : item);
        this.editWorkItemForm.completedAt = this.toDateTimeLocal(updated.completedAt);

        this.api.getWorkItemHistory(this.selectedWorkspaceId!, updated.id, this.actorAccountId!).subscribe({
          next: (history) => {
            this.selectedWorkItemHistory = history;
          },
          error: () => {
            this.selectedWorkItemHistory = [];
          }
        });
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = this.extractBackendMessage(error);
        if (backendMessage === 'Assigned account is not member of workspace') {
          this.showError('Le membre assigné doit appartenir à ce projet.');
        } else if (backendMessage === 'Permission denied') {
          this.showError('Tu dois être admin ou membre du projet pour modifier une tâche.');
        } else {
          this.showError('Mise à jour de la tâche impossible.');
        }
      }
    });
  }

  dismissMessageOnOutsideClick(event: MouseEvent): void {
    if (!this.errorMessage) {
      return;
    }

    const target = event.target as HTMLElement | null;
    if (target?.closest('[data-message-alert]')) {
      return;
    }

    this.clearErrorMessage();
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.clearMessageTimer();
    this.messageTimer = setTimeout(() => {
      this.errorMessage = '';
      this.messageTimer = null;
    }, 7000);
  }

  private clearErrorMessage(): void {
    this.errorMessage = '';
    this.clearMessageTimer();
  }

  private clearMessageTimer(): void {
    if (this.messageTimer) {
      clearTimeout(this.messageTimer);
      this.messageTimer = null;
    }
  }

  private extractBackendMessage(error: HttpErrorResponse): string {
    const payload = error.error;
    if (payload && typeof payload === 'object' && typeof payload.message === 'string') {
      return payload.message;
    }
    return '';
  }

  private toDateTimeLocal(value: string | null): string {
    if (!value) {
      return '';
    }
    return value.length >= 16 ? value.slice(0, 16) : value;
  }
}

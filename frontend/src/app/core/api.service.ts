import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AccountResponse,
  DashboardStatus,
  Invitation,
  LoginResponse,
  MemberRole,
  Notification,
  TeamMember,
  WorkItem,
  WorkItemHistoryEntry,
  Workspace
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = '/api';

  constructor(private readonly http: HttpClient) {}

  // Authentification
  register(payload: { username: string; email: string; password: string }): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.baseUrl}/auth/register`, payload);
  }

  login(payload: { email: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, payload);
  }

  // Workspaces
  getWorkspaces(actorAccountId: number): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(`${this.baseUrl}/workspaces`, {
      params: { actorAccountId }
    });
  }

  createWorkspace(payload: {
    name: string;
    description: string;
    startDate: string;
    ownerAccountId: number;
  }): Observable<Workspace> {
    return this.http.post<Workspace>(`${this.baseUrl}/workspaces`, payload);
  }

  getWorkspaceMembers(workspaceId: number): Observable<TeamMember[]> {
    return this.http.get<TeamMember[]>(`${this.baseUrl}/workspaces/${workspaceId}/members`);
  }

  inviteToWorkspace(workspaceId: number, payload: {
    inviteeEmail: string;
    role: MemberRole;
    actorAccountId: number;
  }): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.baseUrl}/workspaces/${workspaceId}/invitations`, payload);
  }

  getMyInvitations(actorAccountId: number): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.baseUrl}/workspaces/invitations/mine`, {
      params: { actorAccountId }
    });
  }

  acceptInvitation(invitationId: number, actorAccountId: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.baseUrl}/workspaces/invitations/${invitationId}/accept`, {
      actorAccountId
    });
  }

  declineInvitation(invitationId: number, actorAccountId: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.baseUrl}/workspaces/invitations/${invitationId}/decline`, {
      actorAccountId
    });
  }

  updateMemberRole(workspaceId: number, accountId: number, payload: {
    role: MemberRole;
    actorAccountId: number;
  }): Observable<TeamMember> {
    return this.http.patch<TeamMember>(`${this.baseUrl}/workspaces/${workspaceId}/members/${accountId}/role`, payload);
  }

  // Work items
  getWorkItems(workspaceId: number, actorAccountId: number): Observable<WorkItem[]> {
    return this.http.get<WorkItem[]>(`${this.baseUrl}/workspaces/${workspaceId}/work-items`, {
      params: { actorAccountId }
    });
  }

  getWorkItem(workspaceId: number, workItemId: number, actorAccountId: number): Observable<WorkItem> {
    return this.http.get<WorkItem>(`${this.baseUrl}/workspaces/${workspaceId}/work-items/${workItemId}`, {
      params: { actorAccountId }
    });
  }

  createWorkItem(workspaceId: number, payload: {
    title: string;
    description: string;
    dueDate: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH';
    creatorAccountId: number;
    assignedAccountId?: number;
  }): Observable<WorkItem> {
    return this.http.post<WorkItem>(`${this.baseUrl}/workspaces/${workspaceId}/work-items`, payload);
  }

  updateWorkItem(workspaceId: number, workItemId: number, payload: {
    status?: 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
    title?: string;
    description?: string;
    dueDate?: string;
    priority?: 'LOW' | 'MEDIUM' | 'HIGH';
    assignedAccountId?: number;
    clearAssignee?: boolean;
    completedAt?: string;
    actorAccountId: number;
  }): Observable<WorkItem> {
    return this.http.patch<WorkItem>(`${this.baseUrl}/workspaces/${workspaceId}/work-items/${workItemId}`, payload);
  }

  // Requêtes de consultation
  getDashboard(workspaceId: number, actorAccountId: number): Observable<DashboardStatus[]> {
    return this.http.get<DashboardStatus[]>(`${this.baseUrl}/workspaces/${workspaceId}/dashboard`, {
      params: { actorAccountId }
    });
  }

  getWorkItemHistory(workspaceId: number, workItemId: number, actorAccountId: number): Observable<WorkItemHistoryEntry[]> {
    return this.http.get<WorkItemHistoryEntry[]>(`${this.baseUrl}/workspaces/${workspaceId}/work-items/${workItemId}/history`, {
      params: { actorAccountId }
    });
  }

  getNotifications(accountId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.baseUrl}/notifications`, { params: { accountId } });
  }
}

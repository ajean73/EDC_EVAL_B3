export interface AccountResponse {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

export interface LoginResponse {
  accountId: number;
  username: string;
  email: string;
}

// Domaine Workspace
export interface Workspace {
  id: number;
  name: string;
  description: string;
  startDate: string;
  ownerAccountId: number;
  createdAt: string;
}

export type MemberRole = 'ADMIN' | 'MEMBER' | 'OBSERVER';

export interface TeamMember {
  id: number;
  workspaceId: number;
  accountId: number;
  username: string;
  role: MemberRole;
  joinedAt: string;
}

export interface Invitation {
  id: number;
  workspaceId: number;
  inviteeEmail: string;
  role: MemberRole;
  state: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  invitedBy: number;
  invitedByUsername: string;
  createdAt: string;
  respondedAt: string | null;
}

// Domaine Work Item
export interface WorkItem {
  id: number;
  workspaceId: number;
  title: string;
  description: string;
  dueDate: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  status: 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
  creatorAccountId: number;
  assignedAccountId: number | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WorkItemHistoryEntry {
  id: number;
  workItemId: number;
  changedBy: number;
  action: 'CREATED' | 'UPDATED' | 'ASSIGNED' | 'STATUS_CHANGED';
  previousValues: string | null;
  newValues: string | null;
  changedAt: string;
}

// Vue dashboard
export interface DashboardStatus {
  status: 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
  count: number;
}

// Notifications utilisateur
export interface Notification {
  id: number;
  accountId: number;
  workItemId: number;
  kind: 'WORK_ITEM_ASSIGNED' | 'WORK_ITEM_UPDATED';
  isSent: boolean;
  sentAt: string | null;
  createdAt: string;
}

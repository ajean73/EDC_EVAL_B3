import { Injectable } from '@angular/core';
import { LoginResponse } from './models';

const STORAGE_KEY = 'pmt_current_user';

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private currentUser: LoginResponse | null = this.readFromStorage();

  getUser(): LoginResponse | null {
    return this.currentUser;
  }

  isAuthenticated(): boolean {
    return !!this.currentUser;
  }

  setUser(user: LoginResponse): void {
    this.currentUser = user;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  }

  clear(): void {
    this.currentUser = null;
    localStorage.removeItem(STORAGE_KEY);
  }

  private readFromStorage(): LoginResponse | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as LoginResponse;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}

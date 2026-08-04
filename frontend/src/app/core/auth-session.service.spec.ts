import { AuthSessionService } from './auth-session.service';

describe('AuthSessionService', () => {
  const storageKey = 'pmt_current_user';

  beforeEach(() => {
    localStorage.clear();
  });

  it('stores and returns the current user', () => {
    const service = new AuthSessionService();
    const user = { accountId: 7, username: 'alice', email: 'alice@pmt.local' };

    service.setUser(user);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.getUser()).toEqual(user);
    expect(localStorage.getItem(storageKey)).toContain('alice');
  });

  it('clears the stored session', () => {
    const service = new AuthSessionService();
    service.setUser({ accountId: 1, username: 'bob', email: 'bob@pmt.local' });

    service.clear();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getUser()).toBeNull();
    expect(localStorage.getItem(storageKey)).toBeNull();
  });

  it('drops invalid JSON found in localStorage', () => {
    localStorage.setItem(storageKey, '{invalid');

    const service = new AuthSessionService();

    expect(service.getUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(storageKey)).toBeNull();
  });
});

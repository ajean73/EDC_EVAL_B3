import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthSessionService } from './auth-session.service';

describe('authGuard', () => {
  it('returns true when user is authenticated', () => {
    const authMock = { isAuthenticated: jest.fn().mockReturnValue(true) };
    const routerMock = { createUrlTree: jest.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthSessionService, useValue: authMock },
        { provide: Router, useValue: routerMock }
      ]
    });

    const result = TestBed.runInInjectionContext(() => authGuard(null as never, null as never));

    expect(result).toBe(true);
    expect(routerMock.createUrlTree).not.toHaveBeenCalled();
  });

  it('redirects to auth page when user is not authenticated', () => {
    const authMock = { isAuthenticated: jest.fn().mockReturnValue(false) };
    const expectedTree = {} as UrlTree;
    const routerMock = { createUrlTree: jest.fn().mockReturnValue(expectedTree) };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthSessionService, useValue: authMock },
        { provide: Router, useValue: routerMock }
      ]
    });

    const result = TestBed.runInInjectionContext(() => authGuard(null as never, null as never));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/auth']);
    expect(result).toBe(expectedTree);
  });
});

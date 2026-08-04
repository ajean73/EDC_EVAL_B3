import { of, throwError } from 'rxjs';
import { AuthComponent } from './auth.component';

describe('AuthComponent', () => {
  const createComponent = (options?: { authenticated?: boolean }) => {
    const api = {
      login: jest.fn(),
      register: jest.fn()
    };
    const authSession = {
      isAuthenticated: jest.fn().mockReturnValue(options?.authenticated ?? false),
      setUser: jest.fn()
    };
    const router = {
      navigate: jest.fn().mockResolvedValue(true)
    };

    const component = new AuthComponent(api as never, authSession as never, router as never);
    return { component, api, authSession, router };
  };

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('redirects to app when already authenticated', () => {
    const { router } = createComponent({ authenticated: true });

    expect(router.navigate).toHaveBeenCalledWith(['/app']);
  });

  it('switchMode clears both messages', () => {
    const { component } = createComponent();
    component.errorMessage = 'error';
    component.successMessage = 'ok';

    component.switchMode('register');

    expect(component.mode).toBe('register');
    expect(component.errorMessage).toBe('');
    expect(component.successMessage).toBe('');
  });

  it('login stores user and navigates on success', () => {
    const { component, api, authSession, router } = createComponent();
    const user = { accountId: 8, username: 'alice', email: 'alice@pmt.local' };
    component.loginForm = { email: user.email, password: 'Secret123' };
    api.login.mockReturnValue(of(user));

    component.login();

    expect(api.login).toHaveBeenCalledWith(component.loginForm);
    expect(authSession.setUser).toHaveBeenCalledWith(user);
    expect(component.loading).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/app']);
  });

  it('login sets error message on failure', () => {
    const { component, api } = createComponent();
    api.login.mockReturnValue(throwError(() => new Error('bad creds')));

    component.login();

    expect(component.loading).toBe(false);
    expect(component.errorMessage).toBe('Identifiants invalides.');
    expect(component.successMessage).toBe('');
  });

  it('register switches to login and pre-fills login email on success', () => {
    const { component, api } = createComponent();
    component.registerForm = {
      username: 'new-user',
      email: 'new-user@pmt.local',
      password: 'Strong1234'
    };
    api.register.mockReturnValue(of({ id: 1 }));

    component.register();

    expect(component.mode).toBe('login');
    expect(component.loading).toBe(false);
    expect(component.successMessage).toBe('Compte créé. Connecte-toi maintenant.');
    expect(component.loginForm.email).toBe('new-user@pmt.local');
    expect(component.registerForm).toEqual({ username: '', email: '', password: '' });
  });

  it('register sets error message on failure', () => {
    const { component, api } = createComponent();
    api.register.mockReturnValue(throwError(() => new Error('boom')));

    component.register();

    expect(component.loading).toBe(false);
    expect(component.errorMessage).toBe('Création de compte impossible.');
    expect(component.successMessage).toBe('');
  });

  it('dismissMessageOnOutsideClick clears messages when clicking outside alert', () => {
    const { component } = createComponent();
    component.errorMessage = 'Erreur';
    component.successMessage = '';

    component.dismissMessageOnOutsideClick({
      target: {
        closest: () => null
      }
    } as unknown as MouseEvent);

    expect(component.errorMessage).toBe('');
  });

  it('dismissMessageOnOutsideClick keeps messages when clicking inside alert', () => {
    const { component } = createComponent();
    component.errorMessage = 'Erreur';

    component.dismissMessageOnOutsideClick({
      target: {
        closest: () => ({})
      }
    } as unknown as MouseEvent);

    expect(component.errorMessage).toBe('Erreur');
  });

  it('ngOnDestroy clears timer to avoid delayed message reset', () => {
    const { component, api } = createComponent();
    api.login.mockReturnValue(throwError(() => new Error('bad creds')));

    component.login();
    expect(component.errorMessage).toBe('Identifiants invalides.');

    component.ngOnDestroy();
    jest.advanceTimersByTime(7000);

    expect(component.errorMessage).toBe('Identifiants invalides.');
  });

  it('message timer clears notification after 7 seconds', () => {
    const { component, api } = createComponent();
    api.login.mockReturnValue(throwError(() => new Error('bad creds')));

    component.login();
    expect(component.errorMessage).toBe('Identifiants invalides.');

    jest.advanceTimersByTime(7000);

    expect(component.errorMessage).toBe('');
    expect(component.successMessage).toBe('');
  });
});

import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthSessionService } from '../core/auth-session.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html'
})
export class AuthComponent implements OnDestroy {
  // L'ecran alterne entre connexion et inscription sans changer de route.
  mode: 'login' | 'register' = 'login';
  loading = false;
  errorMessage = '';
  successMessage = '';
  private messageTimer: ReturnType<typeof setTimeout> | null = null;

  loginForm = {
    email: '',
    password: ''
  };

  registerForm = {
    username: '',
    email: '',
    password: ''
  };

  constructor(
    private readonly api: ApiService,
    private readonly authSession: AuthSessionService,
    private readonly router: Router
  ) {
    // Si une session existe déjà, on évite de re-afficher le formulaire d'authentification.
    if (this.authSession.isAuthenticated()) {
      void this.router.navigate(['/app']);
    }
  }

  switchMode(mode: 'login' | 'register'): void {
    this.mode = mode;
    this.clearMessages();
  }

  login(): void {
    this.loading = true;
    this.clearMessages();

    // Le backend renvoie l'identité utilisateur, stockée ensuite par AuthSessionService.
    this.api.login(this.loginForm).subscribe({
      next: (user) => {
        this.authSession.setUser(user);
        this.loading = false;
        void this.router.navigate(['/app']);
      },
      error: (err) => {
        this.showError(this.extractErrorMessage(err, 'Identifiants invalides.'));
        this.loading = false;
      }
    });
  }

  register(): void {
    this.loading = true;
    this.clearMessages();

    this.api.register(this.registerForm).subscribe({
      next: () => {
        // UX: pré-remplit l'email pour enchaîner naturellement vers la connexion.
        this.showSuccess('Compte créé. Connecte-toi maintenant.');
        this.loading = false;
        this.mode = 'login';
        this.loginForm.email = this.registerForm.email;
        this.loginForm.password = '';
        this.registerForm = { username: '', email: '', password: '' };
      },
      error: (err) => {
        this.showError(this.extractErrorMessage(err, 'Création de compte impossible.'));
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.clearMessageTimer();
  }

  dismissMessageOnOutsideClick(event: MouseEvent): void {
    if (!this.errorMessage && !this.successMessage) {
      return;
    }

    const target = event.target as HTMLElement | null;
    if (target?.closest('[data-message-alert]')) {
      return;
    }

    // Clic en dehors du composant d'alerte: fermeture manuelle du message.
    this.clearMessages();
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    this.startMessageTimer();
  }

  private showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    this.startMessageTimer();
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.clearMessageTimer();
  }

  private startMessageTimer(): void {
    this.clearMessageTimer();
    // Les alertes disparaissent automatiquement pour garder une interface propre.
    this.messageTimer = setTimeout(() => {
      this.errorMessage = '';
      this.successMessage = '';
      this.messageTimer = null;
    }, 7000);
  }

  private clearMessageTimer(): void {
    if (this.messageTimer) {
      clearTimeout(this.messageTimer);
      this.messageTimer = null;
    }
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    const maybe = err as { error?: { message?: string } };
    const message = maybe?.error?.message;
    if (typeof message === 'string' && message.trim().length > 0) {
      return message;
    }
    return fallback;
  }
}

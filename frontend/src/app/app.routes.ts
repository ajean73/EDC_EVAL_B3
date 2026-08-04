import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { AuthComponent } from './pages/auth.component';
import { HomeComponent } from './pages/home.component';

// Routage principal de l'application frontend.
export const appRoutes: Routes = [
  // Entrée par défaut vers l'authentification.
  { path: '', pathMatch: 'full', redirectTo: 'auth' },
  { path: 'auth', component: AuthComponent },
  // Zone protégée par session.
  { path: 'app', component: HomeComponent, canActivate: [authGuard] },
  // Fallback URL inconnue.
  { path: '**', redirectTo: 'auth' }
];

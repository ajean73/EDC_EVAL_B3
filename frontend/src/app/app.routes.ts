import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { AuthComponent } from './pages/auth.component';
import { HomeComponent } from './pages/home.component';

export const appRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'auth' },
  { path: 'auth', component: AuthComponent },
  { path: 'app', component: HomeComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'auth' }
];

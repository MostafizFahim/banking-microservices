import { Routes } from '@angular/router';
import { LoginComponent } from './components/auth/login.component';
import { RegisterComponent } from './components/auth/register.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { AccountDetailComponent } from './components/account/account-detail.component';
import { CreateAccountComponent } from './components/account/create-account.component';
import { AuthGuard } from './guards/auth.guard';
import { AnalyticsComponent } from './components/analytics/analytics.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'accounts/new', component: CreateAccountComponent, canActivate: [AuthGuard] },
  { path: 'accounts/:accountNumber', component: AccountDetailComponent, canActivate: [AuthGuard] },
  { path: 'analytics', component: AnalyticsComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];

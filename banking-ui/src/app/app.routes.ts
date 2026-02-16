import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { AccountDetailComponent } from './components/account/account-detail.component';
import { CreateAccountComponent } from './components/account/create-account.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'accounts/new', component: CreateAccountComponent },
  { path: 'accounts/:accountNumber', component: AccountDetailComponent },
  { path: '**', redirectTo: '/dashboard' }
];
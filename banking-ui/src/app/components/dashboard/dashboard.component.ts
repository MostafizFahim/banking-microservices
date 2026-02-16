import { Component, OnInit, ChangeDetectorRef } from '@angular/core';  // Add ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AccountService } from '../../services/account.service';
import { Account } from '../../models/account.model';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class DashboardComponent implements OnInit {
  accounts: Account[] = [];
  loading = true;  // Start with true
  totalBalance = 0;
  totalAccounts = 0;
  activeAccounts = 0;

  constructor(
    private accountService: AccountService,
    private notification: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef  // Add this for manual change detection
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    console.log('Loading accounts...');
    this.loading = true;
    
    this.accountService.getAllAccounts().subscribe({
      next: (accounts) => {
        console.log('Accounts loaded:', accounts);
        this.accounts = accounts;
        this.calculateStats();
        this.loading = false;  // Make sure this is set
        this.cdr.detectChanges();  // Force change detection
        console.log('Loading set to false');
      },
      error: (error) => {
        console.error('Error loading accounts:', error);
        this.loading = false;
        this.cdr.detectChanges();
        this.notification.showError('Failed to load accounts');
      }
    });
  }

  calculateStats(): void {
    this.totalAccounts = this.accounts.length;
    this.activeAccounts = this.accounts.filter(a => a.status === 'ACTIVE').length;
    this.totalBalance = this.accounts.reduce((sum, acc) => sum + acc.balance, 0);
  }

 viewAccount(accountNumber: string): void {
  console.log('Viewing account:', accountNumber);
  if (accountNumber) {
    this.router.navigate(['/accounts', accountNumber]).then(success => {
      if (success) {
        console.log('Navigation successful');
      } else {
        console.log('Navigation failed');
        this.notification.showError('Failed to navigate to account');
      }
    });
  }
}

  createNewAccount(): void {
    this.router.navigate(['/accounts/new']);
  }

  refreshDashboard(): void {
    this.loadAccounts();
  }
  trackByAccountNumber(index: number, account: Account): string {
  return account.accountNumber;
}
}
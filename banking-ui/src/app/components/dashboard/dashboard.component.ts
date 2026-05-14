import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
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
  loading = true;
  totalBalance = 0;
  totalAccounts = 0;
  activeAccounts = 0;

  constructor(
    private accountService: AccountService,
    private notification: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    setTimeout(() => this.loadAccounts(false));
  }

  loadAccounts(showLoading = true): void {
    if (showLoading) {
      this.loading = true;
    }
    
    this.accountService.getAllAccounts().subscribe({
      next: (accounts) => {
        this.applyAccounts(accounts);
      },
      error: () => {
        this.scheduleViewUpdate(() => {
          this.loading = false;
          this.notification.showError('Failed to load accounts');
        });
      }
    });
  }

  calculateStats(): void {
    this.totalAccounts = this.accounts.length;
    this.activeAccounts = this.accounts.filter(a => a.status === 'ACTIVE').length;
    this.totalBalance = this.accounts.reduce((sum, acc) => sum + acc.balance, 0);
  }

  private applyAccounts(accounts: Account[]): void {
    this.scheduleViewUpdate(() => {
      this.accounts = accounts;
      this.calculateStats();
      this.loading = false;
    });
  }

  private scheduleViewUpdate(update: () => void): void {
    setTimeout(() => {
      update();
      this.cdr.detectChanges();
    });
  }

  viewAccount(accountNumber: string): void {
    if (accountNumber) {
      this.router.navigate(['/accounts', accountNumber]).then(success => {
        if (!success) {
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

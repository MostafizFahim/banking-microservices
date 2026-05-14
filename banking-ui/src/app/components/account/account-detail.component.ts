import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AccountService } from '../../services/account.service';
import { NotificationService } from '../../services/notification.service';
import { AuthService } from '../../services/auth.service';
import { Account } from '../../models/account.model';
import { Transaction, TransactionSummary, TransactionRequest } from '../../models/transaction.model';
import { ExportService } from '../../services/export.service';

@Component({
  selector: 'app-account-detail',
  templateUrl: './account-detail.component.html',
  styleUrls: ['./account-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule]
})
export class AccountDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private destroyed = false;

  account: Account | null = null;
  transactions: Transaction[] = [];
  summary: TransactionSummary | null = null;
  transactionForm: FormGroup;

  loading = true;
  loadingTransactions = false;
  submitting = false;
  isAdmin = false;

  accountNumber = '';
  originalTransactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private accountService: AccountService,
    private authService: AuthService,
    private notification: NotificationService,
    private fb: FormBuilder,
    private exportService: ExportService,
    private cdr: ChangeDetectorRef
  ) {
    this.transactionForm = this.fb.group({
      transactionType: ['DEPOSIT', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.isAdmin = this.authService.getRole() === 'ADMIN';

    this.route.params.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (params) => {
        this.accountNumber = params['accountNumber'];
        if (this.accountNumber) {
          this.loadAllData();
        } else {
          this.notification.showError('No account number provided');
          this.router.navigate(['/dashboard']);
        }
      },
      error: () => {
        this.notification.showError('Error loading account');
        this.router.navigate(['/dashboard']);
      }
    });
  }

  loadAllData(): void {
    this.scheduleViewUpdate(() => {
      this.loading = true;
      this.summary = null;
    });

    this.accountService.getAccountByNumber(this.accountNumber)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (account) => {
          this.scheduleViewUpdate(() => {
            this.loading = false;

            if (account) {
              this.account = account;
              this.loadTransactions();
              this.loadSummary();
            } else {
              this.notification.showError('Account not found');
              this.router.navigate(['/dashboard']);
            }
          });
        },
        error: () => {
          this.scheduleViewUpdate(() => {
            this.loading = false;
            this.notification.showError('Failed to load account details');
            this.router.navigate(['/dashboard']);
          });
        }
      });
  }

  loadTransactions(): void {
    this.scheduleViewUpdate(() => {
      this.loadingTransactions = true;
    });

    this.accountService.getAccountTransactions(this.accountNumber)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (transactions) => {
          this.scheduleViewUpdate(() => {
            this.originalTransactions = transactions;
            this.filteredTransactions = transactions;
            this.loadingTransactions = false;
          });
        },
        error: () => {
          this.scheduleViewUpdate(() => {
            this.originalTransactions = [];
            this.filteredTransactions = [];
            this.loadingTransactions = false;
          });
        }
      });
  }

  loadSummary(): void {
    this.accountService.getTransactionSummary(this.accountNumber)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary) => {
          this.scheduleViewUpdate(() => {
            this.summary = summary;
          });
        },
        error: () => {}
      });
  }

  onSubmitTransaction(): void {
    if (this.transactionForm.invalid) {
      this.notification.showWarning('Please enter a valid amount');
      return;
    }

    if (!this.account) {
      this.notification.showError('Account not found');
      return;
    }

    const formValue = this.transactionForm.value;
    const amount = Number(formValue.amount);

    if (isNaN(amount) || amount <= 0) {
      this.notification.showWarning('Please enter a valid positive amount');
      return;
    }

    const request: TransactionRequest = {
      accountNumber: this.accountNumber,
      transactionType: formValue.transactionType,
      amount: amount,
      description: formValue.description || `${formValue.transactionType} transaction`
    };

    this.scheduleViewUpdate(() => {
      this.submitting = true;
    });

    this.accountService.processTransaction(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedAccount) => {
          this.scheduleViewUpdate(() => {
            this.submitting = false;

            if (updatedAccount) {
              this.account = updatedAccount;
              this.loadTransactions();
              this.loadSummary();
              this.transactionForm.reset({
                transactionType: 'DEPOSIT',
                amount: '',
                description: ''
              });
              this.notification.showSuccess('Transaction completed successfully');
            }
          });
        },
        error: () => {
          this.scheduleViewUpdate(() => {
            this.submitting = false;
          });
        }
      });
  }

  toggleAccountStatus(): void {
    if (!this.account) return;

    const newStatus = this.account.status === 'ACTIVE' ? 'FROZEN' : 'ACTIVE';
    const action = newStatus === 'ACTIVE' ? 'Activate' : 'Freeze';

    if (confirm(`Are you sure you want to ${action} this account?`)) {
      this.accountService.updateAccountStatus(this.accountNumber, newStatus).subscribe({
        next: (updatedAccount) => {
          if (updatedAccount) {
            this.scheduleViewUpdate(() => {
              this.account = updatedAccount;
            });
          }
        }
      });
    }
  }

  applyFilters(fromDate: string, toDate: string, type: string, minAmount: string): void {
    let filtered = [...this.originalTransactions];

    if (fromDate) {
      const from = new Date(fromDate);
      filtered = filtered.filter(tx => new Date(tx.timestamp) >= from);
    }

    if (toDate) {
      const to = new Date(toDate);
      to.setHours(23, 59, 59);
      filtered = filtered.filter(tx => new Date(tx.timestamp) <= to);
    }

    if (type && type !== 'ALL') {
      filtered = filtered.filter(tx => tx.transactionType === type);
    }

    if (minAmount && !isNaN(Number(minAmount))) {
      const min = Number(minAmount);
      filtered = filtered.filter(tx => tx.amount >= min);
    }

    this.filteredTransactions = filtered;

    if (this.filteredTransactions.length === 0) {
      this.notification.showInfo('No transactions match your filters');
    } else {
      this.notification.showSuccess(`Found ${this.filteredTransactions.length} transactions`);
    }
  }

  resetFilters(): void {
    this.filteredTransactions = [...this.originalTransactions];
    this.notification.showInfo('Filters reset');
  }

  exportTransactions(): void {
    if (this.filteredTransactions.length === 0) {
      this.notification.showWarning('No transactions to export');
      return;
    }

    this.exportService.exportToCSV(
      this.filteredTransactions,
      `account_${this.accountNumber}_transactions`
    );

    this.notification.showSuccess(`Exported ${this.filteredTransactions.length} transactions`);
  }

  exportTransactionsPDF(): void {
    if (this.filteredTransactions.length === 0) {
      this.notification.showWarning('No transactions to export');
      return;
    }

    this.exportService.exportToPDF(
      this.filteredTransactions,
      `account_${this.accountNumber}_transactions`
    );

    this.notification.showSuccess(`Exported ${this.filteredTransactions.length} transactions`);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  trackByTransactionId(index: number, transaction: Transaction): string {
    return transaction.id || transaction.reference || index.toString();
  }

  private scheduleViewUpdate(update: () => void): void {
    setTimeout(() => {
      if (this.destroyed) return;
      update();
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.destroy$.next();
    this.destroy$.complete();
  }
}

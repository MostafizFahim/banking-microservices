import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';  // Add ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil, finalize } from 'rxjs';
import { AccountService } from '../../services/account.service';
import { NotificationService } from '../../services/notification.service';
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

  account: Account | null = null;
  transactions: Transaction[] = [];
  summary: TransactionSummary | null = null;
  transactionForm: FormGroup;

  loading = true;
  loadingTransactions = false;
  submitting = false;

  accountNumber: string = '';

  // Add these properties
  originalTransactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private accountService: AccountService,
    private notification: NotificationService,
    private fb: FormBuilder,
     private exportService: ExportService,
    private cdr: ChangeDetectorRef  // Add this
  ) {
    this.transactionForm = this.fb.group({
      transactionType: ['DEPOSIT', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.route.params.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (params) => {
        this.accountNumber = params['accountNumber'];
        console.log('Loading account:', this.accountNumber);
        if (this.accountNumber) {
          this.loadAllData();
        } else {
          this.notification.showError('No account number provided');
          this.router.navigate(['/dashboard']);
        }
      },
      error: (error) => {
        console.error('Route error:', error);
        this.notification.showError('Error loading account');
        this.router.navigate(['/dashboard']);
      }
    });
  }

  loadAllData(): void {
    this.loading = true;
    this.cdr.detectChanges(); // Force update for loading state

    this.accountService.getAccountByNumber(this.accountNumber)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges(); // Force update after loading completes
          console.log('Account loading finished, loading set to:', this.loading);
        })
      )
      .subscribe({
        next: (account) => {
          console.log('Account loaded:', account);
          if (account) {
            this.account = account;
            this.cdr.detectChanges(); // Force update after account is set
            this.loadTransactions();
            this.loadSummary();
          } else {
            this.notification.showError('Account not found');
            this.router.navigate(['/dashboard']);
          }
        },
        error: (error) => {
          console.error('Error loading account:', error);
          this.notification.showError('Failed to load account details');
          this.router.navigate(['/dashboard']);
        }
      });
  }

  // Update loadTransactions method
  loadTransactions(): void {
    this.loadingTransactions = true;
    this.accountService.getAccountTransactions(this.accountNumber)
      .pipe(finalize(() => this.loadingTransactions = false))
      .subscribe({
        next: (transactions) => {
          this.originalTransactions = transactions;
          this.filteredTransactions = transactions;
        },
        error: (error) => {
          console.error('Error loading transactions:', error);
          this.originalTransactions = [];
          this.filteredTransactions = [];
        }
      });
  }

  loadSummary(): void {
    this.accountService.getTransactionSummary(this.accountNumber)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary) => {
          console.log('Summary loaded:', summary);
          this.summary = summary;
          this.cdr.detectChanges(); // Force update
        },
        error: (error) => {
          console.error('Error loading summary:', error);
        }
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

    console.log('Submitting transaction:', request);
    this.submitting = true;
    this.cdr.detectChanges(); // Force update

    this.accountService.processTransaction(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.submitting = false;
          this.cdr.detectChanges(); // Force update
          console.log('Transaction submission finished');
        })
      )
      .subscribe({
        next: (updatedAccount) => {
          console.log('Transaction success, updated account:', updatedAccount);
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
            this.cdr.detectChanges(); // Force update
          }
        },
        error: (error) => {
          console.error('Transaction error:', error);
        }
      });
  }

  // Add this method
toggleAccountStatus(): void {
  if (!this.account) return;

  const newStatus = this.account.status === 'ACTIVE' ? 'FROZEN' : 'ACTIVE';
  const action = newStatus === 'ACTIVE' ? 'Activate' : 'Freeze';

  if (confirm(`Are you sure you want to ${action} this account?`)) {
    this.accountService.updateAccountStatus(this.accountNumber, newStatus).subscribe({
      next: (updatedAccount) => {
        if (updatedAccount) {
          this.account = updatedAccount;
          this.cdr.detectChanges();
        }
      }
    });
  }
}

// Add filter methods
applyFilters(fromDate: string, toDate: string, type: string, minAmount: string): void {
  let filtered = [...this.originalTransactions];

  // Filter by date range
  if (fromDate) {
    const from = new Date(fromDate);
    filtered = filtered.filter(tx => new Date(tx.timestamp) >= from);
  }

  if (toDate) {
    const to = new Date(toDate);
    to.setHours(23, 59, 59); // End of day
    filtered = filtered.filter(tx => new Date(tx.timestamp) <= to);
  }

  // Filter by transaction type
  if (type && type !== 'ALL') {
    filtered = filtered.filter(tx => tx.transactionType === type);
  }

  // Filter by minimum amount
  if (minAmount && !isNaN(Number(minAmount))) {
    const min = Number(minAmount);
    filtered = filtered.filter(tx => tx.amount >= min);
  }

  this.filteredTransactions = filtered;

  // Show notification
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
// Add export methods
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

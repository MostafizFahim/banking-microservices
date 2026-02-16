import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';  // Add ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil, finalize } from 'rxjs';
import { AccountService } from '../../services/account.service';
import { NotificationService } from '../../services/notification.service';
import { Account } from '../../models/account.model';
import { Transaction, TransactionSummary, TransactionRequest } from '../../models/transaction.model';

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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private accountService: AccountService,
    private notification: NotificationService,
    private fb: FormBuilder,
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

  loadTransactions(): void {
    this.loadingTransactions = true;
    this.cdr.detectChanges(); // Force update
    
    this.accountService.getAccountTransactions(this.accountNumber)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loadingTransactions = false;
          this.cdr.detectChanges(); // Force update
        })
      )
      .subscribe({
        next: (transactions) => {
          console.log('Transactions loaded:', transactions);
          this.transactions = transactions;
          this.cdr.detectChanges(); // Force update
        },
        error: (error) => {
          console.error('Error loading transactions:', error);
          this.transactions = [];
          this.cdr.detectChanges(); // Force update
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
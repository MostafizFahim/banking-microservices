import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, of, tap, timeout } from 'rxjs';
import { environment } from '../../environments/environment';
import { Account, AccountDTO } from '../models/account.model';
import { ApiResponse } from '../models/api-response.model';
import { Transaction, TransactionRequest, TransactionSummary } from '../models/transaction.model';
import { NotificationService } from './notification.service';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = environment.apiUrl;
  private readonly timeoutMs = 10000; // 10 seconds timeout

  constructor(
    private http: HttpClient,
    private notification: NotificationService,
    private authService: AuthService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllAccounts(): Observable<Account[]> {
    const token = this.authService.getToken();
    if (!token) {
      this.notification.showError('Please login first');
      return of([]);
    }

    return this.http.get<ApiResponse<Account[]>>(`${this.apiUrl}/accounts`, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      map(response => {
        if (response.success) {
          return response.data || [];
        }
        return [];
      }),
      catchError(this.handleError<Account[]>('getAllAccounts', []))
    );
  }

  getAccountById(id: string): Observable<Account | null> {
    return this.http.get<ApiResponse<Account>>(`${this.apiUrl}/accounts/${id}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('getAccountById', null))
    );
  }

  getAccountByNumber(accountNumber: string): Observable<Account | null> {
    return this.http.get<ApiResponse<Account>>(`${this.apiUrl}/accounts/number/${accountNumber}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('getAccountByNumber', null))
    );
  }

  createAccount(account: AccountDTO): Observable<Account | null> {
    return this.http.post<ApiResponse<Account>>(`${this.apiUrl}/accounts`, account, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      tap(response => {
        if (response.success && response.data) {
          this.notification.showSuccess(response.message || 'Account created successfully');
        }
      }),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('createAccount', null))
    );
  }

  deposit(accountNumber: string, amount: number): Observable<Account | null> {
    return this.http.post<ApiResponse<Account>>(
      `${this.apiUrl}/accounts/${accountNumber}/deposit?amount=${amount}`,
      {},
      { headers: this.getAuthHeaders() }
    ).pipe(
      timeout(this.timeoutMs),
      tap(response => {
        if (response.success) {
          this.notification.showSuccess(response.message || `Successfully deposited $${amount}`);
        }
      }),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('deposit', null))
    );
  }

  withdraw(accountNumber: string, amount: number): Observable<Account | null> {
    return this.http.post<ApiResponse<Account>>(
      `${this.apiUrl}/accounts/${accountNumber}/withdraw?amount=${amount}`,
      {},
      { headers: this.getAuthHeaders() }
    ).pipe(
      timeout(this.timeoutMs),
      tap(response => {
        if (response.success) {
          this.notification.showSuccess(response.message || `Successfully withdrew $${amount}`);
        }
      }),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('withdraw', null))
    );
  }

  processTransaction(request: TransactionRequest): Observable<Account | null> {
    return this.http.post<ApiResponse<Account>>(`${this.apiUrl}/accounts/transactions`, request, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      tap(response => {
        if (response.success) {
          this.notification.showSuccess(response.message || 'Transaction completed successfully');
        }
      }),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('processTransaction', null))
    );
  }

  getAccountTransactions(accountNumber: string): Observable<Transaction[]> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.apiUrl}/transactions/account/${accountNumber}`, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      map(response => response.success ? response.data || [] : []),
      catchError(this.handleError<Transaction[]>('getAccountTransactions', []))
    );
  }

  getMyTransactions(): Observable<Transaction[]> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.apiUrl}/transactions/my`, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      map(response => response.success ? response.data || [] : []),
      catchError(this.handleError<Transaction[]>('getMyTransactions', []))
    );
  }

  getTransactionSummary(accountNumber: string): Observable<TransactionSummary | null> {
    return this.http.get<ApiResponse<TransactionSummary>>(`${this.apiUrl}/transactions/account/${accountNumber}/summary`, {
      headers: this.getAuthHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<TransactionSummary | null>('getTransactionSummary', null))
    );
  }

  updateAccountStatus(accountNumber: string, status: string): Observable<Account | null> {
    return this.http.put<ApiResponse<Account>>(
      `${this.apiUrl}/accounts/${accountNumber}/status?status=${status}`,
      {},
      { headers: this.getAuthHeaders() }
    ).pipe(
      timeout(this.timeoutMs),
      tap(response => {
        if (response.success) {
          this.notification.showSuccess(`Account ${status.toLowerCase()} successfully`);
        }
      }),
      map(response => response.success ? response.data : null),
      catchError(this.handleError<Account | null>('updateAccountStatus', null))
    );
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      let errorMessage = 'An error occurred';

      if (error.name === 'TimeoutError') {
        errorMessage = 'Request timed out. Please try again.';
      } else if (error instanceof HttpErrorResponse) {
        if (error.status === 0) {
          errorMessage = 'Cannot connect to server. Is the backend running?';
        } else if (error.status === 401) {
          errorMessage = 'Unauthorized. Please login again.';
          this.authService.logout();
        } else if (error.status === 403) {
          errorMessage = 'Access forbidden. You don\'t have permission to access this resource.';
        } else if (error.status === 404) {
          errorMessage = 'Resource not found';
        } else if (error.status === 500) {
          errorMessage = 'Server error. Please try again later.';
        } else if (error.error?.message) {
          errorMessage = error.error.message;
        }
      }

      this.notification.showError(errorMessage);
      return of(result as T);
    };
  }
}

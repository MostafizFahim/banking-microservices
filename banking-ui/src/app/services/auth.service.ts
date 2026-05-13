import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
  accountNumber: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/auth/login`, { username, password })
      .pipe(
        map(response => {
          if (response.success && response.data) {
            return response.data;
          }
          throw new Error(response.message || 'Login failed');
        }),
        tap(authData => {
          if (authData && authData.token) {
            localStorage.setItem('token', authData.token);
            localStorage.setItem('username', authData.username);
            localStorage.setItem('role', authData.role);
            localStorage.setItem('accountNumber', authData.accountNumber || '');
          }
        })
      );
  }

  register(username: string, password: string, email?: string): Observable<any> {
    const payload: any = { username, password };
    if (email) {
      payload.email = email;
    }
    return this.http.post(`${this.apiUrl}/auth/register`, payload);
  }

  logout(): void {
    localStorage.clear();
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  getAccountNumber(): string | null {
    return localStorage.getItem('accountNumber');
  }
}

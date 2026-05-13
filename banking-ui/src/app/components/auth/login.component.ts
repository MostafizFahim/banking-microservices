import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  username: string = '';
  password: string = '';
  loading = false;
  showPassword = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      const accountNumber = this.authService.getAccountNumber();
      if (accountNumber && accountNumber !== '') {
        this.router.navigate(['/accounts', accountNumber]);
      } else {
        this.router.navigate(['/dashboard']);
      }
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.notification.showWarning('Please enter username and password');
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();

    this.authService.login(this.username, this.password).subscribe({
      next: (authData) => {
        this.loading = false;
        this.cdr.detectChanges();

        this.notification.showSuccess('Login successful!');

        if (authData.role === 'ADMIN') {
          this.router.navigate(['/dashboard']);
        } else if (authData.accountNumber && authData.accountNumber !== '') {
          this.router.navigate(['/accounts', authData.accountNumber]);
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (error) => {
        this.loading = false;
        this.cdr.detectChanges();
        const message = error.message || 'Invalid username or password';
        this.notification.showError(message);
      }
    });
  }
}

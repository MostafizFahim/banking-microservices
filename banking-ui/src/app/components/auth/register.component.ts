import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  username: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef  // Add this
  ) {}

  onSubmit(): void {
    // Validation
    if (!this.username || !this.password) {
      this.notification.showWarning('Please fill all required fields');
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.notification.showWarning('Passwords do not match');
      return;
    }

    if (this.password.length < 6) {
      this.notification.showWarning('Password must be at least 6 characters');
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();

    // Pass email only if provided
    const email = this.email || `${this.username}@bank.com`;

    this.authService.register(this.username, this.password, email).subscribe({
      next: (response) => {
        this.loading = false;
        this.cdr.detectChanges();
        this.notification.showSuccess('Registration successful! Please login.');
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.loading = false;
        this.cdr.detectChanges();
        const message = error.error?.message || 'Registration failed. Username may already exist.';
        this.notification.showError(message);
      }
    });
  }
}

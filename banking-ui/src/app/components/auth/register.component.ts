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
  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  get isValidEmail(): boolean {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(this.email);
  }

  isFormValid(): boolean {
    return this.username &&
           this.email &&
           this.isValidEmail &&
           this.password &&
           this.confirmPassword &&
           this.password === this.confirmPassword &&
           this.password.length >= 6;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    if (!this.username || !this.password || !this.email) {
      this.notification.showWarning('Please fill all fields');
      return;
    }

    if (!this.isValidEmail) {
      this.notification.showWarning('Please enter a valid email address');
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

    this.authService.register(this.username, this.password, this.email).subscribe({
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

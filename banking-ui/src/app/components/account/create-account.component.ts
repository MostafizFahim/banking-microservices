import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AccountService } from '../../services/account.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-create-account',
  templateUrl: './create-account.component.html',
  styleUrls: ['./create-account.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule]
})
export class CreateAccountComponent {
  accountForm: FormGroup;
  loading = false;
  submitted = false;
  accountTypes = ['SAVINGS', 'CHECKING'];

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private notification: NotificationService,
    private router: Router
  ) {
    this.accountForm = this.fb.group({
      accountNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      accountHolderName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      balance: ['', [Validators.required, Validators.min(0)]],
      accountType: ['SAVINGS', Validators.required]
    });
  }

  // Convenience getter for easy access to form fields
  get f() { return this.accountForm.controls; }

  onSubmit(): void {
    this.submitted = true;

    // Stop if form is invalid
    if (this.accountForm.invalid) {
      // Show which fields are invalid
      Object.keys(this.accountForm.controls).forEach(key => {
        const control = this.accountForm.get(key);
        if (control?.invalid) {
          console.log(`Invalid field: ${key}`, control.errors);
        }
      });
      
      this.notification.showWarning('Please fill all required fields correctly');
      return;
    }

    this.loading = true;
    console.log('Creating account with data:', this.accountForm.value);

    this.accountService.createAccount(this.accountForm.value).subscribe({
      next: (account) => {
        console.log('Account created:', account);
        this.loading = false;
        if (account) {
          this.notification.showSuccess('Account created successfully!');
          // Navigate to the account detail page
          this.router.navigate(['/accounts', account.accountNumber]);
        }
      },
      error: (error) => {
        console.error('Create account error:', error);
        this.loading = false;
        // Error message is already shown by the service
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
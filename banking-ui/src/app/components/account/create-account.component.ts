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
      accountHolderName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      balance: ['', [Validators.required, Validators.min(0)]],
      accountType: ['SAVINGS', Validators.required]
    });
  }

  get f() { return this.accountForm.controls; }

  onSubmit(): void {
    this.submitted = true;

    if (this.accountForm.invalid) {
      this.notification.showWarning('Please fill all required fields correctly');
      return;
    }

    this.loading = true;

    this.accountService.createAccount(this.accountForm.value).subscribe({
      next: (account) => {
        this.loading = false;
        if (account) {
          this.router.navigate(['/accounts', account.accountNumber]);
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}

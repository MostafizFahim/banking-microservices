import { ChangeDetectorRef, Component, OnInit, AfterViewInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import Chart from 'chart.js/auto';
import { catchError, of, timeout } from 'rxjs';
import { AccountService } from '../../services/account.service';
import { Transaction } from '../../models/transaction.model';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss']
})
export class AnalyticsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('barChart') barChartCanvas!: ElementRef;
  @ViewChild('pieChart') pieChartCanvas!: ElementRef;
  @ViewChild('lineChart') lineChartCanvas!: ElementRef;

  private barChart?: Chart;
  private pieChart?: Chart;
  private lineChart?: Chart;

  loading = true;
  transactions: Transaction[] = [];
  analyticsError = '';

  totalDeposits = 0;
  totalWithdrawals = 0;
  totalTransactions = 0;

  constructor(
    private accountService: AccountService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    if (this.transactions.length > 0) {
      setTimeout(() => this.createCharts(), 100);
    }
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadData(): void {
    this.loading = true;
    this.analyticsError = '';
    this.accountService.getMyTransactions().pipe(
      timeout(10000),
      catchError(() => {
        return of(null);
      })
    ).subscribe({
      next: (transactions) => {
        this.scheduleViewUpdate(() => {
          this.transactions = transactions || [];
          this.analyticsError = transactions
            ? ''
            : 'Unable to load analytics data. Check that the backend is running and try again.';
          this.calculateStats();
          this.loading = false;
        });
      }
    });
  }

  calculateStats(): void {
    let deposits = 0;
    let withdrawals = 0;

    this.transactions.forEach(tx => {
      if (tx.transactionType === 'DEPOSIT') {
        deposits += tx.amount;
      } else if (tx.transactionType === 'WITHDRAWAL') {
        withdrawals += tx.amount;
      }
    });

    this.totalDeposits = deposits;
    this.totalWithdrawals = withdrawals;
    this.totalTransactions = this.transactions.length;
  }

  createCharts(): void {
    if (!this.barChartCanvas || !this.pieChartCanvas || !this.lineChartCanvas) return;
    this.createBarChart();
    this.createPieChart();
    this.createLineChart();
  }

  createBarChart(): void {
    this.barChart?.destroy();
    const monthlyDeposits = new Array(12).fill(0);
    const monthlyWithdrawals = new Array(12).fill(0);

    this.transactions.forEach(tx => {
      const date = new Date(tx.timestamp);
      const month = date.getMonth();

      if (tx.transactionType === 'DEPOSIT') {
        monthlyDeposits[month] += tx.amount;
      } else if (tx.transactionType === 'WITHDRAWAL') {
        monthlyWithdrawals[month] += tx.amount;
      }
    });

    this.barChart = new Chart(this.barChartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
        datasets: [
          { label: 'Deposits', data: monthlyDeposits, backgroundColor: '#10b981' },
          { label: 'Withdrawals', data: monthlyWithdrawals, backgroundColor: '#ef4444' }
        ]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  createPieChart(): void {
    this.pieChart?.destroy();
    let deposits = 0;
    let withdrawals = 0;

    this.transactions.forEach(tx => {
      if (tx.transactionType === 'DEPOSIT') {
        deposits += tx.amount;
      } else if (tx.transactionType === 'WITHDRAWAL') {
        withdrawals += tx.amount;
      }
    });

    this.pieChart = new Chart(this.pieChartCanvas.nativeElement, {
      type: 'pie',
      data: {
        labels: ['Deposits', 'Withdrawals'],
        datasets: [{ data: [deposits, withdrawals], backgroundColor: ['#10b981', '#ef4444'] }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  createLineChart(): void {
    this.lineChart?.destroy();
    const balanceHistory: number[] = [];
    let currentBalance = 0;

    this.transactions.forEach(tx => {
      if (tx.transactionType === 'DEPOSIT') {
        currentBalance += tx.amount;
      } else if (tx.transactionType === 'WITHDRAWAL') {
        currentBalance -= tx.amount;
      }
      balanceHistory.push(currentBalance);
    });

    const last10 = balanceHistory.slice(-10);
    const labels = last10.map((_, i) => `Tx ${i + 1}`);

    this.lineChart = new Chart(this.lineChartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Balance',
          data: last10,
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.1)',
          fill: true,
          tension: 0.4
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  private destroyCharts(): void {
    this.barChart?.destroy();
    this.pieChart?.destroy();
    this.lineChart?.destroy();
  }

  private scheduleViewUpdate(update: () => void): void {
    setTimeout(() => {
      update();
      this.cdr.detectChanges();
      setTimeout(() => this.createCharts(), 100);
    });
  }
}

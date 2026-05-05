import { Injectable } from '@angular/core';
import { Transaction } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class ExportService {

  exportToCSV(transactions: Transaction[], filename: string = 'transactions'): void {
    if (!transactions || transactions.length === 0) {
      return;
    }

    // Define CSV headers
    const headers = ['Date', 'Type', 'Description', 'Amount', 'Balance After', 'Status', 'Reference'];

    // Convert transactions to CSV rows
    const csvRows = [];
    csvRows.push(headers.join(','));

    for (const tx of transactions) {
      const row = [
        `"${new Date(tx.timestamp).toLocaleString()}"`,
        tx.transactionType,
        `"${tx.description || ''}"`,
        tx.amount,
        tx.balanceAfter,
        tx.status,
        tx.reference
      ];
      csvRows.push(row.join(','));
    }

    // Create CSV file
    const csvString = csvRows.join('\n');
    const blob = new Blob([csvString], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);

    // Download file
    const link = document.createElement('a');
    link.href = url;
    link.download = `${filename}_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();

    // Cleanup
    window.URL.revokeObjectURL(url);
  }

  exportToPDF(transactions: Transaction[], filename: string = 'transactions'): void {
    // For now, just use CSV
    this.exportToCSV(transactions, filename);
  }
}

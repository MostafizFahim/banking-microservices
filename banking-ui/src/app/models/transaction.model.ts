export interface Transaction {
  id?: string;
  accountNumber: string;
  transactionType: string;
  amount: number;
  balanceAfter: number;
  description?: string;
  status: string;
  reference: string;
  timestamp: Date;
}

export interface TransactionRequest {
  accountNumber: string;
  transactionType: string;
  amount: number;
  description?: string;
}

export interface TransactionSummary {
  totalDeposits: number;
  totalWithdrawals: number;
  netBalance: number;
  totalTransactions: number;
}
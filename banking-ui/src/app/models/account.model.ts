export interface Account {
  id?: string;
  accountNumber: string;
  accountHolderName: string;
  email: string;
  balance: number;
  accountType: string;
  status: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface AccountDTO {
  accountNumber: string;
  accountHolderName: string;
  email: string;
  balance: number;
  accountType: string;
}
import { Role } from './enums';

export interface UserSession {
  token: string;
  tokenType: string;
  username: string;
  fullName: string;
  role: Role;
  accountId: number | null;
  accountNumber: string | null;
  isMock?: boolean;
}


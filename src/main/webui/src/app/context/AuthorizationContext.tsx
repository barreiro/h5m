import type { Team } from '@client/types.gen.ts';

import { createContext } from 'react';

export interface AuthorizationContextInterface {
  isAdmin: boolean;
  isAuthenticated: boolean;
  teams: Team[];
  userId: number;
}

export const AuthorizationContext = createContext<AuthorizationContextInterface>({
  isAdmin: false,
  isAuthenticated: false,
  teams: [],
  userId: 0,
});

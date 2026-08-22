import { AuthorizationContext } from '@app/context/AuthorizationContext.tsx';
import { useContext } from 'react';

export const useTeams = () => useContext(AuthorizationContext).teams;

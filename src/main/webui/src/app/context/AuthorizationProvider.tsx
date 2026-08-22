import { AuthorizationContext } from '@app/context/AuthorizationContext.tsx';
import { currentUserOptions, userTeamsOptions } from '@client/@tanstack/react-query.gen.ts';
import { client } from '@client/client.gen.ts';
import { useQuery } from '@tanstack/react-query';
import { ReactNode, useEffect } from 'react';
import { useAuth } from 'react-oidc-context';

export const AuthorizationProvider = ({ children }: { children: ReactNode }) => {
  const auth = useAuth();

  const token = auth.user?.access_token;
  const isAuthenticated = !!(auth.isAuthenticated && token);

  useEffect(() => {
    client.setConfig({ auth: isAuthenticated ? token : undefined });
  }, [token, isAuthenticated]);

  const { data: currentUser } = useQuery({
    ...currentUserOptions(),
    enabled: isAuthenticated,
    staleTime: 60 * 1000,
  });

  const { data: teams } = useQuery({
    ...userTeamsOptions(),
    enabled: isAuthenticated,
    staleTime: 60 * 1000,
  });

  return (
    <AuthorizationContext.Provider
      value={{
        isAdmin: isAuthenticated && currentUser?.role === 'ADMIN',
        isAuthenticated,
        teams: teams ?? [],
        userId: currentUser?.id ?? 0,
      }}
    >
      {children}
    </AuthorizationContext.Provider>
  );
};

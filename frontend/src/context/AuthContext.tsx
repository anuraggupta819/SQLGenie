import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { tokenStorage } from '../api/client';

interface JwtPayload {
  sub: string;
  email: string;
  role: string;
  exp: number;
}

function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

interface AuthUser {
  email: string;
  role: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function userFromAccessToken(accessToken: string): AuthUser | null {
  const payload = decodeJwtPayload(accessToken);
  return payload ? { email: payload.email, role: payload.role } : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const token = tokenStorage.getAccessToken();
    return token ? userFromAccessToken(token) : null;
  });

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password);
    tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(userFromAccessToken(response.accessToken));
  }, []);

  const register = useCallback(async (email: string, password: string, fullName: string) => {
    const response = await authApi.register(email, password, fullName);
    tokenStorage.setTokens(response.accessToken, response.refreshToken);
    setUser(userFromAccessToken(response.accessToken));
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken);
      } catch {
        // Logout is best-effort client-side regardless of whether the
        // server call succeeds - the tokens are being discarded either way.
      }
    }
    tokenStorage.clear();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, isAuthenticated: user !== null, login, register, logout }),
    [user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

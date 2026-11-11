import React, { createContext, useContext, useState, useEffect } from 'react';
import type { AuthResponse, UserDto, Role } from '../types/api';
import { authApi } from '../services/api';

interface AuthContextType {
  token: string | null;
  user: UserDto | AuthResponse | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(localStorage.getItem('aquascope_token'));
  const [user, setUser] = useState<AuthResponse | UserDto | null>(() => {
    const saved = localStorage.getItem('aquascope_user');
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    if (token && !user) {
      authApi.getMe()
        .then((res) => {
          setUser(res.data);
          localStorage.setItem('aquascope_user', JSON.stringify(res.data));
        })
        .catch(() => {
          logout();
        });
    }
  }, [token]);

  const login = async (username: string, password: string) => {
    const res = await authApi.login(username, password);
    const authData = res.data;
    setToken(authData.token);
    setUser(authData);
    localStorage.setItem('aquascope_token', authData.token);
    localStorage.setItem('aquascope_user', JSON.stringify(authData));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('aquascope_token');
    localStorage.removeItem('aquascope_user');
  };

  const hasRole = (roles: Role[]): boolean => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  return (
    <AuthContext.Provider value={{ token, user, isAuthenticated: !!token, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
let API_BASE_URL = rawApiBaseUrl.trim();

if (!API_BASE_URL) {
  // Default fallback for development or local relative requests
  API_BASE_URL = '/api';
} else {
  // Remove trailing slash if any
  if (API_BASE_URL.endsWith('/')) {
    API_BASE_URL = API_BASE_URL.slice(0, -1);
  }
  // Ensure that if it is an absolute URL (starts with http) it ends with '/api' to match Spring Boot routes
  if (API_BASE_URL.startsWith('http') && !API_BASE_URL.endsWith('/api')) {
    API_BASE_URL = `${API_BASE_URL}/api`;
  }
}

console.log('Resolved API_BASE_URL to:', API_BASE_URL);

let currentActorId = localStorage.getItem('actorId') || 'emp-4';

export const setActorId = (actorId: string) => {
  currentActorId = actorId;
  localStorage.setItem('actorId', actorId);
};

export const getActorId = () => currentActorId;

export async function fetchApi(endpoint: string, options: RequestInit = {}) {
  const params = new URLSearchParams();
  params.append('actorId', currentActorId);
  params.append('year', '2026');

  const separator = endpoint.includes('?') ? '&' : '?';
  const url = `${API_BASE_URL}${endpoint}${separator}${params.toString()}`;

  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && options.method !== 'GET') {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url.toString(), {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type');
  let data;
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = data?.message || data?.detail || data?.error || (typeof data === 'string' ? data : `HTTP ${response.status} Error`);
    throw new Error(errorMsg);
  }

  return data;
}

// API methods
export const api = {
  // Session / Users
  login: async (username: string, password?: string) => {
    // Explicitly don't use fetchApi because we don't want to send default actorId
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || data.detail || data.error || `HTTP ${response.status} Login failed`);
    return data;
  },
  getUsers: () => fetchApi('/users'),
  
  // Goals (Employee)
  getMyGoalSheet: () => fetchApi('/goal-sheets/me'),
  addGoal: (goal: any) => fetchApi('/goal-sheets/me/goals', {
    method: 'POST',
    body: JSON.stringify(goal)
  }),
  deleteGoal: (goalId: number) => fetchApi(`/goal-sheets/me/goals/${goalId}`, {
    method: 'DELETE'
  }),
  submitGoalSheet: () => fetchApi('/goal-sheets/me/submit', {
    method: 'POST'
  }),
  
  // Team (Manager)
  getTeam: () => fetchApi('/manager/team'),
  approveGoalSheet: (employeeId: string, comment: string) => fetchApi(`/manager/goal-sheets/${employeeId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ comment })
  }),
  rejectGoalSheet: (employeeId: string, comment: string) => fetchApi(`/manager/goal-sheets/${employeeId}/return`, {
    method: 'POST',
    body: JSON.stringify({ comment })
  }),
  pushSharedGoal: (payload: any) => fetchApi('/manager/shared-goals', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  getEmployeeGoalSheet: (employeeId: string) => fetchApi(`/manager/goal-sheets/${employeeId}`),
  addManagerComment: (employeeId: string, period: string, comment: string) => fetchApi(`/manager/check-ins/${employeeId}/${period}/comment`, {
    method: 'POST',
    body: JSON.stringify({ comment })
  }),
  updateCheckIn: (period: string, goalId: number, payload: any) => fetchApi(`/check-ins/me/${period}/goals/${goalId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  getCompletionDashboard: (period: string) => fetchApi(`/dashboard/completion?period=${period}`),
  // Admin
  upsertUser: (payload: any) => fetchApi('/admin/users', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  unlockSheet: (employeeId: string, year: number, reason: string) => fetchApi(`/admin/goal-sheets/${employeeId}/${year}/unlock`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  }),
  updateCycle: (year: number, payload: any) => fetchApi(`/admin/cycles/${year}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  getAuditLogs: (employeeId?: string, year?: number) => {
    let url = '/audit-logs';
    const params = new URLSearchParams();
    if (employeeId) params.append('employeeId', employeeId);
    if (year) params.append('year', year.toString());
    const query = params.toString();
    return fetchApi(query ? `${url}?${query}` : url);
  }
};

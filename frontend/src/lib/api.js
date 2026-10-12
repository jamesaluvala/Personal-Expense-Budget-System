// All requests are same-origin. A Vite or Vercel proxy forwards /api to Spring Boot.
// Authentication lives in an HttpOnly cookie; no tokens or passwords are saved in localStorage.
export class ApiError extends Error {
  constructor(message, status, fields = {}) { super(message); this.status = status; this.fields = fields; }
}
async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api${path}`, { ...options, credentials: 'same-origin', cache: 'no-store' });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('Cannot reach the backend. Check that the Spring Boot service is running and /api is connected.', 0);
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    if (response.status === 401 && !path.startsWith('/auth/')) window.dispatchEvent(new Event('session-expired'));
    throw new ApiError(body.message || `Request failed (${response.status}).`, response.status, body.errors);
  }
  return response;
}
export async function api(path, { method = 'GET', data, form = false, signal } = {}) {
  const headers = { Accept: 'application/json' };
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    // Get a fresh token before every mutation, including login/logout and registration.
    const csrf = await (await request('/auth/csrf', { signal })).json();
    headers[csrf.headerName] = csrf.token;
  }
  if (data !== undefined) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json';
  const response = await request(path, {
    method, headers, signal,
    body: data === undefined ? undefined : form ? new URLSearchParams(data).toString() : JSON.stringify(data),
  });
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text); }
  catch { throw new ApiError('The API returned a web page instead of JSON. Check the /api proxy configuration.', 502); }
}
export async function exportCsv(month) {
  const response = await request(`/reports/export.csv?month=${encodeURIComponent(month)}`);
  if (!response.headers.get('content-type')?.includes('text/csv')) throw new ApiError('Report endpoint did not return a CSV file.', 502);
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url; link.download = `financial-report-${month}.csv`;
  document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

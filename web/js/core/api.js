// ========================================
// API Client & Token Management
// ========================================

import { API_URL } from './config.js';
import { currentLang } from './state.js';

// Active request counter for global loader
let __activeRequests = 0;

// Show/hide loader functions (will be set from ui-utils.js)
let showLoader = null;
let hideLoader = null;
let logout = null;

export function setLoaderFunctions(show, hide) {
    showLoader = show;
    hideLoader = hide;
}

export function setLogoutFunction(logoutFn) {
    logout = logoutFn;
}

/**
 * Main API call wrapper with authentication, error handling, and token refresh
 */
export async function apiCall(endpoint, method = 'GET', body = null, requireAuth = false) {
    const headers = {
        'Content-Type': 'application/json'
    };

    if (requireAuth) {
        const token = localStorage.getItem('triobank_token');
        if (token) headers['Authorization'] = `Bearer ${token}`;
    }

    const config = { method, headers, credentials: 'include' };
    if (body) config.body = JSON.stringify(body);

    __activeRequests += 1;
    if (__activeRequests === 1 && showLoader) showLoader();

    try {
        let response = await fetch(`${API_URL}${endpoint}`, config);

        // read body as text then try to parse JSON (safer for endpoints that return empty or text)
        let text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            // not JSON, keep raw text
            data = text;
        }

        // handle unauthorized centrally only for requests that require auth
        if (response.status === 401 && requireAuth) {
            // Try token refresh once
            try {
                const refreshed = await tryRefreshToken();
                if (refreshed) {
                    // retry original request with new token
                    const token = localStorage.getItem('triobank_token');
                    if (token) {
                        config.headers['Authorization'] = `Bearer ${token}`;
                    }
                    // Retry request with new token (body zaten config'de var)
                    response = await fetch(`${API_URL}${endpoint}`, config);
                    text = await response.text();
                    try {
                        data = text ? JSON.parse(text) : {};
                    } catch (e) {
                        data = text;
                    }
                    // Eğer retry sonrası hala 401 ise, refresh token da geçersiz demektir
                    if (response.status === 401) {
                        if (logout) logout();
                        throw new Error(currentLang === 'tr' ? 'Oturum süresi doldu, lütfen tekrar giriş yapın.' : 'Session expired, please login again.');
                    }
                } else {
                    // refresh failed -> logout
                    if (logout) logout();
                    throw new Error(currentLang === 'tr' ? 'Oturum süresi doldu, lütfen tekrar giriş yapın.' : 'Session expired, please login again.');
                }
            } catch (e) {
                // Eğer zaten logout çağrıldıysa, sadece error'ı fırlat
                if (e.message && e.message.includes('Oturum süresi doldu') || e.message.includes('Session expired')) {
                    throw e;
                }
                // refresh failed -> logout
                if (logout) logout();
                throw new Error(currentLang === 'tr' ? 'Oturum süresi doldu, lütfen tekrar giriş yapın.' : 'Session expired, please login again.');
            }
        }

        if (!response.ok) {
            // Rate limit hatası için özel mesaj
            if (response.status === 429) {
                const retryAfter = response.headers.get('Retry-After') || '60';
                const message = currentLang === 'tr'
                    ? `Çok fazla istek yapıldı. Lütfen ${retryAfter} saniye sonra tekrar deneyin.`
                    : `Too many requests. Please try again after ${retryAfter} seconds.`;
                const error = new Error(message);
                error.status = response.status;
                error.response = data;
                throw error;
            }
            const message = (data && (data.message || data.error)) || (typeof data === 'string' && data) || (currentLang === 'tr' ? 'İşlem başarısız' : 'Request failed');
            const error = new Error(message);
            error.status = response.status;
            error.response = data;
            throw error;
        }

        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    } finally {
        __activeRequests -= 1;
        if (__activeRequests <= 0) {
            __activeRequests = 0;
            if (hideLoader) hideLoader();
        }
    }
}

/**
 * Attempt to refresh access token using refresh cookie. Returns true if refreshed.
 */
export async function tryRefreshToken() {
    try {
        const res = await fetch(`${API_URL}/auth/refresh`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!res.ok) {
            // 401 = refresh token geçersiz veya expire olmuş
            if (res.status === 401) {
                console.log('Refresh token expired or invalid');
                // LocalStorage'ı temizle
                localStorage.removeItem('triobank_token');
            }
            return false;
        }

        const text = await res.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            data = text;
        }

        const newToken = data && (data.access_token || data.accessToken || data.token);
        if (newToken) {
            localStorage.setItem('triobank_token', newToken);
            console.log('Access token refreshed successfully');
            return true;
        }
        return false;
    } catch (err) {
        console.error('Refresh token failed:', err);
        // Hata durumunda da localStorage'ı temizle (güvenlik için)
        localStorage.removeItem('triobank_token');
        return false;
    }
}

/**
 * Response normalizers (tolerate different naming conventions from API)
 */
export function extractSessionId(res) {
    if (!res) return null;
    return res['session-id'] || res.sessionId || res.session_id || res.session || res.sessionid || null;
}

export function extractAccessToken(res) {
    if (!res) return null;
    return res.access_token || res.accessToken || res.token || res.jwt || null;
}

/**
 * JWT Helper Functions
 */

// Decode JWT payload without verifying (for client-side expiry check)
export function parseJwt(token) {
    try {
        const parts = token.split('.');
        if (parts.length < 2) return null;
        const payload = parts[1];
        const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decodeURIComponent(escape(json)));
    } catch (e) {
        return null;
    }
}

export function isTokenValid(token) {
    if (!token) return false;
    const p = parseJwt(token);
    if (!p) return false;
    if (p.exp) {
        // exp may be in seconds
        const exp = Number(p.exp);
        const now = Math.floor(Date.now() / 1000);
        return exp > now + 2; // 2s grace
    }
    return true;
}

/**
 * Helper: Safe query string builder
 */
export function buildQuery(params) {
    const esc = encodeURIComponent;
    return Object.keys(params).filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '').map(k => `${esc(k)}=${esc(params[k])}`).join('&');
}

/**
 * Helper: Sleep function
 */
export function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

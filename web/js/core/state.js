// ========================================
// Global State Management
// ========================================

// Current language
export let currentLang = 'tr';

export function setCurrentLang(lang) {
    currentLang = lang;
}

// Current user (from auth service)
export let currentUser = null;

export function setCurrentUser(user) {
    currentUser = user;
}

// Current session ID (for verification flows)
export let currentSessionId = null;

export function setCurrentSessionId(sessionId) {
    currentSessionId = sessionId;
}

// Auth flow type
export let authFlowType = null; // 'login' | 'register' | 'password-reset'

export function setAuthFlowType(type) {
    authFlowType = type;
}

// Current client (from client service)
export let currentClient = null;

export function setCurrentClient(client) {
    currentClient = client;
}

// Verification timer
export let verificationTimer = null;

export function setVerificationTimer(timer) {
    verificationTimer = timer;
}

// Current verification code (for password reset)
export let currentVerificationCode = null;

export function setCurrentVerificationCode(code) {
    currentVerificationCode = code;
}

// Card type filter
export let currentCardTypeFilter = 'all'; // 'all', 'DEBIT', 'VIRTUAL'

export function setCurrentCardTypeFilter(filter) {
    currentCardTypeFilter = filter;
}

// Global Accounts State
export const accountsState = {
    accounts: [],
    lastUpdated: null,
    isLoading: false
};

// Legacy balance (kept for backward compatibility)
export let userBalance = 0.00;

export function setUserBalance(balance) {
    userBalance = balance;
}

// Legacy transactions (kept for backward compatibility)
export let transactions = [];

export function setTransactions(txs) {
    transactions = txs;
}

// Expose accountsState to window for backward compatibility
if (typeof window !== 'undefined') {
    window.accountsState = accountsState;
}

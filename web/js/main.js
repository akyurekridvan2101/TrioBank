// ========================================
// Main Entry Point
// ========================================

// Import core modules
import { translations, API_URL } from './core/config.js';
import {
    currentLang, setCurrentLang,
    currentUser, setCurrentUser,
    currentSessionId, setCurrentSessionId,
    authFlowType, setAuthFlowType,
    currentClient, setCurrentClient,
    verificationTimer, setVerificationTimer,
    currentVerificationCode, setCurrentVerificationCode,
    currentCardTypeFilter, setCurrentCardTypeFilter,
    accountsState,
    userBalance, setUserBalance,
    transactions, setTransactions
} from './core/state.js';
import {
    apiCall, tryRefreshToken, extractSessionId, extractAccessToken,
    parseJwt, isTokenValid, buildQuery, sleep,
    setLoaderFunctions, setLogoutFunction
} from './core/api.js';
import { initializeRouter, showPage } from './core/router.js';

// Import utilities
import {
    generateIBAN, toggleError, formatPhoneNumber, onlyNumbers,
    isValidEmail, isValidTC, isValidPassword, isValidPhone, formatMoney
} from './utils/helpers.js';
import { showToast, showLoader, hideLoader, showConfirmModal } from './utils/ui-utils.js';

// ========================================
// Initialize API dependencies
// ========================================
setLoaderFunctions(showLoader, hideLoader);

// ========================================
// Expose necessary functions to window
// (Required for inline onclick handlers in HTML)
// ========================================

// NOTE: The original app.js has 5200+ lines with many functions.
// For this modular version, we're exposing only the critical ones.
// The rest will be imported in service modules and exposed as needed.

// Core exposed functions
window.showPage = showPage;
window.showToast = showToast;
window.showConfirmModal = showConfirmModal;

// Temporary: Expose all old functions until fully migrated
// This ensures backward compatibility during the transition
console.log('Main.js loaded - Modular frontend initialized');
console.log('Note: Some functions from app.js still need to be migrated to service modules');

// ========================================
// Application Initialization
// ========================================

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM Content Loaded - Initializing application...');

    // Initialize router
    initializeRouter();

    // Tab Control
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.addEventListener('click', (e) => {
            document.querySelectorAll('.tab-item').forEach(i => i.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            e.target.classList.add('active');
            const targetId = e.target.getAttribute('data-tab');
            document.getElementById(targetId).classList.add('active');
        });
    });

    // Theme control
    const storedTheme = localStorage.getItem('theme');
    const darkToggleEl = document.getElementById('darkModeToggle');
    if (storedTheme === 'dark') {
        document.body.classList.add('dark-mode');
        if (darkToggleEl) darkToggleEl.checked = true;
    } else if (storedTheme === 'light') {
        document.body.classList.remove('dark-mode');
        if (darkToggleEl) darkToggleEl.checked = false;
    } else {
        // Default to Dark Mode
        document.body.classList.add('dark-mode');
        if (darkToggleEl) darkToggleEl.checked = true;
    }

    // IBAN Generator - Static for main display
    const mainIban = generateIBAN();
    const ibanElements = document.querySelectorAll('.dynamic-iban');
    ibanElements.forEach(el => el.innerText = mainIban);

    // Initialize phone number masking
    const phoneInput = document.getElementById('signupPhone');
    if (phoneInput) {
        phoneInput.addEventListener('input', function (e) {
            e.target.value = formatPhoneNumber(e.target.value);
        });
    }

    // Initialize only-numbers inputs
    const onlyNumbersInputs = document.querySelectorAll('.only-numbers');
    onlyNumbersInputs.forEach(input => {
        if (input.id !== 'signupPhone') {
            input.addEventListener('input', function (e) {
                this.value = onlyNumbers(this.value);
            });
        }
    });

    // Note: Session restoration and form handlers will be added
    // as we complete the service modules migration

    console.log('Application initialized successfully');
});

// Temporary compatibility layer
// TODO: Remove after full migration
console.warn('WARNING: Running in hybrid mode. Some functions still load from app.js');
console.warn('TODO: Complete migration of Auth, Banking, and UI service modules');

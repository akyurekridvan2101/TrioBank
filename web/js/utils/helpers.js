// ========================================
// General Helper Functions
// ========================================

/**
 * IBAN Generator (for display purposes only)
 */
export function generateIBAN() {
    let randomTwo = Math.floor(Math.random() * 90 + 10);
    let randomRest = "";
    for (let i = 0; i < 17; i++) {
        randomRest += Math.floor(Math.random() * 10);
    }
    return `TR${randomTwo} 7210 6${randomRest.substring(0, 4)} ${randomRest.substring(4, 8)} ${randomRest.substring(8, 12)} ${randomRest.substring(12)}`;
}

/**
 * Toggle error message visibility
 */
export function toggleError(elementId, show) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = show ? 'block' : 'none';
    return !show;
}

/**
 * Format phone number (3 3 2 2 format)
 */
export function formatPhoneNumber(value) {
    let x = value.replace(/\D/g, '').match(/(\d{0,3})(\d{0,3})(\d{0,2})(\d{0,2})/);
    return !x[2] ? x[1] : x[1] + ' ' + x[2] + (x[3] ? ' ' + x[3] : '') + (x[4] ? ' ' + x[4] : '');
}

/**
 * Remove non-numeric characters
 */
export function onlyNumbers(value) {
    return value.replace(/[^0-9]/g, '');
}

/**
 * Email validation
 */
export function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * TC Kimlik validation
 */
export function isValidTC(tc) {
    return tc && tc.length === 11;
}

/**
 * Password validation
 */
export function isValidPassword(password) {
    return password && password.length >= 8;
}

/**
 * Phone validation
 */
export function isValidPhone(phone) {
    const cleaned = phone.replace(/\D/g, '');
    return cleaned.length === 10;
}

/**
 * Format money for display
 */
export function formatMoney(value, currency = 'TRY') {
    if (typeof value === 'number') {
        return value.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ' + currency;
    }
    return value + ' ' + currency;
}

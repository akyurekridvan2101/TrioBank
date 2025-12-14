// ========================================
// Page Router & Navigation
// ========================================

import { verificationTimer, setVerificationTimer, currentLang } from './state.js';

// All page elements (set during initialization)
let pages = [];

export function initializeRouter() {
    pages = document.querySelectorAll('.page');
}

/**
 * Navigate to a specific page by ID
 */
export function showPage(id) {
    // Sayfa değiştiğinde timer varsa temizle
    if (verificationTimer) {
        clearInterval(verificationTimer);
        setVerificationTimer(null);
    }

    pages.forEach(page => page.classList.remove('active'));
    const target = document.getElementById(id);
    if (target) {
        target.classList.add('active');
        window.scrollTo(0, 0);
    }

    // Dashboard sayfaları listesi
    const dashboardPages = ['dashboard', 'profile', 'accounts', 'cards', 'settings', 'password-page', 'delete-account-page', 'operations', 'markets-page', 'help-page', 'products', 'transfer-selection-page'];

    // Auth (Giriş/Kayıt/Doğrulama/Şifre Sıfırlama) sayfaları
    const authPages = ['login', 'signup', 'verify-email', 'forgot-password-step1', 'forgot-password-verify', 'forgot-password-reset'];

    const slider = document.getElementById('bgSlider');
    const overlay = document.getElementById('bgOverlay');
    const header = document.getElementById('mainHeader');

    if (dashboardPages.includes(id)) {
        if (slider) slider.style.display = 'none';
        if (overlay) overlay.style.display = 'none';
        if (header) header.style.display = 'none';

        // Alt navigasyon aktiflik durumu - Tüm bottom-nav'lardaki nav-item'ları güncelle
        document.querySelectorAll('.bottom-nav .nav-item').forEach(item => {
            item.classList.remove('active');

            // data-target attribute'una göre kontrol et
            if (item.dataset.target === id) {
                item.classList.add('active');
            }
            // Eğer data-target yoksa, onclick içindeki sayfa ID'sini kontrol et
            else if (!item.dataset.target && item.onclick) {
                const onclickStr = item.getAttribute('onclick') || '';
                if (onclickStr.includes(`showPage('${id}')`) || onclickStr.includes(`showPage("${id}")`)) {
                    item.classList.add('active');
                }
            }
        });
    } else {
        // Slider ve Header'ı auth sayfalarında da göster
        if (slider) slider.style.display = 'block';
        if (overlay) overlay.style.display = 'block';
        if (header) header.style.display = 'block';
    }

    // Return the page ID for use by event handlers
    return id;
}

// Expose to window for inline onclick handlers in HTML
if (typeof window !== 'undefined') {
    window.showPage = showPage;
}

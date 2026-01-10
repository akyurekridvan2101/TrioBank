
// --- DİL VERİLERİ ---
const translations = {
    tr: {
        home: "Ana Sayfa", staff: "Kadromuz", about: "Hakkımızda", login: "Giriş Yap", signup: "Üye Ol",
        heroTitle: "Güvenin Adresi Trio Bank", heroDesc: "Geleceğin bankacılığı parmaklarınızın ucunda.",
        internetBranch: "İnternet Şubesi", applyNow: "Hemen Başvur", expertStaff: "Uzman Kadromuz",
        contact: "İletişime Geç", goBack: "Geri Dön", mission: "Hakkımızda & Hedeflerimiz",
        missionText: "Trio Bank olarak temel amacımız...",
        tcLabel: "T.C. Kimlik No", passLabel: "Şifre", loginBtn: "Giriş Yap", joinUs: "Aramıza Katıl",
        name: "Ad", surname: "Soyad", phone: "Telefon", signupBtn: "Kayıt Ol", welcome: "İyi Günler",
        accounts: "Hesaplarım", cards: "Kartlarım", branch: "MERKEZ ŞUBE", vadesiz: "Vadesiz TL",
        availBalance: "Kullanılabilir Bakiye", sendMoney: "Para Gönder", movements: "Hareketler",
        noCard: "Henüz bir kartınız bulunmamaktadır.", operations: "İşlemler", assets: "Varlıklarım",
        transfer: "Transfer", navHome: "Ana Sayfa", navAcc: "Hesaplar", navTrans: "Transfer", navMenu: "Menü",
        moneyTransfer: "Para Transferi", receiverName: "Alıcı Ad Soyad", amount: "Tutar (TL)", send: "Gönder",
        cancel: "Vazgeç", accDetails: "Hesap Detayları", recentMovements: "Son Hareketler", myProfile: "Profilim",
        nameSurname: "Ad Soyad", settings: "Ayarlar", appSettings: "Uygulama Ayarları", darkMode: "Karanlık Mod",
        language: "Dil (Language)", deleteAccount: "Hesabı Sil", deleteWarning: "Bu işlem geri alınamaz. Devam etmek için bilgilerinizi doğrulayın.",
        deleteBtn: "Hesabımı Kalıcı Olarak Sil", profileSettings: "PROFİL VE AYARLAR", bankingOps: "BANKACILIK İŞLEMLERİ",
        transfers: "Para Transferleri", applications: "BAŞVURULAR", loanApp: "Kredi Başvurusu", session: "OTURUM", logout: "Güvenli Çıkış",
        payments: "Ödemeler", paymentsSoon: "Fatura ve Kurum ödemeleri sistemi bakım aşamasındadır.", changePass: "Şifre Değiştir", update: "Güncelle",
        markets: "Piyasalar", gold: "Altın (Gr)"
    },
    en: {
        home: "Home", staff: "Our Staff", about: "About Us", login: "Login", signup: "Sign Up",
        heroTitle: "Trusted Trio Bank", heroDesc: "Future banking at your fingertips.",
        internetBranch: "Internet Branch", applyNow: "Apply Now", expertStaff: "Our Expert Staff",
        contact: "Contact", goBack: "Go Back", mission: "About Us & Goals",
        missionText: "As Trio Bank...",
        tcLabel: "ID Number", passLabel: "Password", loginBtn: "Login", joinUs: "Join Us",
        name: "Name", surname: "Surname", phone: "Phone", signupBtn: "Register", welcome: "Good Day",
        accounts: "Accounts", cards: "Cards", branch: "CENTRAL BRANCH", vadesiz: "Checking Account",
        availBalance: "Available Balance", sendMoney: "Send Money", movements: "Transactions",
        noCard: "You do not have a card yet.", operations: "Operations", assets: "My Assets",
        transfer: "Transfer", navHome: "Home", navAcc: "Accounts", navTrans: "Transfer", navMenu: "Menu",
        moneyTransfer: "Money Transfer", receiverName: "Receiver Name", amount: "Amount (TL)", send: "Send",
        cancel: "Cancel", accDetails: "Account Details", recentMovements: "Recent Transactions", myProfile: "My Profile",
        nameSurname: "Name Surname", settings: "Settings", appSettings: "App Settings", darkMode: "Dark Mode",
        language: "Language", deleteAccount: "Delete Account", deleteWarning: "This action cannot be undone. Verify your details to continue.",
        deleteBtn: "Permanently Delete Account", profileSettings: "PROFILE & SETTINGS", bankingOps: "BANKING OPERATIONS",
        transfers: "Money Transfers", applications: "APPLICATIONS", loanApp: "Loan Application", session: "SESSION", logout: "Secure Logout",
        payments: "Payments", paymentsSoon: "Bill and Institution payment system is under maintenance.", changePass: "Change Password", update: "Update",
        markets: "Markets", gold: "Gold (Gr)"
    }
};

let currentLang = 'tr';
let currentCardTypeFilter = 'all'; // 'all', 'DEBIT', 'VIRTUAL'

// Global Accounts State - Tek kaynak
window.accountsState = {
    accounts: [], // Account array with balance
    lastUpdated: null, // Timestamp of last backend fetch
    isLoading: false // Loading flag to prevent concurrent loads
};

// API base URL - same-origin architecture (relative path)
const API_URL = '/api';

// --- STATE ---
let currentUser = null;
let currentSessionId = null;
let authFlowType = null; // 'login' | 'register' | 'password-reset'
let currentClient = null;
let verificationTimer = null;
let currentVerificationCode = null; // For password reset flow

// --- TOAST NOTIFICATION HELPER ---
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let iconClass = 'fas fa-info-circle';
    if (type === 'success') iconClass = 'fas fa-check-circle';
    if (type === 'error') iconClass = 'fas fa-times-circle';

    toast.innerHTML = `<i class="${iconClass}" style="width:20px;text-align:center;"></i><span>${message}</span>`;

    container.appendChild(toast);

    // 4 saniye sonra yavaşça kaybol ve DOM'dan kaldır
    setTimeout(() => {
        toast.style.animation = 'toastFadeOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// --- GLOBAL LOADER (shows while API requests are in-flight) ---
let __activeRequests = 0;
function showLoader() {
    let el = document.getElementById('global-loader');
    if (!el) {
        el = document.createElement('div');
        el.id = 'global-loader';
        el.innerHTML = '<div class="loader-spinner"></div>';
        document.body.appendChild(el);
    }
    el.style.display = 'flex';
}
function hideLoader() {
    const el = document.getElementById('global-loader');
    if (el) el.style.display = 'none';
}

// --- DUMMY DATA ---
let userBalance = 0.00;
let transactions = [];

// --- API HELPER ---
async function apiCall(endpoint, method = 'GET', body = null, requireAuth = false) {
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
    if (__activeRequests === 1) showLoader();

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
                        logout();
                        throw new Error(currentLang === 'tr' ? 'Oturum süresi doldu, lütfen tekrar giriş yapın.' : 'Session expired, please login again.');
                    }
                } else {
                    // refresh failed -> logout
                    logout();
                    throw new Error(currentLang === 'tr' ? 'Oturum süresi doldu, lütfen tekrar giriş yapın.' : 'Session expired, please login again.');
                }
            } catch (e) {
                // Eğer zaten logout çağrıldıysa, sadece error'ı fırlat
                if (e.message && e.message.includes('Oturum süresi doldu') || e.message.includes('Session expired')) {
                    throw e;
                }
                // refresh failed -> logout
                logout();
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
        // showToast'u burada çağırmayalım, çağıran fonksiyon daha detaylı mesaj gösterebilir
        // showToast(error.message || String(error), 'error');
        throw error;
    } finally {
        __activeRequests -= 1;
        if (__activeRequests <= 0) {
            __activeRequests = 0;
            hideLoader();
        }
    }
}

// Attempt to refresh access token using refresh cookie. Returns true if refreshed.
async function tryRefreshToken() {
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

// --- Response normalizers (tolerate different naming conventions from API) ---
function extractSessionId(res) {
    if (!res) return null;
    return res['session-id'] || res.sessionId || res.session_id || res.session || res.sessionid || null;
}

function extractAccessToken(res) {
    if (!res) return null;
    return res.access_token || res.accessToken || res.token || res.jwt || null;
}

// --- YARDIMCI FONKSİYONLAR ---

// Decode JWT payload without verifying (for client-side expiry check)
function parseJwt(token) {
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

function isTokenValid(token) {
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

// small sleep helper
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}


// IBAN Üretici (Sadece görünmeyen yerlerde kullanılır, ana ekran statik yapıldı)
function generateIBAN() {
    let randomTwo = Math.floor(Math.random() * 90 + 10);
    let randomRest = "";
    for (let i = 0; i < 17; i++) {
        randomRest += Math.floor(Math.random() * 10);
    }
    return `TR${randomTwo} 7210 6${randomRest.substring(0, 4)} ${randomRest.substring(4, 8)} ${randomRest.substring(8, 12)} ${randomRest.substring(12)}`;
}

function toggleError(elementId, show) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = show ? 'block' : 'none';
    return !show;
}

// TC Kimlik ve Telefon Maskeleme (3 3 2 2 FORMATI)
const phoneInput = document.getElementById('signupPhone');
if (phoneInput) {
    phoneInput.addEventListener('input', function (e) {
        let x = e.target.value.replace(/\D/g, '').match(/(\d{0,3})(\d{0,3})(\d{0,2})(\d{0,2})/);
        e.target.value = !x[2] ? x[1] : x[1] + ' ' + x[2] + (x[3] ? ' ' + x[3] : '') + (x[4] ? ' ' + x[4] : '');
    });
}

const onlyNumbersInputs = document.querySelectorAll('.only-numbers');
onlyNumbersInputs.forEach(input => {
    if (input.id !== 'signupPhone') { // Telefon inputunu yukarıda özel formatlıyoruz
        input.addEventListener('input', function (e) {
            this.value = this.value.replace(/[^0-9]/g, '');
        });
    }
});

// --- SAYFA YÖNETİMİ ---
const pages = document.querySelectorAll('.page');

document.addEventListener('DOMContentLoaded', () => {
    // Tab Kontrolü
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.addEventListener('click', (e) => {
            document.querySelectorAll('.tab-item').forEach(i => i.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            e.target.classList.add('active');
            const targetId = e.target.getAttribute('data-tab');
            document.getElementById(targetId).classList.add('active');
        });
    });

    // Theme kontrolü
    const storedTheme = localStorage.getItem('theme');
    const darkToggleEl = document.getElementById('darkModeToggle');
    if (storedTheme === 'dark') {
        document.body.classList.add('dark-mode');
        if (darkToggleEl) darkToggleEl.checked = true;
    } else if (storedTheme === 'light') {
        document.body.classList.remove('dark-mode');
        if (darkToggleEl) darkToggleEl.checked = false;
    } else {
        // İlk açılışta Dark Mode olsun
        document.body.classList.add('dark-mode');
        if (darkToggleEl) darkToggleEl.checked = true;
    }

    // IBAN Oluşturma - Sadece detay sayfaları için, ana ekran statik
    const mainIban = generateIBAN();

    // IBAN'ları yerleştir (Sadece dynamic-iban class'ı olanlara)
    const ibanElements = document.querySelectorAll('.dynamic-iban');
    ibanElements.forEach(el => el.innerText = mainIban);

    // İlk bakiyeyi güncelle
    updateBalanceUI();

    // Try to restore session silently - check both localStorage token and refresh token cookie
    (async () => {
            const ok = await loadUserProfile();
        if (ok) {
            showPage('dashboard');
        } else {
            // Token yok ve refresh token da geçersizse, kullanıcı çıkış yapmış demektir
            // LocalStorage'ı temizle (güvenlik için)
            localStorage.removeItem('triobank_token');
            currentUser = null;
            showPage('home');
        }
        // Ensure header language button reflects current language
        const hLang = document.getElementById('headerLangBtn');
        if (hLang) hLang.innerText = currentLang.toUpperCase();
        // Ensure header theme icon/state reflects stored theme
        const hTheme = document.getElementById('headerThemeBtn');
        if (hTheme) {
            if (document.body.classList.contains('dark-mode')) hTheme.innerHTML = '<i class="fas fa-moon"></i>';
            else hTheme.innerHTML = '<i class="fas fa-sun"></i>';
        }
    })();
});

function showPage(id) {
    // Sayfa değiştiğinde timer varsa temizle
    if (verificationTimer) {
        clearInterval(verificationTimer);
        verificationTimer = null;
    }

    pages.forEach(page => page.classList.remove('active'));
    const target = document.getElementById(id);
    if (target) {
        target.classList.add('active');
        window.scrollTo(0, 0);
    }

    // Dashboard sayfaları listesi
    const dashboardPages = ['dashboard', 'profile', 'accounts', 'cards', 'settings', 'password-page', 'delete-account-page', 'operations', 'markets-page', 'help-page', 'products'];

    // Auth (Giriş/Kayıt/Doğrulama/Şifre Sıfırlama) sayfaları
    const authPages = ['login', 'signup', 'verify-email', 'forgot-password-step1', 'forgot-password-verify', 'forgot-password-reset'];

    const slider = document.getElementById('bgSlider');
    const overlay = document.getElementById('bgOverlay');
    const header = document.getElementById('mainHeader');

    if (dashboardPages.includes(id)) {
        if (slider) slider.style.display = 'none';
        if (overlay) overlay.style.display = 'none';
        if (header) header.style.display = 'none';

        updateProfileDisplay();

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

        if (id === 'dashboard') {
            // Hesapları yükle, sonra dashboard'u güncelle
            (async () => {
                await loadAccounts();
                updateDashboardData();
                updateBalanceUI();
            })();
        }
        if (id === 'accounts') {
            // Hesapları otomatik olarak yükle
            loadAccounts();
            updateBalanceUI();
        }
        if (id === 'cards') {
            // load and render cards for current user (fire-and-forget)
            (async () => {
                try {
                    // CRITICAL: Client ID must be loaded first because Card Service uses client.ID as customerId
                    if (!currentClient || (!currentClient.id && !currentClient.ID)) {
                        console.debug('cards: currentClient not loaded, loading client info first...');
                        await loadClientInfo();
                    }
                    // Reset dropdown to "Tüm Kartlarım"
                    currentCardTypeFilter = 'all';
                    updateCardTypeDropdownText();
                    const list = await loadCards({ customerId: getCustomerId() });
                    renderCardsList(list);
                } catch (e) {
                    console.debug('cards: load failed', e);
                }
            })();
        }
        // Only load client details when user explicitly opens profile page (lazy-load)
        if (id === 'profile') {
            // fire-and-forget; UI will update when data arrives
            loadClientInfo().then(ok => { if (!ok) console.debug('profile: client not loaded'); });
        }
        // When user opens transactions page, load transactions for user's accounts
        if (false) { // transactions-page removed
            (async () => {
                try {
                    await loadTransactionsForCurrentUser();
                } catch (e) {
                    console.debug('transactions: load failed', e);
                }
            })();
        }
    } else {
        // Slider ve Header'ı auth sayfalarında da göster
        if (slider) slider.style.display = 'block';
        if (overlay) overlay.style.display = 'block';
        if (header) header.style.display = 'block';
        
        // Login sayfasına geçildiğinde formu temizle
        if (id === 'login') {
            clearLoginForm();
        }
        // Signup sayfasına geçildiğinde formu temizle
        if (id === 'signup') {
            clearSignupForm();
        }
        // Forgot password sayfalarına geçildiğinde formları temizle
        if (id === 'forgot-password-step1') {
            clearForgotPasswordStep1Form();
            currentSessionId = null;
            currentVerificationCode = null;
        }
        if (id === 'forgot-password-verify') {
            // Verify sayfası zaten currentSessionId kullanıyor, temizleme gerekmez
        }
        if (id === 'forgot-password-reset') {
            clearForgotPasswordResetForm();
        }
    }
}

// Login formunu temizle
function clearLoginForm() {
    const loginIdInput = document.getElementById('loginId');
    const loginPasswordInput = document.getElementById('loginPassword');
    const loginMessageEl = document.getElementById('loginMessage');
    
    if (loginIdInput) {
        loginIdInput.value = '';
        loginIdInput.style.borderColor = '';
    }
    if (loginPasswordInput) {
        loginPasswordInput.value = '';
        loginPasswordInput.style.borderColor = '';
    }
    if (loginMessageEl) {
        loginMessageEl.style.display = 'none';
        loginMessageEl.textContent = '';
    }
}

// Forgot Password Step 1 formunu temizle
function clearForgotPasswordStep1Form() {
    const inputs = ['forgotPasswordTC', 'forgotPasswordEmail'];
    inputs.forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.value = '';
            input.style.borderColor = '';
        }
    });
    
    const errorElements = ['forgotPasswordTcError', 'forgotPasswordEmailError'];
    errorElements.forEach(errorId => {
        const errorEl = document.getElementById(errorId);
        if (errorEl) {
            errorEl.style.display = 'none';
            errorEl.textContent = '';
        }
    });
    
    const messageEl = document.getElementById('forgotPasswordStep1Message');
    if (messageEl) {
        messageEl.style.display = 'none';
        messageEl.textContent = '';
    }
}

// Forgot Password Reset formunu temizle
function clearForgotPasswordResetForm() {
    const inputs = ['forgotPasswordNewPassword', 'forgotPasswordNewPasswordConfirm'];
    inputs.forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.value = '';
            input.style.borderColor = '';
        }
    });
    
    const errorElements = ['forgotPasswordNewPassError', 'forgotPasswordNewPassConfirmError'];
    errorElements.forEach(errorId => {
        const errorEl = document.getElementById(errorId);
        if (errorEl) {
            errorEl.style.display = 'none';
            errorEl.textContent = '';
        }
    });
    
    const messageEl = document.getElementById('forgotPasswordResetMessage');
    if (messageEl) {
        messageEl.style.display = 'none';
        messageEl.textContent = '';
    }
}

// Signup formunu temizle (global fonksiyon - sayfa değiştiğinde kullanılır)
function clearSignupForm() {
    // Tüm input alanlarını temizle ve border'ları sıfırla
    const inputs = ['signupName', 'signupSurname', 'signupTC', 'signupPassword', 'signupEmail', 'signupPhone'];
    inputs.forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.value = '';
            input.style.borderColor = '';
            input.style.borderLeftColor = '';
            input.style.borderLeft = '';
        }
    });
    
    // Telefon prefix border'ını temizle
    const phoneInput = document.getElementById('signupPhone');
    if (phoneInput) {
        const prefix = phoneInput.previousElementSibling;
        if (prefix && prefix.classList.contains('phone-prefix')) {
            prefix.style.borderColor = '';
        }
    }
    
    // Tüm hata mesajlarını gizle
    const errorElements = ['signupNameError', 'signupSurnameError', 'signupTcError', 
                          'signupPassError', 'signupEmailError', 'signupPhoneError'];
    errorElements.forEach(errorId => {
        const errorEl = document.getElementById(errorId);
        if (errorEl) {
            errorEl.style.display = 'none';
            errorEl.textContent = '';
        }
    });
    
    // Genel hata mesajını gizle
    const signupMessage = document.getElementById('signupMessage');
    if (signupMessage) {
        signupMessage.style.display = 'none';
        signupMessage.textContent = '';
    }
}

// --- YENİ EKLENEN SIDEBAR FONKSİYONU ---
function toggleProfileSidebar() {
    const sidebar = document.getElementById('side-profile-menu');
    const overlay = document.getElementById('sidebarOverlay');

    if (sidebar.classList.contains('active')) {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
    } else {
        sidebar.classList.add('active');
        overlay.classList.add('active');
        updateProfileDisplay();
    }
}

// --- AYARLAR VE ÖZELLİKLER ---

// Karanlık Mod
function toggleTheme() {
    document.body.classList.toggle('dark-mode');
    const isDark = document.body.classList.contains('dark-mode');
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
}

// Dil Değiştirme
function toggleLanguage() {
    currentLang = currentLang === 'tr' ? 'en' : 'tr';
    const langBtn = document.getElementById('langBtn');
    const headerLang = document.getElementById('headerLangBtn');
    if (langBtn) langBtn.innerText = currentLang.toUpperCase();
    if (headerLang) headerLang.innerText = currentLang.toUpperCase();

    const elements = document.querySelectorAll('[data-lang]');
    elements.forEach(el => {
        const key = el.getAttribute('data-lang');
        if (translations[currentLang][key]) {
            if (el.tagName === 'INPUT') el.placeholder = translations[currentLang][key];
            else if (el.tagName === 'SPAN' || el.tagName === 'P' || el.tagName === 'H1' || el.tagName === 'H2' || el.tagName === 'H3' || el.tagName === 'H4' || el.tagName === 'DIV' || el.tagName === 'BUTTON' || el.tagName === 'A') {
                const icon = el.querySelector('i');
                if (icon) {
                    const text = translations[currentLang][key];
                    el.innerHTML = '';
                    el.appendChild(icon);
                    el.append(' ' + text);
                } else {
                    el.innerText = translations[currentLang][key];
                }
            }
        }
    });
}

function toggleTheme() {
    // keep existing API for settings toggle (used by checkbox)
    document.body.classList.toggle('dark-mode');
    const isDark = document.body.classList.contains('dark-mode');
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
    // update settings checkbox if present
    const darkToggle = document.getElementById('darkModeToggle');
    if (darkToggle) darkToggle.checked = isDark;
    // update header button icon
    const hTheme = document.getElementById('headerThemeBtn');
    if (hTheme) hTheme.innerHTML = isDark ? '<i class="fas fa-moon"></i>' : '<i class="fas fa-sun"></i>';
}

// Hesap Silme
async function deleteAccount() {
    // Calls API Gateway /auth/delete-account (DELETE) with TC and password in body.
    const enteredTC = document.getElementById('delTC').value.replace(/\D/g, '');
    const enteredPass = document.getElementById('delPass').value;

    if (!enteredTC || enteredTC.length !== 11) {
        showToast(currentLang === 'tr' ? 'Lütfen 11 haneli T.C. girin.' : 'Please enter an 11-digit ID.', 'error');
        return;
    }
    if (!enteredPass) {
        showToast(currentLang === 'tr' ? 'Lütfen şifrenizi girin.' : 'Please enter your password.', 'error');
        return;
    }

    // If we have profile TC loaded, require it to match as a client-side safeguard
    if (currentUser && currentUser.tc && currentUser.tc.toString() !== enteredTC) {
        showToast(currentLang === 'tr' ? 'Girilen T.C. hesabınızla eşleşmiyor.' : 'Entered ID does not match your account.', 'error');
        return;
    }

    // If we don't have server-side TC available to verify, require explicit textual confirmation
    const confirmInput = document.getElementById('delConfirm') ? document.getElementById('delConfirm').value.trim().toUpperCase() : '';
    if (!currentUser || !currentUser.tc) {
        if (confirmInput !== 'SIL' && confirmInput !== 'DELETE') {
            showToast(currentLang === 'tr' ? 'Lütfen onay için "SIL" yazın.' : 'Please type "DELETE" to confirm.', 'error');
            return;
        }
    }

    try {
        // Ensure refresh token is present/valid by attempting refresh first
        const refreshed = await tryRefreshToken();
        if (!refreshed) {
            showToast(currentLang === 'tr'
                ? 'İşlem için aktif oturumunuz bulunamadı. Lütfen tekrar giriş yapın (dev: cookie/https kontrolü).' 
                : 'No active session found. Please login again (dev: check cookie/HTTPS).', 'error');
            return;
        }

        // Proceed with delete, include TC and password in body (server validates password+refresh cookie)
        await apiCall('/auth/delete-account', 'DELETE', { tc: enteredTC, password: enteredPass }, true);

        // on success clear local state
        localStorage.removeItem('triobank_token');
        currentUser = null;
        showToast(currentLang === 'tr' ? 'Hesabınız silindi.' : 'Account deleted.', 'success');
        showPage('home');
    } catch (err) {
        // Look for specific server hint about refresh cookie missing
        const msg = (err && err.message) ? err.message.toLowerCase() : '';
        if (msg.includes('there is no refresh token') || msg.includes('refresh token')) {
            showToast(currentLang === 'tr'
                ? 'Sunucuda refresh token bulunamadı. Giriş sırasında cookie set edilememiş olabilir (HTTPS/secure flag nedeniyle). Lütfen HTTPS deneyin veya sunucu konfigürasyonunu kontrol edin.'
                : 'Refresh token not present on server. Cookie may not be set due to HTTPS/secure flag. Try HTTPS or check server config.', 'error');
            return;
        }

        // apiCall already shows toast with server message; nothing more to do
    }
}

// --- AUTHENTICATION (API INTEGRATED) ---

// 1. REGISTER
const signupForm = document.getElementById('signupForm');
if (signupForm) {
    let isSubmitting = false; // Double-click önleme
    
    // Helper function: Belirli bir input için hata göster
    function showSignupFieldError(inputId, errorId, message) {
        const input = document.getElementById(inputId);
        const errorEl = document.getElementById(errorId);
        if (input) {
            input.style.borderColor = 'var(--negative-red)';
            // Telefon için özel işlem (prefix ile birlikte)
            if (inputId === 'signupPhone') {
                input.style.borderLeft = '1px solid var(--negative-red)';
                const prefix = input.previousElementSibling;
                if (prefix && prefix.classList.contains('phone-prefix')) {
                    prefix.style.borderColor = 'var(--negative-red)';
                }
            }
        }
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.style.display = 'block';
        }
    }
    
    signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Eğer zaten gönderiliyorsa, yeni istek yapma
        if (isSubmitting) {
            showToast('İstek gönderiliyor, lütfen bekleyin...', 'info');
            return;
        }
        
        // Hata mesajlarını gizle ve input border'larını sıfırla
        const signupMessage = document.getElementById('signupMessage');
        if (signupMessage) {
            signupMessage.style.display = 'none';
            signupMessage.textContent = '';
        }
        
        // Tüm hata mesajlarını gizle
        const errorElements = ['signupNameError', 'signupSurnameError', 'signupTcError', 
                              'signupPassError', 'signupEmailError', 'signupPhoneError'];
        errorElements.forEach(errorId => {
            const errorEl = document.getElementById(errorId);
            if (errorEl) {
                errorEl.style.display = 'none';
                errorEl.textContent = '';
            }
        });
        
        // Input border'larını temizle
        const inputs = ['signupName', 'signupSurname', 'signupTC', 'signupPassword', 'signupEmail', 'signupPhone'];
        inputs.forEach(id => {
            const input = document.getElementById(id);
            if (input) {
                input.style.borderColor = '';
                input.style.borderLeft = '';
                // Telefon için özel işlem
                if (id === 'signupPhone') {
                    const prefix = input.previousElementSibling;
                    if (prefix && prefix.classList.contains('phone-prefix')) {
                        prefix.style.borderColor = '';
                    }
                }
            }
        });
        
        const name = document.getElementById('signupName').value.trim();
        const surname = document.getElementById('signupSurname').value.trim();
        const tc = document.getElementById('signupTC').value;
        const pass = document.getElementById('signupPassword').value;
        const email = document.getElementById('signupEmail').value.trim();
        const phone = document.getElementById('signupPhone').value.replace(/\D/g, '');

        // Validasyonlar
        let hasError = false;
        
        // Ad validasyonu
        if (!name || name.length === 0) {
            showSignupFieldError('signupName', 'signupNameError', 'Ad boş olamaz.');
            hasError = true;
        } else if (name.length < 2) {
            showSignupFieldError('signupName', 'signupNameError', 'Ad en az 2 karakter olmalıdır.');
            hasError = true;
        }
        
        // Soyad validasyonu
        if (!surname || surname.length === 0) {
            showSignupFieldError('signupSurname', 'signupSurnameError', 'Soyad boş olamaz.');
            hasError = true;
        } else if (surname.length < 2) {
            showSignupFieldError('signupSurname', 'signupSurnameError', 'Soyad en az 2 karakter olmalıdır.');
            hasError = true;
        }
        
        // TC validasyonu
        if (!tc || tc.length === 0) {
            showSignupFieldError('signupTC', 'signupTcError', 'T.C. Kimlik No boş olamaz.');
            hasError = true;
        } else if (tc.length !== 11) {
            showSignupFieldError('signupTC', 'signupTcError', 'T.C. Kimlik No 11 haneli olmalıdır.');
            hasError = true;
        }
        
        // Şifre validasyonu
        if (!pass || pass.length === 0) {
            showSignupFieldError('signupPassword', 'signupPassError', 'Şifre boş olamaz.');
            hasError = true;
        } else if (pass.length < 8) {
            showSignupFieldError('signupPassword', 'signupPassError', 'Şifre en az 8 karakter olmalıdır.');
            hasError = true;
        }
        
        // E-posta validasyonu
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email || email.length === 0) {
            showSignupFieldError('signupEmail', 'signupEmailError', 'E-posta adresi boş olamaz.');
            hasError = true;
        } else if (!emailRegex.test(email)) {
            showSignupFieldError('signupEmail', 'signupEmailError', 'Geçerli bir e-posta adresi giriniz.');
            hasError = true;
        }
        
        // Telefon validasyonu
        if (!phone || phone.length === 0) {
            showSignupFieldError('signupPhone', 'signupPhoneError', 'Telefon numarası boş olamaz.');
            hasError = true;
        } else if (phone.length !== 10) {
            showSignupFieldError('signupPhone', 'signupPhoneError', 'Telefon numarası 10 haneli olmalıdır.');
            hasError = true;
        }
        
        if (hasError) {
            if (signupMessage) {
                signupMessage.textContent = 'Lütfen tüm alanları doğru şekilde doldurunuz.';
                signupMessage.style.display = 'block';
            }
            return;
        }

        // Submit butonunu disable et
        const submitBtn = signupForm.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn ? submitBtn.innerText : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerText = 'Gönderiliyor...';
        }
        isSubmitting = true;

        try {
            const res = await apiCall('/auth/register', 'POST', { name, surname, email, password: pass, tel: phone, tc });
            const sid = extractSessionId(res);
            if (sid) {
                currentSessionId = sid;
                authFlowType = 'register';
                document.getElementById('verifyEmailDisplay').innerText = email;
                document.getElementById('verifyCodeInput').value = '';
                showPage('verify-email');
                startVerificationTimer('timerDisplay', 'signup');
            }
        } catch (err) {
            // Hata durumunda butonu tekrar aktif et
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
            }
            isSubmitting = false;

            // Hata mesajını göster
            let errorMessage = '';
            const errorText = err.message || '';
            const statusCode = err.status || 0;

            // Status koduna ve mesaja göre farklı mesajlar göster
            if (statusCode === 409 || errorText.toLowerCase().includes('already exist') || errorText.toLowerCase().includes('zaten mevcut')) {
                errorMessage = 'Bu T.C. Kimlik No ile kayıtlı bir kullanıcı zaten mevcut. Lütfen giriş yapın.';
                showSignupFieldError('signupTC', 'signupTcError', 'Bu T.C. Kimlik No zaten kayıtlı.');
            } else if (statusCode === 429 || errorText.toLowerCase().includes('already there is an active process')) {
                errorMessage = 'Çok fazla deneme yapıldı. Lütfen birkaç dakika sonra tekrar deneyiniz.';
            } else if (statusCode === 400 || errorText.toLowerCase().includes('bad request')) {
                errorMessage = 'Eksik veya hatalı bilgi girdiniz. Lütfen tüm alanları kontrol ediniz.';
            } else if (statusCode === 500) {
                errorMessage = 'Sunucu hatası oluştu. Lütfen daha sonra tekrar deneyiniz.';
            } else {
                errorMessage = 'Kayıt işlemi başarısız oldu. Lütfen bilgilerinizi kontrol ediniz.';
            }

            if (signupMessage) {
                signupMessage.textContent = errorMessage;
                signupMessage.style.display = 'block';
            } else {
                showToast(errorMessage, 'error');
            }
        } finally {
            // Başarılı durumda da flag'i sıfırla (sayfa değişeceği için gerekli olmayabilir ama güvenli)
            if (!currentSessionId) {
                isSubmitting = false;
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerText = originalBtnText;
                }
            }
        }
    });
}

// 2. LOGIN
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    let isSubmitting = false; // Double-click önleme
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Eğer zaten gönderiliyorsa, yeni istek yapma
        if (isSubmitting) {
            showToast('İstek gönderiliyor, lütfen bekleyin...', 'info');
            return;
        }
        
        const tc = document.getElementById('loginId').value;
        const pass = document.getElementById('loginPassword').value;

        // Submit butonunu disable et
        const submitBtn = loginForm.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn ? submitBtn.innerText : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerText = 'Giriş yapılıyor...';
        }
        isSubmitting = true;

        // Hata mesajını gizle ve input border'larını sıfırla
        const loginMessageEl = document.getElementById('loginMessage');
        if (loginMessageEl) {
            loginMessageEl.style.display = 'none';
            loginMessageEl.textContent = '';
        }
        const loginIdInput = document.getElementById('loginId');
        const loginPasswordInput = document.getElementById('loginPassword');
        if (loginIdInput) {
            loginIdInput.style.borderColor = '';
        }
        if (loginPasswordInput) {
            loginPasswordInput.style.borderColor = '';
        }

        try {
            const res = await apiCall('/auth/login', 'POST', { tc, password: pass });
            const sid = extractSessionId(res);
            if (sid) {
                currentSessionId = sid;
                authFlowType = 'login';
                document.getElementById('verifyEmailDisplay').innerText = 'kayıtlı iletişim adresinize';
                document.getElementById('verifyCodeInput').value = '';
                showPage('verify-email');
                startVerificationTimer('timerDisplay', 'login');
            }
        } catch (err) {
            // Hata durumunda butonu tekrar aktif et
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
            }
            isSubmitting = false;

            // Hata mesajını göster
            let errorMessage = '';
            if (loginMessageEl) {
                // API'den gelen hata mesajını kontrol et
                const errorText = err.message || '';
                const statusCode = err.status || 0;

                // Status koduna ve mesaja göre farklı mesajlar göster
                if (statusCode === 401) {
                    // 401 hatası: Kullanıcı bulunamadı veya şifre yanlış
                    // Backend'den gelen mesaj aynı olduğu için, genel bir mesaj gösterelim
                    errorMessage = 'T.C. Kimlik No veya şifre hatalı. Lütfen bilgilerinizi kontrol ediniz.';
                } else if (statusCode === 404) {
                    errorMessage = 'Kullanıcı bulunamadı. Lütfen kayıt olunuz.';
                } else if (statusCode === 400) {
                    errorMessage = 'Eksik veya hatalı bilgi girdiniz. Lütfen tüm alanları doldurunuz.';
                } else if (statusCode === 429) {
                    errorMessage = 'Çok fazla deneme yapıldı. Lütfen birkaç dakika sonra tekrar deneyiniz.';
                } else if (errorText.toLowerCase().includes('user') || errorText.toLowerCase().includes('kullanıcı') || errorText.toLowerCase().includes('not found')) {
                    errorMessage = 'Bu T.C. Kimlik No ile kayıtlı kullanıcı bulunamadı.';
                } else if (errorText.toLowerCase().includes('password') || errorText.toLowerCase().includes('şifre') || errorText.toLowerCase().includes('invalid')) {
                    errorMessage = 'Girdiğiniz şifre hatalı. Lütfen tekrar deneyiniz.';
                } else {
                    errorMessage = 'Giriş yapılamadı. Lütfen T.C. Kimlik No ve şifrenizi kontrol ediniz.';
                }

                loginMessageEl.textContent = errorMessage;
                loginMessageEl.style.display = 'block';
                
                // Input alanlarına hata stili ekle
                const loginIdInput = document.getElementById('loginId');
                const loginPasswordInput = document.getElementById('loginPassword');
                if (loginIdInput) {
                    loginIdInput.style.borderColor = 'var(--negative-red)';
                }
                if (loginPasswordInput) {
                    loginPasswordInput.style.borderColor = 'var(--negative-red)';
                }
            } else {
                // Fallback: Toast göster
                showToast(errorMessage || 'Giriş yapılamadı. Lütfen bilgilerinizi kontrol ediniz.', 'error');
            }
        } finally {
            // Başarılı durumda da flag'i sıfırla
            if (!currentSessionId) {
                isSubmitting = false;
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerText = originalBtnText;
                }
            }
        }
    });
}

// 3. OTP VERIFICATION
const verifyForm = document.getElementById('verifyForm');
if (verifyForm) {
    verifyForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const code = document.getElementById('verifyCodeInput').value;

        if (!currentSessionId) {
            showToast('Oturum bilgisi eksik. Lütfen giriş işlemini tekrarlayın.', 'error');
            showPage('login');
            return;
        }

        // Expect 4-digit code
        if (!code || code.length !== 4) {
            showToast(currentLang === 'tr' ? 'Lütfen 4 haneli kod giriniz.' : 'Please enter the 4-digit code.', 'error');
            return;
        }

        const endpoint = authFlowType === 'register' ? '/auth/register/confirm' : '/auth/login/confirm';
        try {
            const res = await apiCall(endpoint, 'POST', { 'session-id': currentSessionId, 'code': code });
            const token = extractAccessToken(res);
            if (token) {
                localStorage.setItem('triobank_token', token);
                clearInterval(verificationTimer);
                showToast('Giriş Başarılı!', 'success');
                // Profil yükle ve yönlendir
                await loadUserProfile();
                showPage('dashboard');
            }
        } catch (err) { }
    });
}

// 4. FORGOT PASSWORD

// Step 1: Initiate password reset (TC + Email)
const forgotPasswordStep1Form = document.getElementById('forgotPasswordStep1Form');
if (forgotPasswordStep1Form) {
    let isSubmitting = false;
    forgotPasswordStep1Form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (isSubmitting) {
            showToast('İstek gönderiliyor, lütfen bekleyin...', 'info');
            return;
        }

        // Clear previous errors
        const messageEl = document.getElementById('forgotPasswordStep1Message');
        if (messageEl) {
            messageEl.style.display = 'none';
            messageEl.textContent = '';
        }
        
        const errorElements = ['forgotPasswordTcError', 'forgotPasswordEmailError'];
        errorElements.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.style.display = 'none';
                el.textContent = '';
            }
        });

        const inputs = ['forgotPasswordTC', 'forgotPasswordEmail'];
        inputs.forEach(id => {
            const input = document.getElementById(id);
            if (input) input.style.borderColor = '';
        });

        const tc = document.getElementById('forgotPasswordTC').value;
        const email = document.getElementById('forgotPasswordEmail').value.trim();

        // Validations
        let hasError = false;

        if (!tc || tc.length !== 11) {
            const input = document.getElementById('forgotPasswordTC');
            const errorEl = document.getElementById('forgotPasswordTcError');
            if (input) input.style.borderColor = 'var(--negative-red)';
            if (errorEl) {
                errorEl.textContent = tc.length === 0 ? 'TC Kimlik No boş olamaz.' : 'TC Kimlik No 11 haneli olmalıdır.';
                errorEl.style.display = 'block';
            }
            hasError = true;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email || !emailRegex.test(email)) {
            const input = document.getElementById('forgotPasswordEmail');
            const errorEl = document.getElementById('forgotPasswordEmailError');
            if (input) input.style.borderColor = 'var(--negative-red)';
            if (errorEl) {
                errorEl.textContent = email.length === 0 ? 'E-posta adresi boş olamaz.' : 'Geçerli bir e-posta adresi giriniz.';
                errorEl.style.display = 'block';
            }
            hasError = true;
        }

        if (hasError) {
            if (messageEl) {
                messageEl.textContent = 'Lütfen tüm alanları doğru şekilde doldurunuz.';
                messageEl.style.display = 'block';
            }
            return;
        }

        const submitBtn = forgotPasswordStep1Form.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn ? submitBtn.innerText : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerText = 'Gönderiliyor...';
        }
        isSubmitting = true;

        try {
            const res = await apiCall('/auth/forgot-password/initiate', 'POST', { tc, email });
            const sid = extractSessionId(res);
            if (sid) {
                currentSessionId = sid;
                authFlowType = 'password-reset';
                document.getElementById('forgotPasswordEmailDisplay').innerText = email;
                document.getElementById('forgotPasswordCodeInput').value = '';
                showPage('forgot-password-verify');
                startVerificationTimer('forgotPasswordTimerDisplay', 'forgot-password-step1');
            }
        } catch (err) {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
            }
            isSubmitting = false;

            let errorMessage = '';
            const errorText = err.message || '';
            const statusCode = err.status || 0;

            if (statusCode === 401 || errorText.toLowerCase().includes('do not match') || errorText.toLowerCase().includes('eşleşmiyor')) {
                errorMessage = 'T.C. Kimlik No ve E-posta adresi eşleşmiyor. Lütfen kontrol ediniz.';
            } else if (statusCode === 404 || errorText.toLowerCase().includes('not found')) {
                errorMessage = 'Bu T.C. Kimlik No ile kayıtlı kullanıcı bulunamadı.';
            } else if (statusCode === 429 || errorText.toLowerCase().includes('active process')) {
                errorMessage = 'Çok fazla deneme yapıldı. Lütfen birkaç dakika sonra tekrar deneyiniz.';
            } else if (statusCode === 400) {
                errorMessage = 'Eksik veya hatalı bilgi girdiniz. Lütfen tüm alanları doldurunuz.';
            } else {
                errorMessage = 'İşlem başarısız oldu. Lütfen daha sonra tekrar deneyiniz.';
            }

            if (messageEl) {
                messageEl.textContent = errorMessage;
                messageEl.style.display = 'block';
            } else {
                showToast(errorMessage, 'error');
            }
        }
    });
}

// Step 2: Verify code
const forgotPasswordVerifyForm = document.getElementById('forgotPasswordVerifyForm');
if (forgotPasswordVerifyForm) {
    forgotPasswordVerifyForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const code = document.getElementById('forgotPasswordCodeInput').value;

        if (!currentSessionId) {
            showToast('Oturum bilgisi eksik. Lütfen işlemi tekrarlayın.', 'error');
            showPage('forgot-password-step1');
            return;
        }

        if (!code || code.length !== 4) {
            const errorEl = document.getElementById('forgotPasswordCodeError');
            const input = document.getElementById('forgotPasswordCodeInput');
            if (input) input.style.borderColor = 'var(--negative-red)';
            if (errorEl) {
                errorEl.textContent = 'Lütfen 4 haneli kod giriniz.';
                errorEl.style.display = 'block';
            }
            return;
        }

        // Clear error
        const errorEl = document.getElementById('forgotPasswordCodeError');
        const input = document.getElementById('forgotPasswordCodeInput');
        if (errorEl) errorEl.style.display = 'none';
        if (input) input.style.borderColor = '';

        const messageEl = document.getElementById('forgotPasswordVerifyMessage');
        if (messageEl) {
            messageEl.style.display = 'none';
            messageEl.textContent = '';
        }

        try {
            const res = await apiCall('/auth/forgot-password/verify-code', 'POST', { 'session-id': currentSessionId, 'code': code });
            if (res && res.verified) {
                // Save code for reset step
                currentVerificationCode = code;
                // Clear timer
                if (verificationTimer) {
                    clearInterval(verificationTimer);
                    verificationTimer = null;
                }
                showPage('forgot-password-reset');
            }
        } catch (err) {
            const errorText = err.message || '';
            const statusCode = err.status || 0;

            if (input) input.style.borderColor = 'var(--negative-red)';
            
            let errorMessage = '';
            if (statusCode === 401 || errorText.toLowerCase().includes('not correct') || errorText.toLowerCase().includes('not found')) {
                errorMessage = 'Kod yanlış veya süresi dolmuş. Lütfen tekrar deneyiniz.';
            } else {
                errorMessage = 'Kod doğrulanamadı. Lütfen tekrar deneyiniz.';
            }

            if (messageEl) {
                messageEl.textContent = errorMessage;
                messageEl.style.display = 'block';
            } else {
                showToast(errorMessage, 'error');
            }
        }
    });
}

// Step 3: Reset password
const forgotPasswordResetForm = document.getElementById('forgotPasswordResetForm');
if (forgotPasswordResetForm) {
    let isSubmitting = false;
    forgotPasswordResetForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (isSubmitting) {
            showToast('İstek gönderiliyor, lütfen bekleyin...', 'info');
            return;
        }

        // Clear previous errors
        const messageEl = document.getElementById('forgotPasswordResetMessage');
        if (messageEl) {
            messageEl.style.display = 'none';
            messageEl.textContent = '';
        }
        
        const errorElements = ['forgotPasswordNewPassError', 'forgotPasswordNewPassConfirmError'];
        errorElements.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.style.display = 'none';
                el.textContent = '';
            }
        });

        const inputs = ['forgotPasswordNewPassword', 'forgotPasswordNewPasswordConfirm'];
        inputs.forEach(id => {
            const input = document.getElementById(id);
            if (input) input.style.borderColor = '';
        });

        const newPassword = document.getElementById('forgotPasswordNewPassword').value;
        const newPasswordConfirm = document.getElementById('forgotPasswordNewPasswordConfirm').value;

        if (!currentSessionId) {
            showToast('Oturum bilgisi eksik. Lütfen işlemi tekrarlayın.', 'error');
            showPage('forgot-password-step1');
            return;
        }

        // Get code from saved variable or form
        const code = currentVerificationCode || document.getElementById('forgotPasswordCodeInput')?.value;
        if (!code || code.length !== 4) {
            showToast('Kod bilgisi eksik. Lütfen işlemi tekrarlayın.', 'error');
            showPage('forgot-password-step1');
            return;
        }

        // Validations
        let hasError = false;

        if (!newPassword || newPassword.length < 8) {
            const input = document.getElementById('forgotPasswordNewPassword');
            const errorEl = document.getElementById('forgotPasswordNewPassError');
            if (input) input.style.borderColor = 'var(--negative-red)';
            if (errorEl) {
                errorEl.textContent = newPassword.length === 0 ? 'Şifre boş olamaz.' : 'Şifre en az 8 karakter olmalıdır.';
                errorEl.style.display = 'block';
            }
            hasError = true;
        }

        if (newPassword !== newPasswordConfirm) {
            const input = document.getElementById('forgotPasswordNewPasswordConfirm');
            const errorEl = document.getElementById('forgotPasswordNewPassConfirmError');
            if (input) input.style.borderColor = 'var(--negative-red)';
            if (errorEl) {
                errorEl.textContent = 'Şifreler eşleşmiyor.';
                errorEl.style.display = 'block';
            }
            hasError = true;
        }

        if (hasError) {
            if (messageEl) {
                messageEl.textContent = 'Lütfen şifre bilgilerini doğru şekilde giriniz.';
                messageEl.style.display = 'block';
            }
            return;
        }

        const submitBtn = forgotPasswordResetForm.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn ? submitBtn.innerText : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerText = 'Şifre Sıfırlanıyor...';
        }
        isSubmitting = true;

        try {
            await apiCall('/auth/forgot-password/reset', 'POST', { 
                'session-id': currentSessionId, 
                'code': code,
                'new_password': newPassword 
            });
            
            // Success
            showToast('Şifreniz başarıyla sıfırlandı. Lütfen yeni şifrenizle giriş yapınız.', 'success');
            currentSessionId = null;
            authFlowType = null;
            currentVerificationCode = null;
            
            // Clear forms
            document.getElementById('forgotPasswordNewPassword').value = '';
            document.getElementById('forgotPasswordNewPasswordConfirm').value = '';
            
            // Redirect to login
            setTimeout(() => {
                showPage('login');
            }, 1500);
        } catch (err) {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
            }
            isSubmitting = false;

            let errorMessage = '';
            const errorText = err.message || '';
            const statusCode = err.status || 0;

            if (statusCode === 401 || errorText.toLowerCase().includes('not correct') || errorText.toLowerCase().includes('not found') || errorText.toLowerCase().includes('expired')) {
                errorMessage = 'Kod geçersiz veya süresi dolmuş. Lütfen işlemi baştan başlatınız.';
            } else if (statusCode === 400) {
                errorMessage = 'Şifre gereksinimlerini karşılamıyor. Lütfen en az 8 karakter giriniz.';
            } else {
                errorMessage = 'Şifre sıfırlama işlemi başarısız oldu. Lütfen tekrar deneyiniz.';
            }

            if (messageEl) {
                messageEl.textContent = errorMessage;
                messageEl.style.display = 'block';
            } else {
                showToast(errorMessage, 'error');
            }
        }
    });
}

// 5. HELPER: Verification Timer
function startVerificationTimer(displayId, failRedirectId) {
    let timeLeft = 180;
    const timerDisplay = document.getElementById(displayId);
    if (verificationTimer) clearInterval(verificationTimer);

    verificationTimer = setInterval(() => {
        const m = Math.floor(timeLeft / 60);
        let s = timeLeft % 60;
        s = s < 10 ? '0' + s : s;
        if (timerDisplay) timerDisplay.innerText = `0${m}:${s}`;

        if (timeLeft <= 0) {
            clearInterval(verificationTimer);
            showToast('Süre doldu.', 'error');
            showPage(failRedirectId);
        }
        timeLeft -= 1;
    }, 1000);
}

// --- PROFILE UPDATE ---
async function updateProfile() {
    const email = document.getElementById('profileEmail') ? document.getElementById('profileEmail').value.trim() : '';
    const phone = document.getElementById('profilePhone') ? document.getElementById('profilePhone').value.replace(/\D/g, '') : '';

    if (!email) { showToast(currentLang === 'tr' ? 'E-posta boş olamaz.' : 'Email cannot be empty.', 'error'); return; }

    try {
        await apiCall('/auth/user/update', 'POST', { email, phone }, true);
        showToast(currentLang === 'tr' ? 'Profil güncellendi.' : 'Profile updated.', 'success');
        // reload profile from server
        await loadUserProfile();
        showPage('profile');
    } catch (err) {
        // apiCall shows toast
    }
}

// --- TOGGLE PASSWORD VISIBILITY ---
function togglePasswordVisibility(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    
    if (input && icon) {
        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        } else {
            input.type = 'password';
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        }
    }
}

// --- PASSWORD CHANGE ---
async function changePassword() {
    const oldPass = document.getElementById('oldPass') ? document.getElementById('oldPass').value : '';
    const newPass = document.getElementById('newPass') ? document.getElementById('newPass').value : '';

    if (!oldPass || !newPass) { 
        showToast(currentLang === 'tr' ? 'Eski ve yeni şifre gerekli.' : 'Old and new password required.', 'error'); 
        return; 
    }
    if (newPass.length < 6) { 
        showToast(currentLang === 'tr' ? 'Yeni şifre en az 6 karakter olmalıdır.' : 'New password must be at least 6 characters.', 'error'); 
        return; 
    }

    try {
        await apiCall('/auth/password-change', 'POST', { old_password: oldPass, new_password: newPass }, true);
        showToast(currentLang === 'tr' ? 'Şifre başarıyla güncellendi.' : 'Password changed successfully.', 'success');
        // Clear password fields after successful change
        const oldPassInput = document.getElementById('oldPass');
        const newPassInput = document.getElementById('newPass');
        if (oldPassInput) oldPassInput.value = '';
        if (newPassInput) newPassInput.value = '';
        // Reset password visibility icons and input types
        const oldIcon = document.getElementById('toggleOldPass');
        const newIcon = document.getElementById('toggleNewPass');
        if (oldIcon && oldPassInput) { 
            oldIcon.classList.remove('fa-eye-slash'); 
            oldIcon.classList.add('fa-eye');
            oldPassInput.type = 'password';
        }
        if (newIcon && newPassInput) { 
            newIcon.classList.remove('fa-eye-slash'); 
            newIcon.classList.add('fa-eye');
            newPassInput.type = 'password';
        }
        setTimeout(() => showPage('dashboard'), 1500);
    } catch (err) {
        // Check if error is about wrong old password
        const errorMessage = (err?.message || err?.error || err?.response?.error || err?.response?.message || '').toLowerCase();
        const errorText = err?.message || err?.error || err?.response?.error || err?.response?.message || '';
        
        if (errorMessage.includes('old password') || 
            errorMessage.includes('mevcut şifre') ||
            errorMessage.includes('incorrect password') ||
            errorMessage.includes('yanlış şifre') ||
            errorMessage.includes('invalid password') ||
            errorMessage.includes('wrong password') ||
            errorMessage.includes('password mismatch')) {
            showToast(currentLang === 'tr' ? 'Mevcut şifre yanlış. Lütfen tekrar deneyin.' : 'Current password is incorrect. Please try again.', 'error');
        } else if (errorText) {
            // Show the error message from server
            showToast(errorText, 'error');
        } else {
            // Generic error message
            showToast(currentLang === 'tr' ? 'Şifre değiştirilemedi. Lütfen tekrar deneyin.' : 'Failed to change password. Please try again.', 'error');
        }
    }
}

// 6. LOAD PROFILE
async function loadUserProfile() {
    let token = localStorage.getItem('triobank_token');
    
    // Eğer token yoksa, refresh token ile yeni token almayı dene
    if (!token) {
        const refreshed = await tryRefreshToken();
        if (refreshed) {
            token = localStorage.getItem('triobank_token');
        } else {
            // Refresh token da yoksa veya geçersizse, kullanıcı giriş yapmamış demektir
            return false;
        }
    }
    
    // If token is present and not expired locally, use it without needing refresh/call
    if (isTokenValid(token)) {
        const payload = parseJwt(token);
        // subject contains user UUID
        if (payload && (payload.sub || payload.subject)) {
            const userUUID = payload.sub || payload.subject;
            currentUser = { user_id: userUUID };
            // lazy-load: do not fetch client here; load when user opens profile
            return true;
        }
    }

    try {
        // Try /auth/me first (some deployments implement this)
        let res = await fetch(`${API_URL}/auth/me`, { 
            method: 'GET', 
            headers: { 
                'Authorization': `Bearer ${token}`
            }, 
            credentials: 'include' 
        });
        if (res.ok) {
            const user = await res.json();
            currentUser = user;
            updateProfileUI(user);
            // Attempt to load client info when we have user
            if (currentUser && (currentUser.user_id || currentUser.id || currentUser.UUID)) {
                // normalize user_id field
                currentUser.user_id = currentUser.user_id || currentUser.id || currentUser.UUID;
            }
            return true;
        }

        // Fallback: validate token to ensure session is valid (silent)
        res = await fetch(`${API_URL}/auth/validation`, { 
            method: 'POST', 
            headers: { 
                'Content-Type': 'application/json', 
                'Authorization': `Bearer ${token}`
            }, 
            credentials: 'include' 
        });
        if (res.ok) {
            const data = await res.json();
            // set minimal currentUser to avoid breaking UI
            currentUser = { user_id: data.user_id };
            // we don't have full profile fields, so only update small UI bits
            const ids = ['accountCardUserDisplay', 'menuUserNamePage', 'profileNameDisplay', 'specialMenuUserName', 'cardHolderName'];
            ids.forEach(id => {
                const el = document.getElementById(id);
                if (el && currentUser.user_id) el.innerText = (currentUser.user_id + '').substring(0, 12);
            });
            // lazy-load: do not fetch client here; load when user opens profile
            return true;
        }

        // invalid token -> try refresh once before giving up
        if (res.status === 401) {
            const refreshed = await tryRefreshToken();
            if (refreshed) {
                // retry with new token
                token = localStorage.getItem('triobank_token');
                const retry = await fetch(`${API_URL}/auth/validation`, { 
                    method: 'POST', 
                    headers: { 
                        'Content-Type': 'application/json', 
                        'Authorization': `Bearer ${token}`
                    }, 
                    credentials: 'include' 
                });
                    if (retry.ok) {
                    const data = await retry.json();
                    currentUser = { user_id: data.user_id };
                    // lazy-load: do not fetch client here
                    return true;
                }
            }
            // could not refresh - refresh token expired or invalid
            return false;
        }
    } catch (err) {
        console.error('Profil hatası (silent):', err);
        // Hata durumunda refresh token ile deneme yap (sadece bir kez)
        if (!token || err.message?.includes('401') || err.message?.includes('Unauthorized')) {
            const refreshed = await tryRefreshToken();
            if (refreshed) {
                // Yeni token ile tekrar dene (recursive call yerine direkt validation)
                token = localStorage.getItem('triobank_token');
                try {
                    const retry = await fetch(`${API_URL}/auth/validation`, { 
                        method: 'POST', 
                        headers: { 
                            'Content-Type': 'application/json', 
                            'Authorization': `Bearer ${token}`
                        }, 
                        credentials: 'include' 
                    });
                    if (retry.ok) {
                        const data = await retry.json();
                        currentUser = { user_id: data.user_id };
                        return true;
                    }
                } catch (retryErr) {
                    console.error('Retry after refresh failed:', retryErr);
                }
            }
        }
        return false;
    }
}

// --- CLIENT: load client info for current user ---
async function loadClientInfo() {
    if (!currentUser || !currentUser.user_id) {
        console.debug('loadClientInfo: no currentUser.user_id');
        return false;
    }
    // Retry loop: sometimes client-service creates client asynchronously after user registration
    const maxAttempts = 5;
    let attempt = 0;
    let delay = 500; // ms
    while (attempt < maxAttempts) {
        attempt += 1;
        try {
            console.debug('loadClientInfo: requesting client for user_id=', currentUser.user_id, 'attempt=', attempt);
            const res = await apiCall(`/clients/user/${currentUser.user_id}`, 'GET', null, true);
            console.debug('loadClientInfo: received', res);
            currentClient = res;
            updateClientUI(currentClient);
            return true;
        } catch (err) {
            console.warn('loadClientInfo attempt', attempt, 'failed:', err && err.message ? err.message : err);

            // perform a light direct fetch to inspect status when debugging
            try {
                const token = localStorage.getItem('triobank_token');
                const headers = { 'Content-Type': 'application/json' };
                if (token) headers['Authorization'] = `Bearer ${token}`;
                const f = await fetch(`${API_URL}/clients/user/${currentUser.user_id}`, { method: 'GET', headers, credentials: 'include' });
                const text = await f.text();
                console.debug('Direct fetch to /clients/user response status=', f.status, 'body=', text);
                // if server explicitly returns 404, try again after waiting (client may be created asynchronously)
                if (f.status === 404) {
                    // if last attempt, show info message
                    if (attempt >= maxAttempts) {
                        showToast(currentLang === 'tr' ? 'Müşteri bilgisi bulunamadı.' : 'Client record not found.', 'info');
                        return false;
                    }
                    // wait and retry
                    await sleep(delay);
                    delay *= 2;
                    continue;
                }

                // For other statuses, break and surface error
                showToast(currentLang === 'tr' ? `Müşteri yüklenemedi (durum: ${f.status}). Konsolu kontrol edin.` : `Client load failed (status: ${f.status}). Check console.`, 'error');
                return false;
            } catch (e) {
                console.error('Direct fetch diagnostic failed:', e);
                // wait briefly and retry
                if (attempt < maxAttempts) await sleep(delay);
                delay *= 2;
            }
        }
    }
    return false;
}

// Update small profile display used in header/sidebar when on dashboard pages
function updateProfileDisplay() {
    // Update basic profile fields
    try {
        if (currentUser) updateProfileUI(currentUser);

        // If we already have client info, populate UI; otherwise attempt to load it
        if (currentClient) {
            updateClientUI(currentClient);
        }

        // Update balance display as precaution
        updateBalanceUI();
    } catch (e) {
        console.error('updateProfileDisplay error:', e);
    }
}

// --- CLIENT: create client record from profile fields (manual trigger) ---
async function createClientFromProfile() {
    // Build payload from available profile inputs
    const name = document.getElementById('profileName') ? document.getElementById('profileName').value.trim() : '';
    const email = document.getElementById('profileEmail') ? document.getElementById('profileEmail').value.trim() : '';
    const tc = document.getElementById('profileTC') ? document.getElementById('profileTC').value.replace(/\D/g, '') : '';
    const gsm = document.getElementById('profilePhone') ? document.getElementById('profilePhone').value.replace(/\D/g, '') : '';
    const addr = document.getElementById('profileAddress') ? document.getElementById('profileAddress').value.trim() : '';

    if (!tc || tc.length !== 11) {
        showToast('Lütfen geçerli 11 haneli T.C. girin.', 'error');
        return;
    }

    const payload = {
        user_id: currentUser && currentUser.user_id ? currentUser.user_id : undefined,
        tc_no: tc,
        first_name: (name.split(' ')[0] || name) ,
        last_name: (name.split(' ').slice(1).join(' ') || ''),
        email: email,
        gsm: gsm,
        address: { street: addr }
    };

    try {
        const res = await apiCall('/clients', 'POST', payload, true);
        // Expect created client object
        currentClient = res;
        updateClientUI(currentClient);
        showToast('Müşteri kaydı oluşturuldu.', 'success');
        // hide create button
        const btn = document.getElementById('create-client-btn'); if (btn) btn.style.display = 'none';
    } catch (err) {
        console.error('createClientFromProfile error', err);
    }
}

function updateBalanceUI() {
    try {
        const mobileEl = document.getElementById('total-balance-mobile');
        const detailEl = document.getElementById('acc-detail-balance');
        const text = (typeof userBalance === 'number') ? userBalance.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : (userBalance || '0.00');
        if (mobileEl) mobileEl.innerText = text;
        if (detailEl) detailEl.innerText = text;
    } catch (e) {
        console.error('updateBalanceUI error:', e);
    }
}

// --- CLIENT: update client info (first update auth user contact, then client record)
async function updateClientInfo() {
    if (!currentClient) {
        showToast(currentLang === 'tr' ? 'Müşteri bilgisi yüklenemedi.' : 'Client info not loaded.', 'error');
        return;
    }

    // Only allow updating phone and address from frontend
    const phone = document.getElementById('profilePhone') ? document.getElementById('profilePhone').value.replace(/\D/g, '') : '';
    const address = document.getElementById('profileAddress') ? document.getElementById('profileAddress').value.trim() : '';

    if (!phone) { showToast(currentLang === 'tr' ? 'Telefon boş olamaz.' : 'Phone cannot be empty.', 'error'); return; }

    try {
        // First, update auth service contact (only phone)
        try {
            await apiCall('/auth/user/update', 'POST', { phone }, true);
        } catch (e) {
            showToast(currentLang === 'tr' ? 'Auth servisinde telefon güncellemesi başarısız.' : 'Auth phone update failed.', 'error');
            return;
        }

        // Keep previous email to detect accidental truncation from server
        const prevEmail = currentClient && (currentClient.email || currentClient.email_address || '') || '';

        // Then update client service record (do not send email)
        const payload = { gsm: phone, address: { street: address } };
        const updated = await apiCall(`/clients/${currentClient.id}`, 'PUT', payload, true);

        // Basic sanity check: if server returns an obviously-truncated email (like single char), recover and re-fetch
        const newEmail = updated && (updated.email || updated.email_address || '');
        if (newEmail && newEmail.length <= 1 && prevEmail && prevEmail.length > 1) {
            console.warn('updateClientInfo: suspicious email from server', { prevEmail, newEmail, clientId: currentClient.id });
            showToast(currentLang === 'tr' ? 'Sunucudan gelen e-posta verisi beklenmedik. Mevcut e-posta korunuyor, sunucudan tekrar çekiliyor.' : 'Unexpected email received from server. Keeping previous email and re-fetching client.', 'error');
            // revert email locally; do not fetch automatically (lazy-load on profile open)
            updated.email = prevEmail;
            currentClient = updated;
            updateClientUI(currentClient);
            return;
        }

        // Otherwise accept update
        currentClient = updated;
        updateClientUI(currentClient);
        showToast(currentLang === 'tr' ? 'Bilgiler başarıyla güncellendi.' : 'Client info updated.', 'success');
    } catch (err) {
        // apiCall will show error toast
    }
}

function updateProfileUI(user) {
    if (!user) return;
    const fullName = `${user.name} ${user.surname}`.toUpperCase();

    // İsim alanlarını güncelle
    const ids = ['accountCardUserDisplay', 'menuUserNamePage', 'profileNameDisplay', 'specialMenuUserName', 'cardHolderName'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.innerText = fullName;
    });

    // Profil formunu güncelle
    if (document.getElementById('profileName')) document.getElementById('profileName').value = fullName;
    if (document.getElementById('profileEmail')) document.getElementById('profileEmail').value = user.email || '';
    if (document.getElementById('profilePhone')) document.getElementById('profilePhone').value = user.tel || '';
    if (document.getElementById('profileTC')) document.getElementById('profileTC').value = user.tc || '';
}

function updateClientUI(client) {
    if (!client) return;

    // Normalize gsm and address
    const gsm = client.gsm || client.phone || client.tel || '';
    let street = '', city = '', district = '', postal = '';
    const addr = client.address || client.adress || client.addresses || '';
    if (typeof addr === 'string') {
        const parts = addr.split('|').map(s => s.trim());
        street = parts[0] || '';
        city = parts[1] || '';
        district = parts[2] || '';
        postal = parts[3] || '';
    } else if (typeof addr === 'object' && addr !== null) {
        street = addr.street || addr.address || '';
        city = addr.city || '';
        district = addr.district || '';
        postal = addr.postal_code || addr.postal || '';
    }

    // Fill profile inputs if they exist
    const elPhone = document.getElementById('profilePhone');
    if (elPhone) elPhone.value = gsm;
    const elAddress = document.getElementById('profileAddress'); if (elAddress) elAddress.value = (street || city || district || postal) ? `${street}${city ? ', ' + city : ''}${district ? ', ' + district : ''}${postal ? ' ' + postal : ''}`.trim() : (typeof addr === 'string' ? addr : '');

    // Also fill account page inputs (if present)
    const aPhone = document.getElementById('accProfilePhone'); if (aPhone) aPhone.value = gsm;
    const aAddress = document.getElementById('accProfileAddress'); if (aAddress) aAddress.value = (street || city || district || postal) ? `${street}${city ? ', ' + city : ''}${district ? ', ' + district : ''}${postal ? ' ' + postal : ''}`.trim() : (typeof addr === 'string' ? addr : '');

    // Update sidebar small profile widget
    const sbName = document.getElementById('sidebarProfileName');
    const sbPhone = document.getElementById('sidebarProfilePhone');
    if (sbPhone) sbPhone.innerText = gsm || '';
    let displayName = '';
    if (sbName) {
        const first = client.first_name || client.firstName || client.name || '';
        const last = client.last_name || client.lastName || client.surname || client.lastName || '';
        displayName = ((first || last) ? (first + (last ? ' ' + last : '')) : (currentUser && (currentUser.name || currentUser.fullname) ? (currentUser.name + (currentUser.surname ? ' ' + currentUser.surname : '')) : ''));
        sbName.innerText = displayName || '';
    }
    // populate profile name field too
    const profileNameEl = document.getElementById('profileName');
    const profileNameDisplay = document.getElementById('profileNameDisplay');
    if (profileNameEl) profileNameEl.value = (displayName || '').toUpperCase();
    if (profileNameDisplay) profileNameDisplay.innerText = (displayName || '').toUpperCase();
        // Populate email and TC if available from client record (read-only fields)
        const pEmail = document.getElementById('profileEmail');
        const pTC = document.getElementById('profileTC');
        if (pEmail) pEmail.value = client.email || client.email_address || client.mail || '';
        if (pTC) pTC.value = client.tc_no || client.tc || client.tcNo || '';
}
// Old updateProfileDisplay removed

function updateClientInfoFromAccounts() {
    // reuse profile form values from accounts card
    const gsm = document.getElementById('accProfilePhone').value.trim();
    const address = document.getElementById('accProfileAddress').value.trim();
    // mirror into main profile inputs so updateClientInfo() picks them up
    const profPhone = document.getElementById('profilePhone'); if (profPhone) profPhone.value = gsm;
    const profAddr = document.getElementById('profileAddress'); if (profAddr) profAddr.value = address;
    updateClientInfo();
}

// --- ACCOUNTS: list, detail, create, close ---

/**
 * Update account balance in state after a transaction
 * @param {string} accountId - Account ID to update
 * @param {number} newBalance - New balance amount
 * @param {number} newAvailableBalance - New available balance amount
 */
function updateAccountBalanceInState(accountId, newBalance, newAvailableBalance) {
    if (!window.accountsState || !window.accountsState.accounts) {
        console.warn('Accounts state not initialized, cannot update balance');
        return;
    }
    
    const account = window.accountsState.accounts.find(acc => acc.id === accountId);
    if (account) {
        account.balance = newBalance !== undefined ? newBalance : account.balance;
        account.availableBalance = newAvailableBalance !== undefined ? newAvailableBalance : account.availableBalance;
        console.debug(`Updated balance for account ${accountId}:`, { balance: account.balance, availableBalance: account.availableBalance });
        
        // Update window.currentAccounts for backward compatibility
        window.currentAccounts = window.accountsState.accounts;
        
        // Re-render accounts list if visible
        if (document.getElementById('accountsList') || document.querySelector('.acc-list')) {
            renderAccountsList(window.accountsState.accounts);
        }
    } else {
        console.warn(`Account ${accountId} not found in state`);
    }
}

/**
 * Update account balances after a transfer
 * @param {string} fromAccountId - Source account ID
 * @param {string} toAccountId - Destination account ID (can be IBAN)
 * @param {number} amount - Transfer amount
 */
function updateBalancesAfterTransfer(fromAccountId, toAccountId, amount) {
    if (!window.accountsState || !window.accountsState.accounts) {
        console.warn('Accounts state not initialized, cannot update balances');
        return;
    }
    
    // Update from account (subtract amount)
    const fromAccount = window.accountsState.accounts.find(acc => acc.id === fromAccountId);
    if (fromAccount) {
        const currentBalance = parseFloat(fromAccount.balance || 0);
        const currentAvailableBalance = parseFloat(fromAccount.availableBalance || 0);
        
        fromAccount.balance = Math.max(0, currentBalance - amount);
        fromAccount.availableBalance = Math.max(0, currentAvailableBalance - amount);
        
        console.debug(`Updated from account ${fromAccountId}:`, { 
            oldBalance: currentBalance, 
            newBalance: fromAccount.balance,
            oldAvailableBalance: currentAvailableBalance,
            newAvailableBalance: fromAccount.availableBalance
        });
    }
    
    // Update to account (add amount) - can be by ID or IBAN
    const toAccount = window.accountsState.accounts.find(acc => 
        acc.id === toAccountId || acc.accountNumber === toAccountId
    );
    if (toAccount) {
        const currentBalance = parseFloat(toAccount.balance || 0);
        const currentAvailableBalance = parseFloat(toAccount.availableBalance || 0);
        
        toAccount.balance = currentBalance + amount;
        toAccount.availableBalance = currentAvailableBalance + amount;
        
        console.debug(`Updated to account ${toAccountId}:`, { 
            oldBalance: currentBalance, 
            newBalance: toAccount.balance,
            oldAvailableBalance: currentAvailableBalance,
            newAvailableBalance: toAccount.availableBalance
        });
    } else {
        console.debug(`To account ${toAccountId} not found in state (might be external account)`);
    }
    
    // Update window.currentAccounts for backward compatibility
    window.currentAccounts = window.accountsState.accounts;
    
    // Re-render accounts list if visible
    if (document.getElementById('accountsList') || document.querySelector('.acc-list')) {
        renderAccountsList(window.accountsState.accounts);
    }
}

async function loadAccounts(forceRefresh = false) {
    if (!currentUser || !currentUser.user_id) {
        showToast(currentLang === 'tr' ? 'Oturum bulunamadı. Lütfen giriş yapın.' : 'No active session. Please login.', 'error');
        return;
    }

    // If state exists and not forcing refresh, return cached accounts
    if (!forceRefresh && window.accountsState && window.accountsState.accounts && window.accountsState.accounts.length > 0) {
        console.debug('loadAccounts: Using cached accounts from state');
        window.currentAccounts = window.accountsState.accounts;
        renderAccountsList(window.accountsState.accounts);
        return;
    }

    // Prevent concurrent loads
    if (window.accountsState && window.accountsState.isLoading) {
        console.debug('loadAccounts: Already loading, skipping...');
        return;
    }

    try {
        if (window.accountsState) {
            window.accountsState.isLoading = true;
        }
        // CRITICAL: Client ID must be loaded first because Account Service uses client.ID as customerId
        // If currentClient is not loaded, load it first
        if (!currentClient || (!currentClient.id && !currentClient.ID)) {
            console.debug('loadAccounts: currentClient not loaded, loading client info first...');
            await loadClientInfo();
        }

        // Prefer using client ID (created by client-service) as customerId.
        // Account Service stores accounts with client.ID as customerId, NOT user_id
        const customerId = (currentClient && (currentClient.id || currentClient.ID)) ? (currentClient.id || currentClient.ID) : currentUser.user_id;
        
        if (!customerId) {
            showToast(currentLang === 'tr' ? 'Müşteri bilgisi bulunamadı.' : 'Client information not found.', 'error');
            if (window.accountsState) {
                window.accountsState.isLoading = false;
            }
            return;
        }

        console.debug('loadAccounts: using customerId=', customerId);
        // GET accounts for customer - Sadece ACTIVE hesapları iste
        const res = await apiCall(`/v1/accounts?customerId=${customerId}&status=ACTIVE`, 'GET', null, true);
        // assume res is array or { items: [...] }
        const accounts = Array.isArray(res) ? res : (res.items || []);
        // Ekstra güvenlik: Frontend'de de ACTIVE olmayanları filtrele
        const activeAccounts = accounts.filter(acc => (acc.status || '').toUpperCase() === 'ACTIVE');
        console.debug('loadAccounts: received', activeAccounts.length, 'active accounts');
        
        // Her hesap için Ledger Service'den bakiye bilgisini çek
        const accountsWithBalance = await Promise.all(activeAccounts.map(async (acc) => {
            const accountId = acc.id || acc.accountId || acc.account_id;
            if (!accountId) {
                console.warn('Account without ID found:', acc);
                return { ...acc, balance: 0, availableBalance: 0 };
            }
            
            try {
                // Gateway üzerinden Ledger Service'den bakiye çek
                const balanceRes = await apiCall(`/api/v1/ledger/balances/${accountId}`, 'GET', null, true);
                console.debug(`Balance for account ${accountId}:`, balanceRes);
                
                // Bakiye bilgisini account objesine ekle
                return {
                    ...acc,
                    balance: balanceRes.balance || balanceRes.availableBalance || 0,
                    availableBalance: balanceRes.availableBalance || balanceRes.balance || 0,
                    currency: balanceRes.currency || acc.currency || 'TRY'
                };
            } catch (balanceErr) {
                console.warn(`Failed to fetch balance for account ${accountId}:`, balanceErr);
                // Bakiye çekilemezse 0 olarak ayarla
                return {
                    ...acc,
                    balance: 0,
                    availableBalance: 0,
                    currency: acc.currency || 'TRY'
                };
            }
        }));
        
        console.debug('loadAccounts: accounts with balance:', accountsWithBalance);
        // Store accounts in global state
        window.accountsState.accounts = accountsWithBalance;
        window.accountsState.lastUpdated = Date.now();
        window.accountsState.isLoading = false;
        // Keep window.currentAccounts for backward compatibility
        window.currentAccounts = accountsWithBalance;
        renderAccountsList(accountsWithBalance);
    } catch (err) {
        console.error('loadAccounts error:', err);
        // Show user-friendly error message
        if (err.message && err.message.includes('404')) {
            showToast(currentLang === 'tr' ? 'Hesap bulunamadı. Müşteri bilgisi yükleniyor olabilir.' : 'Account not found. Client information may be loading.', 'info');
        }
    }
}

function renderAccountsList(accounts) {
    const container = document.getElementById('accountsList') || document.querySelector('.acc-list');
    if (!container) return;
    container.innerHTML = '';

    if (!accounts || accounts.length === 0) {
        container.innerHTML = '<div class="acc-item"><div class="acc-info"><h3>Hesap bulunamadı</h3><p>Kayıtlı hesap bulunmuyor.</p></div></div>';
        return;
    }

    accounts.forEach(acc => {
        const accId = acc.id || acc.accountId || acc.account_id || acc.accountNumber || '';
        // Bakiye öncelik sırası: availableBalance > balance > 0
        const balance = (acc.availableBalance !== undefined && acc.availableBalance !== null) 
            ? Number(acc.availableBalance) 
            : ((acc.balance !== undefined && acc.balance !== null) 
                ? Number(acc.balance) 
                : ((acc.available_balance !== undefined && acc.available_balance !== null) 
                    ? Number(acc.available_balance) 
                    : 0));
        const currency = acc.currency || acc.ccy || 'TRY';
        const title = acc.type || acc.product || (currency === 'TRY' ? 'Vadesiz TL Hesabı' : (acc.name || 'Hesap'));
        const iban = acc.iban || acc.Iban || acc.accountNumber || '';

    const safeId = ('acc-' + (accId || Math.random().toString(36).slice(2))).replace(/[^a-zA-Z0-9-_]/g, '-');
    const item = document.createElement('div');
        item.className = 'acc-item';
        item.innerHTML = `
            <div class="acc-info">
                <h3 style="margin-bottom:5px;">${title}</h3>
                <p style="font-size:1.1rem; font-weight:600; margin:0;"> <span id="balance-${safeId}">${(typeof balance === 'number') ? balance.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : balance} ${currency}</span></p>
                <small class="dynamic-iban" style="opacity:0.8; font-family:monospace; font-size:0.8rem; display:block;">${iban}</small>
                <div style="margin-top:8px; display:flex; gap:8px; flex-wrap: wrap;">
                    <button class="btn small" onclick="viewAccountDetails('${accId}')">Detay</button>
                    <button class="btn small" onclick="showAccountStatementModal('${accId}')" style="background: #1976d2; color: white; border: none;">
                        <i class="fas fa-history" style="margin-right: 4px;"></i>Hesap Hareketleri
                    </button>
                    <button class="btn small secondary" onclick="closeAccount('${accId}')">Hesabı Kapat</button>
                </div>
            </div>
        `;
        container.appendChild(item);

        // Fetch balance asynchronously and update UI (non-blocking)
        (async () => {
            try {
                const bal = await fetchBalance(accId);
                const el = document.getElementById(`balance-${safeId}`);
                if (el && bal) {
                    el.innerText = (typeof bal.availableBalance === 'number' ? bal.availableBalance.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : bal.balance) + ' ' + (bal.currency || currency);
                }
            } catch (e) {
                console.debug('fetchBalance failed for', accId, e);
            }
        })();
    });
}

// Safe helper to build query strings
function buildQuery(params) {
    const esc = encodeURIComponent;
    return Object.keys(params).filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '').map(k => `${esc(k)}=${esc(params[k])}`).join('&');
}

// Get customer id to use when calling services that require customerId (prefer client.id created by client-service)
function getCustomerId() {
    if (currentClient && (currentClient.id || currentClient.ID)) return (currentClient.id || currentClient.ID);
    if (currentUser && currentUser.user_id) return currentUser.user_id;
    return null;
}

// --- LEDGER: fetch balance and statements ---
async function fetchBalance(accountId) {
    if (!accountId) throw new Error('accountId required');
    // GET /api/v1/ledger/balances/{accountId}
    const endpoint = `/api/v1/ledger/balances/${accountId}`;
    const res = await apiCall(endpoint, 'GET', null, true);
    // Expect BalanceResponse
    return res;
}

// Fetch statement with options
async function fetchStatement(accountId, opts = {}) {
    if (!accountId) throw new Error('accountId required');
    const q = {};
    if (opts.startDate) q.startDate = opts.startDate; // YYYY-MM-DD
    if (opts.endDate) q.endDate = opts.endDate;
    if (opts.type) q.type = opts.type; // DEBIT|CREDIT
    if (opts.keyword) q.keyword = opts.keyword;
    q.page = opts.page != null ? opts.page : 0;
    q.size = opts.size != null ? opts.size : 20;
    if (opts.includeRunningBalance != null) q.includeRunningBalance = opts.includeRunningBalance;

    const query = buildQuery(q);
    const endpoint = `/api/v1/ledger/accounts/${accountId}/statement${query ? '?' + query : ''}`;
    const res = await apiCall(endpoint, 'GET', null, true);
    return res;
}

// --- CARDS: list, get, issue (debit/virtual), block, activate ---
async function loadCards(opts = {}) {
    // opts: { customerId, accountId, cardTypes: ['DEBIT','VIRTUAL'] }
    const q = {};
    if (opts.customerId) q.customerId = opts.customerId;
    if (opts.accountId) q.accountId = opts.accountId;
    if (opts.cardTypes && Array.isArray(opts.cardTypes) && opts.cardTypes.length) q.cardType = opts.cardTypes.join(',');

    const query = buildQuery(q);
    const endpoint = `/v1/cards${query ? '?' + query : ''}`;
    const res = await apiCall(endpoint, 'GET', null, true);
    // Expect array of CardResponse
    let cards = Array.isArray(res) ? res : (res.items || res || []);
    
    // CRITICAL: Sanal kartlar için sadece ACTIVE olanları göster (diğerleri DB'de log için kalır)
    // Diğer kart türleri için tüm status'leri göster
    cards = cards.filter(c => {
        const cardType = c.cardType || c.type || '';
        const status = c.status || '';
        // Sanal kart ise sadece ACTIVE olanları göster
        if (cardType === 'VIRTUAL' || cardType === 'SANAL') {
            return status === 'ACTIVE';
        }
        // Diğer kartlar için tüm status'leri göster
        return true;
    });
    
    // Apply current filter
    if (currentCardTypeFilter !== 'all') {
        cards = cards.filter(c => {
            const cardType = c.cardType || c.type || '';
            return cardType === currentCardTypeFilter;
        });
    }
    
    return cards;
}

async function getCard(cardId) {
    if (!cardId) throw new Error('cardId required');
    const res = await apiCall(`/v1/cards/${cardId}`, 'GET', null, true);
    return res;
}

async function issueDebitCard(payload) {
    // payload: { accountId, cardholderName, dailyWithdrawalLimit?, atmEnabled?, pin? }
    if (!payload || !payload.accountId) throw new Error('accountId required to issue debit card');
    const res = await apiCall('/v1/cards/debit', 'POST', payload, true);
    showToast(currentLang === 'tr' ? 'Banka kartı talebi gönderildi.' : 'Debit card request sent.', 'success');
    return res;
}

async function issueVirtualCard(payload) {
    // payload: { accountId, cardholderName, onlineOnly?, singleUse?, singleUseValidityHours?, usageRestriction? }
    if (!payload || !payload.accountId) throw new Error('accountId required to issue virtual card');
    const res = await apiCall('/v1/cards/virtual', 'POST', payload, true);
    showToast(currentLang === 'tr' ? 'Sanal kart oluşturuldu.' : 'Virtual card created.', 'success');
    return res;
}

async function blockCard(cardId, reason = 'user_request') {
    if (!cardId) throw new Error('cardId required');
    const endpoint = `/v1/cards/${encodeURIComponent(cardId)}/block?reason=${encodeURIComponent(reason)}`;
    await apiCall(endpoint, 'PATCH', null, true);
    
    // Sanal kart silme için farklı mesaj
    if (reason === 'user_deleted') {
        showToast(currentLang === 'tr' ? 'Sanal kart silindi.' : 'Virtual card deleted.', 'success');
    } else {
    showToast(currentLang === 'tr' ? 'Kart bloke edildi.' : 'Card blocked.', 'success');
    }
}

// Show confirmation modal (Evet/Hayır)
function showConfirmModal(title, message, onConfirm, onCancel = null) {
    const modal = document.createElement('div');
    modal.style.position = 'fixed';
    modal.style.left = '0';
    modal.style.top = '0';
    modal.style.width = '100%';
    modal.style.height = '100%';
    modal.style.background = 'rgba(0,0,0,0.5)';
    modal.style.display = 'flex';
    modal.style.alignItems = 'center';
    modal.style.justifyContent = 'center';
    modal.style.zIndex = '9999';
    
    const box = document.createElement('div');
    box.style.background = 'white';
    box.style.color = '#222';
    box.style.padding = '24px';
    box.style.borderRadius = '12px';
    box.style.width = '90%';
    box.style.maxWidth = '400px';
    box.style.boxShadow = '0 8px 24px rgba(0,0,0,0.2)';
    
    box.innerHTML = `
        <div style="text-align: center; margin-bottom: 20px;">
            <i class="fas fa-exclamation-triangle" style="font-size: 3rem; color: #ff9800; margin-bottom: 12px;"></i>
            <h3 style="margin: 0 0 8px 0; font-size: 1.2rem; color: #222;">${title}</h3>
            <p style="margin: 0; color: #666; font-size: 0.95rem; line-height: 1.5;">${message}</p>
        </div>
        <div style="display: flex; gap: 12px; justify-content: center; margin-top: 24px;">
            <button class="btn secondary" id="confirm-cancel" style="flex: 1; padding: 12px; font-size: 1rem;">
                ${currentLang === 'tr' ? 'Hayır' : 'No'}
            </button>
            <button class="btn primary" id="confirm-ok" style="flex: 1; padding: 12px; font-size: 1rem; background: #ff4444;">
                ${currentLang === 'tr' ? 'Evet' : 'Yes'}
            </button>
        </div>
    `;
    
    modal.appendChild(box);
    document.body.appendChild(modal);
    
    // Close on background click
    modal.onclick = (e) => {
        if (e.target === modal) {
            modal.remove();
            if (onCancel) onCancel();
        }
    };
    
    // Cancel button
    document.getElementById('confirm-cancel').addEventListener('click', () => {
        modal.remove();
        if (onCancel) onCancel();
    });
    
    // Confirm button
    document.getElementById('confirm-ok').addEventListener('click', () => {
        modal.remove();
        if (onConfirm) onConfirm();
    });
}

async function activateCard(cardId) {
    if (!cardId) throw new Error('cardId required');
    const endpoint = `/v1/cards/${encodeURIComponent(cardId)}/activate`;
    await apiCall(endpoint, 'PATCH', null, true);
    showToast(currentLang === 'tr' ? 'Kart aktifleştirildi.' : 'Card activated.', 'success');
}

// Render card list into #cardsList
function renderCardsList(cards) {
    const container = document.getElementById('cardsList');
    if (!container) return;
    container.innerHTML = '';

    if (!cards || cards.length === 0) {
        container.innerHTML = `<div class="card text-contrast-fix" style="padding:20px; text-align:center;">${translations[currentLang].noCard || 'No cards found.'}</div>`;
        return;
    }

    cards.forEach(c => {
        const id = c.id || c.ID || '';
        const num = c.number || '';
        const typ = c.cardType || c.type || '';
        const status = c.status || '';
        const cardholderName = c.cardholderName || 'UNDEFINED UNDEFINED';
        const expiryMonth = c.expiryMonth || 12;
        const expiryYear = c.expiryYear || 2028;
        const cardBrand = c.cardBrand || 'VISA';
        
        // Kart tipine göre renk belirle
        let cardBgColor = '#1a1a1a'; // Default dark
        let cardTextColor = '#ffffff';
        let cardAccentColor = '#ff4444'; // Default red
        
        if (typ === 'DEBIT' || typ === 'BANKA') {
            // Banka kartı - Mavi tonları
            cardBgColor = 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)';
            cardAccentColor = '#4a90e2';
        } else if (typ === 'VIRTUAL' || typ === 'SANAL') {
            // Sanal kart - Mor tonları
            cardBgColor = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
            cardAccentColor = '#9b59b6';
        } else if (typ === 'CREDIT' || typ === 'KREDI') {
            // Kredi kartı - Altın tonları
            cardBgColor = 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)';
            cardAccentColor = '#ff6b6b';
        }

        // Modal mesajları hazırla
        const isVirtual = (typ === 'VIRTUAL' || typ === 'SANAL');
        const modalTitle = isVirtual 
            ? (currentLang === 'tr' ? 'Sanal Kartı Sil' : 'Delete Virtual Card')
            : (currentLang === 'tr' ? 'Kartı Bloke Et' : 'Block Card');
        const modalMessage = isVirtual
            ? (currentLang === 'tr' ? 'Bu sanal kartı silmek istediğinize emin misiniz?' : 'Are you sure you want to delete this virtual card?')
            : (currentLang === 'tr' ? 'Bu kartı bloke etmek istediğinize emin misiniz?' : 'Are you sure you want to block this card?');
        const buttonText = isVirtual 
            ? (currentLang === 'tr' ? 'Sil' : 'Delete')
            : (currentLang === 'tr' ? 'Bloke' : 'Block');
        const actionReason = isVirtual ? 'user_deleted' : 'user_request';

        const item = document.createElement('div');
        item.style.marginBottom = '16px';
        item.style.borderRadius = '12px';
        item.style.overflow = 'hidden';
        item.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';

        item.innerHTML = `
            <div style="background: ${cardBgColor}; color: ${cardTextColor}; padding: 20px; position: relative; min-height: 180px;">
                <!-- Card Header -->
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div style="width: 8px; height: 8px; background: ${cardAccentColor}; border-radius: 50%;"></div>
                        <div style="font-size: 0.75rem; opacity: 0.9;">${cardBrand}</div>
            </div>
                    <div style="font-weight: 700; font-size: 1.1rem; letter-spacing: 1px;">TRIO</div>
                </div>

                <!-- Card Number -->
                <div style="font-family: 'Courier New', monospace; font-size: 1.3rem; font-weight: 600; letter-spacing: 2px; margin-bottom: 25px; word-spacing: 8px;">
                    ${num || '**** **** **** ****'}
                </div>

                <!-- Card Footer -->
                <div style="display: flex; justify-content: space-between; align-items: flex-end;">
                    <div style="flex: 1;">
                        <div style="font-size: 0.7rem; opacity: 0.8; margin-bottom: 4px;">KART SAHİBİ</div>
                        <div style="font-size: 0.9rem; font-weight: 600; text-transform: uppercase;">${cardholderName}</div>
                    </div>
                    <div style="display: flex; gap: 20px; align-items: flex-end;">
                        ${(typ !== 'DEBIT' && typ !== 'BANKA') ? `
                        <div>
                            <div style="font-size: 0.7rem; opacity: 0.8; margin-bottom: 4px;">SKT</div>
                            <div style="font-size: 0.9rem; font-weight: 600;">${String(expiryMonth).padStart(2, '0')}/${String(expiryYear).slice(-2)}</div>
                        </div>
                        ` : ''}
                    </div>
                </div>
            </div>

            <!-- Card Info and Actions -->
            <div style="background: var(--card-bg, white); padding: 12px; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(0,0,0,0.1);">
                <div style="flex: 1;">
                    <div style="font-weight: 600; font-size: 0.9rem; color: var(--text-primary, #222);">
                        ${typ === 'DEBIT' ? 'Banka Kartı' : typ === 'VIRTUAL' ? 'Sanal Kart' : typ === 'CREDIT' ? 'Kredi Kartı' : typ}
                    </div>
                    <div style="font-size: 0.75rem; opacity: 0.7; margin-top: 2px; color: var(--text-secondary, #666);">
                        ${status === 'ACTIVE' ? 'Aktif' : status === 'BLOCKED' ? 'Bloke' : status === 'EXPIRED' ? 'Süresi Dolmuş' : status}
                    </div>
                </div>
                <div style="display: flex; gap: 8px; margin-left: 12px;">
                    <button class="btn small" onclick="(function(id){ showCardDetails(id); })('${id}')" style="font-size: 0.8rem; padding: 6px 12px;">Detay</button>
                    ${status === 'ACTIVE' ? 
                        `<button class="btn small" onclick="(function(id, title, msg, reason){ showConfirmModal(title, msg, () => { blockCard(id, reason).then(() => loadCards({ customerId: getCustomerId() }).then(renderCardsList)).catch(err => console.error('Card action error:', err)); }); })('${id}', '${modalTitle}', '${modalMessage}', '${actionReason}')" style="font-size: 0.8rem; padding: 6px 12px; background: #ff4444; color: white;">${buttonText}</button>` :
                        `<button class="btn small secondary" onclick="(function(id){ activateCard(id).then(()=>loadCards({ customerId: getCustomerId() }).then(renderCardsList)); })('${id}')" style="font-size: 0.8rem; padding: 6px 12px;">Aktifleştir</button>`
                    }
                </div>
            </div>
        `;

        container.appendChild(item);
    });
}

// Show card detail modal
async function showCardDetails(cardId) {
    if (!cardId) return;
    try {
        const detail = await getCard(cardId);
        
        // Kart tipine göre renk belirle
        const cardType = detail.cardType || detail.type || '';
        let cardBgColor = '#1a1a1a';
        let cardTextColor = '#ffffff';
        let cardAccentColor = '#ff4444';
        
        if (cardType === 'DEBIT' || cardType === 'BANKA') {
            cardBgColor = 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)';
            cardAccentColor = '#4a90e2';
        } else if (cardType === 'VIRTUAL' || cardType === 'SANAL') {
            cardBgColor = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
            cardAccentColor = '#9b59b6';
        } else if (cardType === 'CREDIT' || cardType === 'KREDI') {
            cardBgColor = 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)';
            cardAccentColor = '#ff6b6b';
        }
        
        // Veri hazırlama
        const number = detail.number || '**** **** **** ****';
        const cardholderName = detail.cardholderName || 'N/A';
        const expiryMonth = detail.expiryMonth || 12;
        const expiryYear = detail.expiryYear || 2028;
        const cardBrand = detail.cardBrand || 'VISA';
        const status = detail.status || 'UNKNOWN';
        const accountId = detail.accountId || detail.accountID || 'N/A';
        
        // Tarihlerden saati kaldır (sadece tarih)
        const createdAt = detail.createdAt 
            ? new Date(detail.createdAt).toLocaleDateString('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' })
            : 'N/A';
        const blockedAt = detail.blockedAt 
            ? new Date(detail.blockedAt).toLocaleDateString('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' })
            : null;
        const blockReason = detail.blockReason || '';
        
        
        // Account detayını çek (IBAN için)
        let accountNumber = 'N/A';
        try {
            if (accountId && accountId !== 'N/A') {
                const accountDetail = await apiCall(`/v1/accounts/${accountId}`, 'GET', null, true);
                if (accountDetail && accountDetail.accountNumber) {
                    // IBAN'ın son 16 hanesini al ve 4-8-4 formatında göster
                    const iban = accountDetail.accountNumber.replace(/\s/g, ''); // Boşlukları kaldır
                    const last16 = iban.slice(-16); // Son 16 hane
                    if (last16.length === 16) {
                        accountNumber = `${last16.slice(0, 4)}-${last16.slice(4, 12)}-${last16.slice(12, 16)}`;
                    } else {
                        accountNumber = last16; // 16 hane değilse olduğu gibi göster
                    }
                }
            }
        } catch (err) {
            console.warn('Failed to fetch account details for IBAN:', err);
            accountNumber = 'N/A';
        }
        
        // Status badge
        let statusBadge = '';
        let statusColor = '#666';
        if (status === 'ACTIVE') {
            statusBadge = 'Aktif';
            statusColor = '#4caf50';
        } else if (status === 'BLOCKED') {
            statusBadge = 'Bloke';
            statusColor = '#f44336';
        } else if (status === 'EXPIRED') {
            statusBadge = 'Süresi Dolmuş';
            statusColor = '#ff9800';
        } else if (status === 'CANCELLED') {
            statusBadge = 'İptal Edilmiş';
            statusColor = '#9e9e9e';
        } else {
            statusBadge = status;
        }
        
        // Debit Card özel alanlar
        let debitSpecificHTML = '';
        if (cardType === 'DEBIT' || cardType === 'BANKA') {
            const dailyLimit = detail.dailyWithdrawalLimit !== undefined && detail.dailyWithdrawalLimit !== null 
                ? parseFloat(detail.dailyWithdrawalLimit).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ₺'
                : 'Belirtilmemiş';
            const atmEnabled = detail.atmEnabled !== undefined && detail.atmEnabled !== null 
                ? (detail.atmEnabled ? 'Evet' : 'Hayır')
                : 'Belirtilmemiş';
            
            debitSpecificHTML = `
                <div style="background: #f5f5f5; padding: 16px; border-radius: 8px; margin-top: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Banka Kartı Ayarları</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Günlük Çekim Limiti</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${dailyLimit}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">ATM Kullanımı</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${atmEnabled}</div>
                        </div>
                    </div>
                </div>
            `;
        }
        
        // Virtual Card özel alanlar
        let virtualSpecificHTML = '';
        if (cardType === 'VIRTUAL' || cardType === 'SANAL') {
            const onlineOnly = detail.onlineOnly !== undefined && detail.onlineOnly !== null 
                ? (detail.onlineOnly ? 'Evet' : 'Hayır')
                : 'Belirtilmemiş';
            const singleUseExpiresAt = detail.singleUseExpiresAt 
                ? new Date(detail.singleUseExpiresAt).toLocaleDateString('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' })
                : null;
            
            // JSON'dan limit bilgilerini parse et
            let dailyLimit = 'Belirtilmemiş';
            let totalLimit = 'Belirtilmemiş';
            
            if (detail.usageRestriction) {
                try {
                    const usageRestriction = typeof detail.usageRestriction === 'string' 
                        ? JSON.parse(detail.usageRestriction) 
                        : detail.usageRestriction;
                    
                    if (usageRestriction && typeof usageRestriction === 'object') {
                        if (usageRestriction.dailyLimit !== undefined && usageRestriction.dailyLimit !== null) {
                            dailyLimit = parseFloat(usageRestriction.dailyLimit).toLocaleString('tr-TR', { 
                                minimumFractionDigits: 2, 
                                maximumFractionDigits: 2 
                            }) + ' ₺';
                        }
                        if (usageRestriction.totalLimit !== undefined && usageRestriction.totalLimit !== null) {
                            totalLimit = parseFloat(usageRestriction.totalLimit).toLocaleString('tr-TR', { 
                                minimumFractionDigits: 2, 
                                maximumFractionDigits: 2 
                            }) + ' ₺';
                        }
                    }
                } catch (e) {
                    console.error('Error parsing usageRestriction:', e);
                }
            }
            
            virtualSpecificHTML = `
                <div style="background: #f5f5f5; padding: 16px; border-radius: 8px; margin-top: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Sanal Kart Ayarları</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;">
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Sadece Online</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${onlineOnly}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Günlük Limit</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${dailyLimit}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Toplam Limit</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${totalLimit}</div>
                        </div>
                    </div>
                    ${singleUseExpiresAt ? `
                        <div style="margin-top: 12px;">
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Tek Kullanımlık Bitiş Tarihi</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${singleUseExpiresAt}</div>
                        </div>
                    ` : ''}
                </div>
            `;
        }
        
        // Credit Card özel alanlar
        let creditSpecificHTML = '';
        if (cardType === 'CREDIT' || cardType === 'KREDI') {
            const creditLimit = detail.creditLimit !== undefined && detail.creditLimit !== null 
                ? parseFloat(detail.creditLimit).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ₺'
                : 'Belirtilmemiş';
            const availableCredit = detail.availableCredit !== undefined && detail.availableCredit !== null 
                ? parseFloat(detail.availableCredit).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ₺'
                : 'Belirtilmemiş';
            const interestRate = detail.interestRate !== undefined && detail.interestRate !== null 
                ? parseFloat(detail.interestRate).toFixed(2) + '%'
                : 'Belirtilmemiş';
            
            creditSpecificHTML = `
                <div style="background: #f5f5f5; padding: 16px; border-radius: 8px; margin-top: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Kredi Kartı Bilgileri</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Kredi Limiti</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${creditLimit}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Kullanılabilir Limit</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${availableCredit}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Faiz Oranı</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #222;">${interestRate}</div>
                        </div>
                    </div>
                </div>
            `;
        }
        
        // Action buttons
        const isVirtual = (cardType === 'VIRTUAL' || cardType === 'SANAL');
        const actionButtonHTML = status === 'ACTIVE' 
            ? (isVirtual 
                ? `<button class="btn" id="card-delete" style="background: #f44336; color: white; flex: 1;">Sil</button>`
                : `<button class="btn" id="card-block" style="background: #f44336; color: white; flex: 1;">Bloke Et</button>`
            )
            : `<button class="btn" id="card-activate" style="background: #4caf50; color: white; flex: 1;">Aktifleştir</button>`;
        
        const modal = document.createElement('div');
        modal.style.position = 'fixed';
        modal.style.left = '0';
        modal.style.top = '0';
        modal.style.width = '100%';
        modal.style.height = '100%';
        modal.style.background = 'rgba(0,0,0,0.6)';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.zIndex = '9999';
        modal.style.padding = '20px';
        modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

        const box = document.createElement('div');
        box.style.background = 'white';
        box.style.color = '#222';
        box.style.borderRadius = '16px';
        box.style.width = '100%';
        box.style.maxWidth = '700px';
        box.style.maxHeight = '90vh';
        box.style.overflow = 'auto';
        box.style.boxShadow = '0 20px 60px rgba(0,0,0,0.3)';
        
        box.innerHTML = `
            <!-- Kart Görseli -->
            <div style="background: ${cardBgColor}; color: ${cardTextColor}; padding: 32px; border-radius: 16px 16px 0 0; position: relative; min-height: 220px;">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div style="width: 10px; height: 10px; background: ${cardAccentColor}; border-radius: 50%;"></div>
                        <div style="font-size: 0.8rem; opacity: 0.9;">${cardBrand}</div>
                    </div>
                    <div style="font-weight: 700; font-size: 1.3rem; letter-spacing: 2px;">TRIO</div>
                </div>
                
                <div style="font-family: 'Courier New', monospace; font-size: 1.6rem; font-weight: 600; letter-spacing: 3px; margin-bottom: 32px; word-spacing: 10px;">
                    ${number}
                </div>
                
                <div style="display: flex; justify-content: space-between; align-items: flex-end;">
                    <div style="flex: 1;">
                        <div style="font-size: 0.7rem; opacity: 0.8; margin-bottom: 4px;">KART SAHİBİ</div>
                        <div style="font-size: 1rem; font-weight: 600; text-transform: uppercase;">${cardholderName}</div>
                    </div>
                    <div style="display: flex; gap: 20px; align-items: flex-end;">
                        ${(cardType !== 'DEBIT' && cardType !== 'BANKA') ? `
                        <div>
                            <div style="font-size: 0.7rem; opacity: 0.8; margin-bottom: 4px;">SKT</div>
                            <div style="font-size: 1rem; font-weight: 600;">${String(expiryMonth).padStart(2, '0')}/${String(expiryYear).slice(-2)}</div>
                        </div>
                        ` : ''}
                    </div>
                </div>
            </div>
            
            <!-- Detay Bilgileri -->
            <div style="padding: 24px;">
                <!-- Header -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                    <h2 style="margin: 0; font-size: 1.5rem; color: #222;">Kart Detayları</h2>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="background: ${statusColor}; color: white; padding: 6px 12px; border-radius: 20px; font-size: 0.85rem; font-weight: 600;">${statusBadge}</span>
                        <button class="btn secondary" id="card-close" style="padding: 8px 16px;">✕</button>
                    </div>
                </div>
                
                <!-- Genel Bilgiler -->
                <div style="background: #f9f9f9; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Genel Bilgiler</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Kart Tipi</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">
                                ${cardType === 'DEBIT' ? 'Banka Kartı' : cardType === 'VIRTUAL' ? 'Sanal Kart' : cardType === 'CREDIT' ? 'Kredi Kartı' : cardType}
                            </div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Kart Markası</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${cardBrand}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Ana Hesap</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222; font-family: 'Courier New', monospace;">${accountNumber}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Oluşturulma Tarihi</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${createdAt}</div>
                        </div>
                        ${(blockedAt && status !== 'ACTIVE') ? `
                            <div>
                                <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Bloke Tarihi</div>
                                <div style="font-size: 1rem; font-weight: 600; color: #222;">${blockedAt}</div>
                            </div>
                            ${blockReason ? `
                                <div>
                                    <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Bloke Nedeni</div>
                                    <div style="font-size: 1rem; font-weight: 600; color: #222;">${blockReason}</div>
                                </div>
                            ` : ''}
                        ` : ''}
                    </div>
                </div>
                
                ${debitSpecificHTML}
                ${virtualSpecificHTML}
                ${creditSpecificHTML}
                
                <!-- Action Buttons -->
                <div style="display: flex; gap: 12px; margin-top: 24px; padding-top: 24px; border-top: 1px solid #e0e0e0;">
                    ${actionButtonHTML}
                </div>
            </div>
        `;
        
        modal.appendChild(box);
        document.body.appendChild(modal);
        
        // Event listeners
        document.getElementById('card-close').addEventListener('click', () => modal.remove());
        
        if (status === 'ACTIVE') {
            if (isVirtual) {
                document.getElementById('card-delete').addEventListener('click', () => {
                    modal.remove();
                    showConfirmModal(
                        currentLang === 'tr' ? 'Sanal Kartı Sil' : 'Delete Virtual Card',
                        currentLang === 'tr' ? 'Bu sanal kartı silmek istediğinize emin misiniz?' : 'Are you sure you want to delete this virtual card?',
                        () => {
                            blockCard(cardId, 'user_deleted')
                                .then(() => {
                                    loadCards({ customerId: getCustomerId() }).then(renderCardsList);
                                    showToast(currentLang === 'tr' ? 'Sanal kart silindi.' : 'Virtual card deleted.', 'success');
                                })
                                .catch(err => {
                                    console.error('Delete card error:', err);
                                    showToast(currentLang === 'tr' ? 'Kart silinirken hata oluştu.' : 'Error deleting card.', 'error');
                                });
                        }
                    );
                });
            } else {
                document.getElementById('card-block').addEventListener('click', () => {
                    modal.remove();
                    showConfirmModal(
                        currentLang === 'tr' ? 'Kartı Bloke Et' : 'Block Card',
                        currentLang === 'tr' ? 'Bu kartı bloke etmek istediğinize emin misiniz?' : 'Are you sure you want to block this card?',
                        () => {
                            blockCard(cardId, 'user_request')
                                .then(() => {
                                    loadCards({ customerId: getCustomerId() }).then(renderCardsList);
                                    showToast(currentLang === 'tr' ? 'Kart bloke edildi.' : 'Card blocked.', 'success');
                                })
                                .catch(err => {
                                    console.error('Block card error:', err);
                                    showToast(currentLang === 'tr' ? 'Kart bloke edilirken hata oluştu.' : 'Error blocking card.', 'error');
                                });
                        }
                    );
                });
            }
        } else {
            document.getElementById('card-activate').addEventListener('click', () => {
                modal.remove();
                activateCard(cardId)
                    .then(() => {
                        loadCards({ customerId: getCustomerId() }).then(renderCardsList);
                        showToast(currentLang === 'tr' ? 'Kart aktifleştirildi.' : 'Card activated.', 'success');
                    })
                    .catch(err => {
                        console.error('Activate card error:', err);
                        showToast(currentLang === 'tr' ? 'Kart aktifleştirilirken hata oluştu.' : 'Error activating card.', 'error');
                    });
            });
        }
        
    } catch (e) {
        console.error('showCardDetails error', e);
        showToast(currentLang === 'tr' ? 'Kart detayları yüklenirken hata oluştu.' : 'Error loading card details.', 'error');
    }
}

// ==========================================
// CARD TYPE DROPDOWN FUNCTIONS
// ==========================================

function toggleCardTypeDropdown() {
    const dropdown = document.getElementById('cardTypeDropdown');
    const icon = document.getElementById('cardTypeDropdownIcon');
    
    if (dropdown.style.display === 'none' || !dropdown.style.display) {
        dropdown.style.display = 'block';
        icon.style.transform = 'rotate(180deg)';
    } else {
        dropdown.style.display = 'none';
        icon.style.transform = 'rotate(0deg)';
    }
}

function selectCardType(type) {
    currentCardTypeFilter = type;
    updateCardTypeDropdownText();
    toggleCardTypeDropdown();
    
    // Reload cards with filter
    (async () => {
        try {
            if (!currentClient || (!currentClient.id && !currentClient.ID)) {
                await loadClientInfo();
            }
            const list = await loadCards({ customerId: getCustomerId() });
            renderCardsList(list);
        } catch (e) {
            console.error('Failed to load filtered cards:', e);
            showToast(currentLang === 'tr' ? 'Kartlar yüklenirken hata oluştu.' : 'Error loading cards.', 'error');
        }
    })();
}

function updateCardTypeDropdownText() {
    const textEl = document.getElementById('cardTypeDropdownText');
    if (!textEl) return;
    
    switch (currentCardTypeFilter) {
        case 'DEBIT':
            textEl.textContent = 'Banka Kartlarım';
            break;
        case 'VIRTUAL':
            textEl.textContent = 'Sanal Kartlarım';
            break;
        default:
            textEl.textContent = 'Tüm Kartlarım';
    }
}

async function refreshCardsList() {
    try {
        if (!currentClient || (!currentClient.id && !currentClient.ID)) {
            await loadClientInfo();
        }
        const list = await loadCards({ customerId: getCustomerId() });
        renderCardsList(list);
        showToast(currentLang === 'tr' ? 'Kartlar yenilendi.' : 'Cards refreshed.', 'success');
    } catch (e) {
        console.error('Failed to refresh cards:', e);
        showToast(currentLang === 'tr' ? 'Kartlar yenilenirken hata oluştu.' : 'Error refreshing cards.', 'error');
    }
}

// Close dropdown when clicking outside
document.addEventListener('click', (e) => {
    const dropdown = document.getElementById('cardTypeDropdown');
    const btn = document.getElementById('cardTypeDropdownBtn');
    
    if (dropdown && btn && !dropdown.contains(e.target) && !btn.contains(e.target)) {
        dropdown.style.display = 'none';
        const icon = document.getElementById('cardTypeDropdownIcon');
        if (icon) icon.style.transform = 'rotate(0deg)';
    }
});

// Modal to issue virtual card
async function showIssueVirtualModal() {
    // Önce client bilgisini yükle (mutlaka yükle)
    if (!currentClient || (!currentClient.id && !currentClient.ID)) {
        const loaded = await loadClientInfo();
        if (!loaded) {
            // loadClientInfo başarısız oldu, tekrar dene
            await loadClientInfo();
        }
    }
    
    // Önce hesapları yükle
    if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
        await loadAccounts();
    }
    
    if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
        showToast(currentLang === 'tr' ? 'Sanal kart oluşturmak için en az bir hesabınız olmalıdır.' : 'You need at least one account to create a virtual card.', 'error');
        return;
    }

    // Kart sahibi adını otomatik al (currentClient'tan) - updateClientUI ile aynı mantık
    let cardholderName = '';
    if (currentClient) {
        // updateClientUI'deki mantıkla aynı şekilde al
        const first = currentClient.first_name || currentClient.firstName || currentClient.name || '';
        const last = currentClient.last_name || currentClient.lastName || currentClient.surname || '';
        if (first || last) {
            cardholderName = (first + (last ? ' ' + last : '')).trim().toUpperCase();
        }
    }
    
    // Eğer currentClient'tan alınamadıysa, currentUser'dan dene
    if (!cardholderName && currentUser) {
        if (currentUser.name) {
            cardholderName = currentUser.name.toUpperCase();
        } else if (currentUser.firstName || currentUser.lastName || currentUser.first_name || currentUser.last_name) {
            const firstName = currentUser.firstName || currentUser.first_name || '';
            const lastName = currentUser.lastName || currentUser.last_name || currentUser.surname || '';
            cardholderName = (firstName + ' ' + lastName).trim().toUpperCase();
        } else if (currentUser.fullname) {
            cardholderName = currentUser.fullname.toUpperCase();
        }
    }
    
    // Hala bulunamadıysa hata göster
    if (!cardholderName) {
        console.error('Cardholder name not found. currentClient:', currentClient, 'currentUser:', currentUser);
        showToast(currentLang === 'tr' ? 'Kullanıcı bilgisi bulunamadı. Lütfen profil bilgilerinizi güncelleyin.' : 'User information not found. Please update your profile.', 'error');
        return;
    }

    const modal = document.createElement('div');
    modal.id = 'virtual-card-modal';
    modal.style.position='fixed'; modal.style.left='0'; modal.style.top='0'; modal.style.width='100%'; modal.style.height='100%'; modal.style.background='rgba(0,0,0,0.5)'; modal.style.display='flex'; modal.style.alignItems='center'; modal.style.justifyContent='center'; modal.style.zIndex='9999';
    modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

    const box = document.createElement('div');
    box.style.background='white'; box.style.color='#222'; box.style.padding='24px'; box.style.borderRadius='12px'; box.style.width='95%'; box.style.maxWidth='520px'; box.style.maxHeight='90vh'; box.style.overflowY='auto';
    
    // Hesap seçenekleri
    const formatBalance = (bal) => {
        if (!bal && bal !== 0) return '0,00';
        return parseFloat(bal).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };
    const accountOptions = window.accountsState.accounts.map(acc => {
        const accId = acc.id || acc.accountId || acc.account_id || '';
        const accNum = acc.accountNumber || accId;
        const balance = (acc.availableBalance !== undefined && acc.availableBalance !== null) 
            ? Number(acc.availableBalance) 
            : ((acc.balance !== undefined && acc.balance !== null) ? Number(acc.balance) : 0);
        return `<option value="${accId}">${accNum} - ${formatBalance(balance)} TL</option>`;
    }).join('');

    box.innerHTML = `
        <div style="text-align: center; margin-bottom: 24px;">
            <div style="width: 60px; height: 60px; background: linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px;">
                <i class="fas fa-credit-card" style="color: white; font-size: 24px;"></i>
            </div>
            <h3 style="margin: 0 0 8px 0; color: #1a1a1a; font-size: 1.5rem; font-weight: 700;">Sanal Kart Oluştur</h3>
            <p style="margin: 0; color: #666; font-size: 0.9rem;">Online işlemler için sanal kart oluşturun</p>
        </div>
        
        <div style="background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%); padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 1px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-wallet" style="color: #9b59b6; margin-right: 6px;"></i>Hesap Seçin
            </label>
            <select id="virt-acc" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; background: white; cursor: pointer; transition: all 0.3s; font-weight: 500;" onfocus="this.style.borderColor='#9b59b6'; this.style.boxShadow='0 0 0 3px rgba(155, 89, 182, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                <option value="">Hesap seçin...</option>
                ${accountOptions}
            </select>
        </div>
        
        <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-user" style="color: #9b59b6; margin-right: 6px;"></i>Kart Sahibi
            </label>
            <div style="padding: 14px 16px; background: #f5f5f5; border-radius: 10px; color: #666; font-weight: 600; text-transform: uppercase;">
                ${cardholderName}
            </div>
            <small style="color: #999; font-size: 0.85rem; margin-top: 4px; display: block;">Kart sahibi bilgisi otomatik olarak profilinizden alınmıştır.</small>
        </div>
        
        <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-calendar-day" style="color: #9b59b6; margin-right: 6px;"></i>Günlük Harcama Limiti (TL)
            </label>
            <input type="number" id="virt-daily-limit" min="0" step="0.01" placeholder="0.00" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; font-weight: 500; margin-bottom: 16px;" onfocus="this.style.borderColor='#9b59b6'; this.style.boxShadow='0 0 0 3px rgba(155, 89, 182, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
            <small style="color: #666; font-size: 0.85rem; margin-bottom: 16px; display: block;">
                <i class="fas fa-info-circle" style="margin-right: 4px;"></i>Kart için günlük maksimum harcama limiti (TL cinsinden)
            </small>
            
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-money-bill-wave" style="color: #9b59b6; margin-right: 6px;"></i>Toplam Limit (TL)
            </label>
            <input type="number" id="virt-total-limit" min="0" step="0.01" placeholder="0.00" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; font-weight: 500;" onfocus="this.style.borderColor='#9b59b6'; this.style.boxShadow='0 0 0 3px rgba(155, 89, 182, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
            <small style="color: #666; font-size: 0.85rem; margin-top: 8px; display: block;">
                <i class="fas fa-info-circle" style="margin-right: 4px;"></i>Kart için toplam maksimum harcama limiti (TL cinsinden)
            </small>
        </div>
        
        <div style="background: #e3f2fd; border-left: 4px solid #9b59b6; border-radius: 8px; padding: 16px; margin-bottom: 24px;">
            <div style="display: flex; align-items: start; gap: 12px;">
                <i class="fas fa-info-circle" style="color: #9b59b6; font-size: 20px; margin-top: 2px;"></i>
                <div style="flex: 1;">
                    <div style="font-weight: 600; color: #9b59b6; margin-bottom: 4px; font-size: 0.9rem;">Bilgi</div>
                    <div style="font-size: 0.85rem; color: #9b59b6; line-height: 1.5;">Sanal kartlar sadece online işlemler için kullanılabilir. Günlük limit ve toplam limit belirleyerek harcamalarınızı kontrol edebilirsiniz.</div>
                </div>
            </div>
        </div>
        
        <div style="display:flex; gap:12px; justify-content:flex-end; padding-top: 20px; border-top: 1px solid #e0e4e8;">
            <button class="btn" id="virt-cancel" style="padding: 14px 28px; font-weight: 600; border: 2px solid #ddd; background: white; color: #666;">İptal</button>
            <button class="btn" id="virt-submit" style="padding: 14px 28px; font-weight: 600; background: linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%); color: white; border: none; box-shadow: 0 4px 12px rgba(155, 89, 182, 0.2);">
                <i class="fas fa-check" style="margin-right: 8px;"></i>Oluştur
            </button>
        </div>
    `;
    modal.appendChild(box);
    document.body.appendChild(modal);

    document.getElementById('virt-cancel').addEventListener('click', () => modal.remove());
    document.getElementById('virt-submit').addEventListener('click', async () => {
        const accId = document.getElementById('virt-acc').value.trim();
        const dailyLimitValue = document.getElementById('virt-daily-limit').value.trim();
        const totalLimitValue = document.getElementById('virt-total-limit').value.trim();
        
        const dailyLimit = dailyLimitValue ? parseFloat(dailyLimitValue.replace(',', '.')) : null;
        const totalLimit = totalLimitValue ? parseFloat(totalLimitValue.replace(',', '.')) : null;
        
        if (!accId) { 
            showToast(currentLang === 'tr' ? 'Lütfen bir hesap seçin.' : 'Please select an account.', 'error'); 
            return; 
        }
        if (!dailyLimitValue || dailyLimit === null || isNaN(dailyLimit) || dailyLimit <= 0) {
            showToast(currentLang === 'tr' ? 'Lütfen geçerli bir günlük limit girin (0\'dan büyük olmalıdır).' : 'Please enter a valid daily limit (must be greater than 0).', 'error');
            return;
        }
        if (!totalLimitValue || totalLimit === null || isNaN(totalLimit) || totalLimit <= 0) {
            showToast(currentLang === 'tr' ? 'Lütfen geçerli bir toplam limit girin (0\'dan büyük olmalıdır).' : 'Please enter a valid total limit (must be greater than 0).', 'error');
            return;
        }
        
        try {
            // JSON formatında usageRestriction oluştur
            const usageRestrictionJson = JSON.stringify({
                dailyLimit: dailyLimit,
                totalLimit: totalLimit
            });
            
            const payload = { 
                accountId: accId, 
                cardholderName: cardholderName, 
                onlineOnly: true,
                singleUse: true,
                usageRestriction: usageRestrictionJson
            };
            
            await issueVirtualCard(payload);
            modal.remove();
            // Kart listesini yenile
            const list = await loadCards({ customerId: getCustomerId() });
            renderCardsList(list);
        } catch (e) { 
            console.error('issue virtual failed', e);
            // Hata mesajı apiCall tarafından gösterilecek
        }
    });
}

// Modal to request debit card
function showIssueDebitModal() {
    const modal = document.createElement('div');
    modal.style.position='fixed'; modal.style.left='0'; modal.style.top='0'; modal.style.width='100%'; modal.style.height='100%'; modal.style.background='rgba(0,0,0,0.5)'; modal.style.display='flex'; modal.style.alignItems='center'; modal.style.justifyContent='center'; modal.style.zIndex='9999';
    modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

    const box = document.createElement('div');
    box.style.background='white'; box.style.color='#222'; box.style.padding='16px'; box.style.borderRadius='8px'; box.style.width='90%'; box.style.maxWidth='520px';
    box.innerHTML = `
        <h3>Banka Kartı Talebi</h3>
        <label>Hesap ID</label>
        <input id="debit-acc" placeholder="accountId">
        <label>Kart Sahibinin Adı</label>
        <input id="debit-name" placeholder="Kart sahibinin adı">
        <label>PIN (4 rakam)</label>
        <input id="debit-pin" placeholder="1234" maxlength="4">
        <div style="display:flex; gap:8px; margin-top:10px; justify-content:flex-end;">
            <button class="btn" id="debit-cancel">İptal</button>
            <button class="btn primary" id="debit-submit">Talep Et</button>
        </div>
    `;
    modal.appendChild(box);
    document.body.appendChild(modal);

    document.getElementById('debit-cancel').addEventListener('click', () => modal.remove());
    document.getElementById('debit-submit').addEventListener('click', async () => {
        const acc = document.getElementById('debit-acc').value.trim();
        const name = document.getElementById('debit-name').value.trim() || (currentUser && currentUser.name) || '';
        const pin = document.getElementById('debit-pin').value.trim();
        if (!acc) { showToast('Lütfen hesap ID girin.', 'error'); return; }
        if (!pin || pin.length !== 4) { showToast('Lütfen 4 haneli PIN girin.', 'error'); return; }
        try {
            const payload = { accountId: acc, cardholderName: name, pin };
            await issueDebitCard(payload);
            modal.remove();
            const list = await loadCards({ customerId: getCustomerId() });
            renderCardsList(list);
        } catch (e) { console.error('issue debit failed', e); }
    });
}

// Render statement in a modal with basic pagination and filters
async function showStatementModal(accountId) {
    // create modal skeleton
    const modal = document.createElement('div');
    modal.style.position = 'fixed'; modal.style.left = '0'; modal.style.top = '0'; modal.style.width = '100%'; modal.style.height = '100%';
    modal.style.background = 'rgba(0,0,0,0.5)'; modal.style.display = 'flex'; modal.style.alignItems = 'center'; modal.style.justifyContent = 'center'; modal.style.zIndex = '9999';
    modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

    const box = document.createElement('div');
    box.style.background = 'white'; box.style.color = '#222'; box.style.padding = '16px'; box.style.borderRadius = '8px'; box.style.maxWidth = '920px'; box.style.width = '95%'; box.style.maxHeight = '85vh'; box.style.overflow = 'auto';

    box.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <h3>Hesap Ekstresi</h3>
            <div><button class="btn secondary" id="stmt-close">Kapat</button></div>
        </div>
        <div id="stmt-header" style="margin-top:8px; margin-bottom:12px; display:flex; gap:12px; align-items:center;"></div>
        <div style="margin-bottom:8px; display:flex; gap:8px; align-items:center;">
            <label>Başlangıç:</label><input type="date" id="stmt-start">
            <label>Bitiş:</label><input type="date" id="stmt-end">
            <label>Tür:</label>
            <select id="stmt-type"><option value="">Hepsi</option><option value="DEBIT">DEBIT</option><option value="CREDIT">CREDIT</option></select>
            <input id="stmt-keyword" placeholder="Ara..." style="flex:1;">
            <button class="btn" id="stmt-filter">Filtrele</button>
        </div>
        <div id="stmt-entries" style="max-height:52vh; overflow:auto; border-top:1px solid rgba(0,0,0,0.06); padding-top:8px;"></div>
        <div style="display:flex; justify-content:space-between; align-items:center; margin-top:8px;">
            <div id="stmt-pagination"></div>
            <div><button class="btn" id="stmt-prev">Önceki</button><button class="btn" id="stmt-next">Sonraki</button></div>
        </div>
    `;

    modal.appendChild(box);
    document.body.appendChild(modal);

    document.getElementById('stmt-close').addEventListener('click', () => modal.remove());

    // state
    let page = 0; const size = 20;

    async function loadPage() {
        try {
            const start = document.getElementById('stmt-start').value || undefined;
            const end = document.getElementById('stmt-end').value || undefined;
            const type = document.getElementById('stmt-type').value || undefined;
            const keyword = document.getElementById('stmt-keyword').value || undefined;

            const stmt = await fetchStatement(accountId, { page, size, startDate: start, endDate: end, type, keyword, includeRunningBalance: true });

            // Header: balance and summary if available
            const header = document.getElementById('stmt-header'); header.innerHTML = '';
            if (stmt) {
                const h = document.createElement('div');
                h.innerHTML = `<strong>Hesap:</strong> ${stmt.accountId || accountId} &nbsp; <strong>Döviz:</strong> ${stmt.currency || ''} &nbsp; <strong>Açılış:</strong> ${stmt.openingBalance != null ? stmt.openingBalance : ''} &nbsp; <strong>Kapanış:</strong> ${stmt.closingBalance != null ? stmt.closingBalance : ''}`;
                header.appendChild(h);
            }

            // Entries
            const entriesEl = document.getElementById('stmt-entries'); entriesEl.innerHTML = '';
            if (!stmt || !stmt.entries || stmt.entries.length === 0) {
                entriesEl.innerHTML = '<div style="padding:10px;">İşlem bulunamadı.</div>';
            } else {
                const table = document.createElement('table');
                table.style.width = '100%'; table.style.borderCollapse = 'collapse';
                const headerRow = document.createElement('tr');
                ['Tarih','Tip','Açıklama','Tutar','Bakiye','Referans'].forEach(t => { const th = document.createElement('th'); th.style.textAlign='left'; th.style.padding='6px'; th.innerText = t; headerRow.appendChild(th); });
                table.appendChild(headerRow);
                stmt.entries.forEach(e => {
                    const r = document.createElement('tr');
                    r.innerHTML = `<td style="padding:6px">${e.date || ''}</td><td style="padding:6px">${e.entryType || ''}</td><td style="padding:6px">${e.description || ''}</td><td style="padding:6px">${e.amount != null ? e.amount.toLocaleString('tr-TR') : ''}</td><td style="padding:6px">${e.runningBalance != null ? e.runningBalance.toLocaleString('tr-TR') : ''}</td><td style="padding:6px">${e.referenceNumber || ''}</td>`;
                    table.appendChild(r);
                });
                entriesEl.appendChild(table);
            }

            // Pagination metadata
            const pg = document.getElementById('stmt-pagination'); pg.innerText = stmt && stmt.pagination ? `Sayfa ${stmt.pagination.page+1} / ${stmt.pagination.totalPages}` : `Sayfa ${page+1}`;
        } catch (err) {
            console.error('loadPage statement error', err);
            showToast('Ekstre yüklenemedi. Konsolu kontrol edin.', 'error');
        }
    }

    document.getElementById('stmt-filter').addEventListener('click', () => { page = 0; loadPage(); });
    document.getElementById('stmt-prev').addEventListener('click', () => { if (page > 0) { page -= 1; loadPage(); } });
    document.getElementById('stmt-next').addEventListener('click', () => { page += 1; loadPage(); });

    // initial load
    loadPage();
}

// Hesap Hareketleri Modal - Modern tasarım, pagination ve filtreleme ile
async function showAccountStatementModal(accountId) {
    if (!accountId) {
        showToast('Hesap ID bulunamadı.', 'error');
        return;
    }
    
    // Hesap bilgisini bul
    const account = window.accountsState?.accounts?.find(acc => 
        (acc.id || acc.accountId || acc.account_id) === accountId
    );
    const accountNumber = account ? (account.accountNumber || account.id || accountId) : accountId;
    const accountType = account ? (account.type || account.product || 'Vadesiz TL') : 'Hesap';
    
    // Modal oluştur
    const modal = document.createElement('div');
    modal.id = 'account-statement-modal';
    modal.style.cssText = 'position: fixed; left: 0; top: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 9999;';
    modal.onclick = (e) => { if (e.target === modal) modal.remove(); };
    
    const box = document.createElement('div');
    box.style.cssText = 'background: white; border-radius: 16px; width: 95%; max-width: 900px; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.3);';
    
    // Header
    box.innerHTML = `
        <div style="padding: 24px; border-bottom: 2px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center;">
            <div>
                <h2 style="margin: 0 0 4px 0; color: #1a1a1a; font-size: 1.5rem; font-weight: 700;">Hesap Hareketleri</h2>
                <div style="font-size: 0.9rem; color: #666;">
                    <span style="font-weight: 600;">${escapeHtml(accountType)}</span> - 
                    <span style="font-family: monospace;">${escapeHtml(accountNumber)}</span>
                </div>
            </div>
            <button id="stmt-modal-close" style="width: 40px; height: 40px; background: #f5f5f5; border: none; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.3s;" onmouseover="this.style.background='#e0e0e0'" onmouseout="this.style.background='#f5f5f5'">
                <i class="fas fa-times" style="color: #666; font-size: 18px;"></i>
            </button>
        </div>
        
        <!-- Filtrele Butonu -->
        <div style="padding: 16px 24px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center;">
            <button id="stmt-filter-btn" style="display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; font-weight: 600; color: #333; transition: all 0.3s;" onmouseover="this.style.background='#e8e8e8'" onmouseout="this.style.background='#f5f5f5'">
                <i class="fas fa-filter" style="color: #d32f2f;"></i>
                <span>Filtrele</span>
            </button>
            <div id="stmt-summary" style="font-size: 0.85rem; color: #666;"></div>
        </div>
        
        <!-- İşlemler Listesi -->
        <div id="stmt-entries-container" style="flex: 1; overflow-y: auto; padding: 0;">
            <div style="text-align: center; padding: 40px; color: #999;">
                <i class="fas fa-spinner fa-spin" style="font-size: 2rem; margin-bottom: 12px;"></i>
                <div>Hareketler yükleniyor...</div>
            </div>
        </div>
        
        <!-- Pagination -->
        <div id="stmt-pagination-container" style="padding: 16px 24px; border-top: 1px solid #f0f0f0; display: none; justify-content: space-between; align-items: center; background: #fafafa;">
            <div id="stmt-page-info" style="font-size: 0.9rem; color: #666; font-weight: 600;"></div>
            <div style="display: flex; gap: 8px;">
                <button id="stmt-prev-btn" style="padding: 8px 16px; background: white; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; font-weight: 600; color: #333; transition: all 0.3s;" onmouseover="this.style.borderColor='#d32f2f'; this.style.color='#d32f2f'" onmouseout="this.style.borderColor='#ddd'; this.style.color='#333'">
                    <i class="fas fa-chevron-left" style="margin-right: 4px;"></i>Önceki
                </button>
                <button id="stmt-next-btn" style="padding: 8px 16px; background: white; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; font-weight: 600; color: #333; transition: all 0.3s;" onmouseover="this.style.borderColor='#d32f2f'; this.style.color='#d32f2f'" onmouseout="this.style.borderColor='#ddd'; this.style.color='#333'">
                    Sonraki<i class="fas fa-chevron-right" style="margin-left: 4px;"></i>
                </button>
            </div>
        </div>
    `;
    
    modal.appendChild(box);
    document.body.appendChild(modal);
    
    // State
    let currentPage = 0;
    const pageSize = 10;
    let filters = {
        startDate: null,
        endDate: null,
        type: null,
        keyword: null
    };
    
    // Filtrele modal'ı
    function showFilterModal() {
        const filterModal = document.createElement('div');
        filterModal.style.cssText = 'position: fixed; left: 0; top: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 10000;';
        filterModal.onclick = (e) => { if (e.target === filterModal) filterModal.remove(); };
        
        const filterBox = document.createElement('div');
        filterBox.style.cssText = 'background: white; border-radius: 16px; padding: 24px; width: 90%; max-width: 500px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);';
        
        filterBox.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h3 style="margin: 0; color: #1a1a1a; font-size: 1.3rem; font-weight: 700;">Filtrele</h3>
                <button id="filter-close" style="width: 32px; height: 32px; background: #f5f5f5; border: none; border-radius: 50%; cursor: pointer;">
                    <i class="fas fa-times" style="color: #666;"></i>
                </button>
            </div>
            
            <div style="display: flex; flex-direction: column; gap: 16px;">
                <div>
                    <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #333; font-size: 0.9rem;">Başlangıç Tarihi</label>
                    <input type="date" id="filter-start-date" value="${filters.startDate || ''}" style="width: 100%; padding: 12px; border: 2px solid #ddd; border-radius: 8px; font-size: 1rem;">
                </div>
                
                <div>
                    <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #333; font-size: 0.9rem;">Bitiş Tarihi</label>
                    <input type="date" id="filter-end-date" value="${filters.endDate || ''}" style="width: 100%; padding: 12px; border: 2px solid #ddd; border-radius: 8px; font-size: 1rem;">
                </div>
                
                <div>
                    <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #333; font-size: 0.9rem;">İşlem Tipi</label>
                    <select id="filter-type" style="width: 100%; padding: 12px; border: 2px solid #ddd; border-radius: 8px; font-size: 1rem; background: white;">
                        <option value="">Tümü</option>
                        <option value="DEBIT" ${filters.type === 'DEBIT' ? 'selected' : ''}>Borç (Çıkış)</option>
                        <option value="CREDIT" ${filters.type === 'CREDIT' ? 'selected' : ''}>Alacak (Giriş)</option>
                    </select>
                </div>
                
                <div>
                    <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #333; font-size: 0.9rem;">Anahtar Kelime</label>
                    <input type="text" id="filter-keyword" value="${filters.keyword || ''}" placeholder="Açıklama veya referans numarası..." style="width: 100%; padding: 12px; border: 2px solid #ddd; border-radius: 8px; font-size: 1rem;">
                </div>
            </div>
            
            <div style="display: flex; gap: 12px; margin-top: 24px; padding-top: 20px; border-top: 1px solid #f0f0f0;">
                <button id="filter-reset" style="flex: 1; padding: 14px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 8px; font-weight: 600; color: #666; cursor: pointer; transition: all 0.3s;" onmouseover="this.style.background='#e8e8e8'" onmouseout="this.style.background='#f5f5f5'">
                    Temizle
                </button>
                <button id="filter-apply" style="flex: 1; padding: 14px; background: linear-gradient(135deg, #d32f2f 0%, #f44336 100%); border: none; border-radius: 8px; font-weight: 600; color: white; cursor: pointer; box-shadow: 0 4px 12px rgba(211, 47, 47, 0.3);">
                    Tamam
                </button>
            </div>
        `;
        
        filterModal.appendChild(filterBox);
        document.body.appendChild(filterModal);
        
        document.getElementById('filter-close').addEventListener('click', () => filterModal.remove());
        document.getElementById('filter-reset').addEventListener('click', () => {
            filters = { startDate: null, endDate: null, type: null, keyword: null };
            document.getElementById('filter-start-date').value = '';
            document.getElementById('filter-end-date').value = '';
            document.getElementById('filter-type').value = '';
            document.getElementById('filter-keyword').value = '';
        });
        document.getElementById('filter-apply').addEventListener('click', () => {
            filters.startDate = document.getElementById('filter-start-date').value || null;
            filters.endDate = document.getElementById('filter-end-date').value || null;
            filters.type = document.getElementById('filter-type').value || null;
            filters.keyword = document.getElementById('filter-keyword').value.trim() || null;
            filterModal.remove();
            currentPage = 0;
            loadStatement();
        });
    }
    
    // İşlemleri yükle
    async function loadStatement() {
        const entriesContainer = document.getElementById('stmt-entries-container');
        const paginationContainer = document.getElementById('stmt-pagination-container');
        const pageInfo = document.getElementById('stmt-page-info');
        const summary = document.getElementById('stmt-summary');
        
        entriesContainer.innerHTML = `
            <div style="text-align: center; padding: 40px; color: #999;">
                <i class="fas fa-spinner fa-spin" style="font-size: 2rem; margin-bottom: 12px;"></i>
                <div>Hareketler yükleniyor...</div>
            </div>
        `;
        
        try {
            const stmt = await fetchStatement(accountId, {
                page: currentPage,
                size: pageSize,
                startDate: filters.startDate,
                endDate: filters.endDate,
                type: filters.type,
                keyword: filters.keyword,
                includeRunningBalance: true
            });
            
            if (!stmt || !stmt.entries || stmt.entries.length === 0) {
                entriesContainer.innerHTML = `
                    <div style="text-align: center; padding: 60px 20px; color: #999;">
                        <i class="fas fa-inbox" style="font-size: 3rem; margin-bottom: 16px; opacity: 0.5;"></i>
                        <div style="font-size: 1.1rem; font-weight: 600; margin-bottom: 8px;">İşlem bulunamadı</div>
                        <div style="font-size: 0.9rem;">Seçtiğiniz kriterlere uygun hareket bulunmuyor.</div>
                    </div>
                `;
                paginationContainer.style.display = 'none';
                summary.textContent = '';
                return;
            }
            
            // Özet bilgi
            if (stmt.pagination) {
                const total = stmt.pagination.totalElements || 0;
                summary.textContent = `Toplam ${total} işlem`;
            }
            
            // İşlemleri render et - REST response'dan gelen değerleri direkt kullan
            // Backend'den zaten DESC sırada geliyor (en yeni başta) ve running balance değerleri doğru hesaplanmış
            // Frontend'de sıralama yapmaya gerek yok, backend'den gelen sırayı direkt kullan
            entriesContainer.innerHTML = '';
            const list = document.createElement('div');
            list.style.cssText = 'padding: 0;';
            
            stmt.entries.forEach((entry, index) => {
                // REST response'dan gelen değerleri direkt kullan - hiçbir hesaplama yapma
                const isDebit = entry.entryType === 'DEBIT';
                
                // Amount - response'dan direkt al
                const amount = entry.amount != null ? Number(entry.amount) : 0;
                
                // Running Balance - response'dan direkt al (backend'den BigDecimal, API Gateway float64'e dönüştürüyor)
                // runningBalance değeri bu işlemden SONRA kalan bakiyeyi gösterir
                let runningBalance = null;
                if (entry.runningBalance != null && entry.runningBalance !== undefined) {
                    // String veya number olarak gelebilir, her iki durumu da handle et
                    if (typeof entry.runningBalance === 'string') {
                        const parsed = parseFloat(entry.runningBalance);
                        if (!isNaN(parsed) && isFinite(parsed)) {
                            runningBalance = parsed;
                        }
                    } else if (typeof entry.runningBalance === 'number') {
                        runningBalance = entry.runningBalance;
                    }
                }
                
                // Tarih ve saat - response'dan direkt al
                // entry.date = postingDate (LocalDate format: YYYY-MM-DD)
                // entry.transactionTime = createdAt (Instant format: ISO 8601)
                let dateStr = '';
                let timeStr = '';
                
                // transactionTime öncelikli (daha detaylı, saat bilgisi içerir)
                if (entry.transactionTime) {
                    try {
                        const dateTime = new Date(entry.transactionTime);
                        if (!isNaN(dateTime.getTime())) {
                            dateStr = dateTime.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' });
                            timeStr = dateTime.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });
                        }
                    } catch (e) {
                        // Ignore parse errors
                    }
                }
                
                // transactionTime yoksa date kullan
                if (!dateStr && entry.date) {
                    try {
                        // LocalDate format: YYYY-MM-DD
                        const date = new Date(entry.date + 'T00:00:00');
                        if (!isNaN(date.getTime())) {
                            dateStr = date.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' });
                        } else {
                            dateStr = entry.date;
                        }
                    } catch (e) {
                        dateStr = entry.date;
                    }
                }
                
                const item = document.createElement('div');
                item.style.cssText = 'padding: 20px; border-bottom: 1px solid #f0f0f0; transition: all 0.3s;';
                item.onmouseover = () => item.style.background = '#fafafa';
                item.onmouseout = () => item.style.background = 'white';
                
                item.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;">
                        <div style="flex: 1; min-width: 0;">
                            <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
                                <div style="width: 40px; height: 40px; background: ${isDebit ? 'rgba(211, 47, 47, 0.1)' : 'rgba(76, 175, 80, 0.1)'}; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                                    <i class="fas ${isDebit ? 'fa-arrow-up' : 'fa-arrow-down'}" style="color: ${isDebit ? '#d32f2f' : '#4caf50'}; font-size: 18px;"></i>
                                </div>
                                <div style="flex: 1; min-width: 0;">
                                    <div style="font-weight: 600; color: #1a1a1a; font-size: 1rem; margin-bottom: 4px;">${escapeHtml(entry.description || 'İşlem')}</div>
                                    ${entry.referenceNumber ? `<div style="font-size: 0.8rem; color: #999; font-family: monospace;">Ref: ${escapeHtml(entry.referenceNumber)}</div>` : ''}
                                </div>
                            </div>
                            <div style="display: flex; align-items: center; gap: 16px; margin-top: 8px;">
                                <div style="font-size: 0.85rem; color: #666;">
                                    <i class="fas fa-calendar" style="margin-right: 4px;"></i>${dateStr}
                                </div>
                                ${timeStr ? `<div style="font-size: 0.85rem; color: #666;"><i class="fas fa-clock" style="margin-right: 4px;"></i>${timeStr}</div>` : ''}
                                <div style="font-size: 0.85rem; color: #666; padding: 4px 8px; background: #f5f5f5; border-radius: 4px; font-weight: 600;">
                                    ${isDebit ? 'Çıkış' : 'Giriş'}
                                </div>
                            </div>
                        </div>
                        <div style="text-align: right; min-width: 140px; flex-shrink: 0;">
                            <div style="font-weight: 700; color: ${isDebit ? '#d32f2f' : '#4caf50'}; font-size: 1.1rem; margin-bottom: 4px;">
                                ${isDebit ? '-' : '+'}${Math.abs(amount).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${stmt.currency || 'TL'}
                            </div>
                            ${runningBalance !== null ? `<div style="font-size: 0.85rem; color: #666; font-weight: 600;">Bakiye: ${runningBalance.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${stmt.currency || 'TL'}</div>` : ''}
                        </div>
                    </div>
                `;
                list.appendChild(item);
            });
            
            entriesContainer.appendChild(list);
            
            // Pagination
            if (stmt.pagination && stmt.pagination.totalPages > 1) {
                paginationContainer.style.display = 'flex';
                const current = stmt.pagination.page + 1;
                const total = stmt.pagination.totalPages;
                pageInfo.textContent = `Sayfa ${current} / ${total}`;
                
                const prevBtn = document.getElementById('stmt-prev-btn');
                const nextBtn = document.getElementById('stmt-next-btn');
                
                prevBtn.disabled = currentPage === 0;
                prevBtn.style.opacity = currentPage === 0 ? '0.5' : '1';
                prevBtn.style.cursor = currentPage === 0 ? 'not-allowed' : 'pointer';
                
                nextBtn.disabled = currentPage >= total - 1;
                nextBtn.style.opacity = currentPage >= total - 1 ? '0.5' : '1';
                nextBtn.style.cursor = currentPage >= total - 1 ? 'not-allowed' : 'pointer';
            } else {
                paginationContainer.style.display = 'none';
            }
            
        } catch (err) {
            console.error('loadStatement error:', err);
            entriesContainer.innerHTML = `
                <div style="text-align: center; padding: 60px 20px; color: #d32f2f;">
                    <i class="fas fa-exclamation-circle" style="font-size: 3rem; margin-bottom: 16px;"></i>
                    <div style="font-size: 1.1rem; font-weight: 600; margin-bottom: 8px;">Hareketler yüklenemedi</div>
                    <div style="font-size: 0.9rem;">${err.message || 'Bir hata oluştu'}</div>
                </div>
            `;
            paginationContainer.style.display = 'none';
            showToast('Hesap hareketleri yüklenirken hata oluştu.', 'error');
        }
    }
    
    // Event listeners
    document.getElementById('stmt-modal-close').addEventListener('click', () => modal.remove());
    document.getElementById('stmt-filter-btn').addEventListener('click', showFilterModal);
    document.getElementById('stmt-prev-btn').addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            loadStatement();
        }
    });
    document.getElementById('stmt-next-btn').addEventListener('click', () => {
        currentPage++;
        loadStatement();
    });
    
    // İlk yükleme
    loadStatement();
}

async function viewAccountDetails(accountId) {
    if (!accountId) return;
    try {
        // Hesap detayını ve bakiyeyi paralel olarak çek
        const [detail, balanceRes] = await Promise.all([
            apiCall(`/v1/accounts/${accountId}`, 'GET', null, true),
            apiCall(`/api/v1/ledger/balances/${accountId}`, 'GET', null, true).catch(() => null)
        ]);
        
        // Veri hazırlama
        const iban = detail.accountNumber || 'N/A';
        const productCode = detail.productCode || 'N/A';
        const status = detail.status || 'UNKNOWN';
        const currency = detail.currency || 'TRY';
        const configurations = detail.configurations || {};
        
        // Bakiye bilgisi
        const balance = balanceRes ? (balanceRes.availableBalance || balanceRes.balance || 0) : 0;
        const blockedAmount = balanceRes ? (balanceRes.blockedAmount || 0) : 0;
        const totalBalance = balanceRes ? (balanceRes.balance || 0) : 0;
        
        // Tarihlerden saati kaldır (sadece tarih)
        const createdAt = detail.createdAt 
            ? new Date(detail.createdAt).toLocaleDateString('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' })
            : 'N/A';
        const updatedAt = detail.updatedAt 
            ? new Date(detail.updatedAt).toLocaleDateString('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' })
            : 'N/A';
        
        // Status badge
        let statusBadge = '';
        let statusColor = '#666';
        if (status === 'ACTIVE') {
            statusBadge = 'Aktif';
            statusColor = '#4caf50';
        } else if (status === 'CLOSED') {
            statusBadge = 'Kapatılmış';
            statusColor = '#f44336';
        } else if (status === 'FROZEN') {
            statusBadge = 'Dondurulmuş';
            statusColor = '#ff9800';
        } else if (status === 'PENDING') {
            statusBadge = 'Beklemede';
            statusColor = '#9e9e9e';
        } else {
            statusBadge = status;
        }
        
        // Ürün adı
        let productName = productCode;
        if (productCode === 'CHECKING_TRY') {
            productName = 'Vadesiz TL Hesabı';
        } else if (productCode === 'SAVINGS_TRY') {
            productName = 'Vadeli TL Hesabı';
        } else if (productCode === 'GOLD_ACCOUNT') {
            productName = 'Altın Hesabı';
        }
        
        // Yapılandırma bilgileri
        let configHTML = '';
        if (configurations && Object.keys(configurations).length > 0) {
            const configItems = Object.entries(configurations).map(([key, value]) => {
                const label = key === 'emailNotifications' ? 'E-posta Bildirimleri' :
                             key === 'smsNotifications' ? 'SMS Bildirimleri' :
                             key === 'dailyTransactionLimit' ? 'Günlük İşlem Limiti' :
                             key === 'dailyWithdrawalLimit' ? 'Günlük Çekim Limiti' :
                             key;
                const displayValue = typeof value === 'boolean' ? (value ? 'Açık' : 'Kapalı') :
                                    typeof value === 'number' ? value.toLocaleString('tr-TR') + ' ₺' :
                                    value;
                return `<div>
                    <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">${label}</div>
                    <div style="font-size: 1rem; font-weight: 600; color: #222;">${displayValue}</div>
                </div>`;
            }).join('');
            
            configHTML = `
                <div style="background: #f5f5f5; padding: 16px; border-radius: 8px; margin-top: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Hesap Ayarları</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        ${configItems}
                    </div>
                </div>
            `;
        }
        
        // Action button (sadece ACTIVE hesaplar için kapatma butonu)
        const actionButtonHTML = status === 'ACTIVE' 
            ? `<button class="btn" id="account-close" style="background: #f44336; color: white; flex: 1;">Hesabı Kapat</button>`
            : '';
        
        const modal = document.createElement('div');
        modal.style.position = 'fixed';
        modal.style.left = '0';
        modal.style.top = '0';
        modal.style.width = '100%';
        modal.style.height = '100%';
        modal.style.background = 'rgba(0,0,0,0.6)';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.zIndex = '9999';
        modal.style.padding = '20px';
        modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

        const box = document.createElement('div');
        box.style.background = 'white';
        box.style.color = '#222';
        box.style.borderRadius = '16px';
        box.style.width = '100%';
        box.style.maxWidth = '700px';
        box.style.maxHeight = '90vh';
        box.style.overflow = 'auto';
        box.style.boxShadow = '0 20px 60px rgba(0,0,0,0.3)';
        
        box.innerHTML = `
            <!-- Hesap Görseli -->
            <div style="background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); color: #ffffff; padding: 32px; border-radius: 16px 16px 0 0; position: relative; min-height: 180px;">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div style="width: 10px; height: 10px; background: #4a90e2; border-radius: 50%;"></div>
                        <div style="font-size: 0.8rem; opacity: 0.9;">TRIOBANK</div>
                    </div>
                    <div style="font-weight: 700; font-size: 1.3rem; letter-spacing: 2px;">HESAP</div>
                </div>
                
                <div style="font-family: 'Courier New', monospace; font-size: 1.4rem; font-weight: 600; letter-spacing: 2px; margin-bottom: 16px; word-spacing: 4px;">
                    ${iban}
                </div>
                
                <div style="display: flex; justify-content: space-between; align-items: flex-end;">
                    <div>
                        <div style="font-size: 0.7rem; opacity: 0.8; margin-bottom: 4px;">BAKIYE</div>
                        <div style="font-size: 1.5rem; font-weight: 700;">${parseFloat(balance).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}</div>
                    </div>
                    <div style="text-align: right;">
                        <div style="font-size: 0.7rem; opacity: 0.8; margin-bottom: 4px;">ÜRÜN</div>
                        <div style="font-size: 1rem; font-weight: 600; text-transform: uppercase;">${productName}</div>
                    </div>
                </div>
            </div>
            
            <!-- Detay Bilgileri -->
            <div style="padding: 24px;">
                <!-- Header -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                    <h2 style="margin: 0; font-size: 1.5rem; color: #222;">Hesap Detayları</h2>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="background: ${statusColor}; color: white; padding: 6px 12px; border-radius: 20px; font-size: 0.85rem; font-weight: 600;">${statusBadge}</span>
                        <button class="btn secondary" id="account-close-btn" style="padding: 8px 16px;">✕</button>
                    </div>
                </div>
                
                <!-- Bakiye Bilgileri -->
                <div style="background: #f9f9f9; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Bakiye Bilgileri</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Kullanılabilir Bakiye</div>
                            <div style="font-size: 1.2rem; font-weight: 600; color: #222;">${parseFloat(balance).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Toplam Bakiye</div>
                            <div style="font-size: 1.2rem; font-weight: 600; color: #222;">${parseFloat(totalBalance).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}</div>
                        </div>
                        ${blockedAmount > 0 ? `
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Bloke Tutar</div>
                            <div style="font-size: 1.1rem; font-weight: 600; color: #f44336;">${parseFloat(blockedAmount).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}</div>
                        </div>
                        ` : ''}
                    </div>
                </div>
                
                <!-- Genel Bilgiler -->
                <div style="background: #f9f9f9; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
                    <h4 style="margin: 0 0 12px 0; color: #222; font-size: 1rem;">Genel Bilgiler</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">IBAN</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222; font-family: 'Courier New', monospace;">${iban}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Ürün Tipi</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${productName}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Para Birimi</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${currency}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Hesap Durumu</div>
                            <div style="font-size: 1rem; font-weight: 600; color: ${statusColor};">${statusBadge}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Oluşturulma Tarihi</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${createdAt}</div>
                        </div>
                        <div>
                            <div style="font-size: 0.75rem; color: #666; margin-bottom: 4px;">Son Güncelleme</div>
                            <div style="font-size: 1rem; font-weight: 600; color: #222;">${updatedAt}</div>
                        </div>
                    </div>
                </div>
                
                ${configHTML}
                
                <!-- Action Buttons -->
                ${actionButtonHTML ? `
                <div style="display: flex; gap: 12px; margin-top: 24px; padding-top: 24px; border-top: 1px solid #e0e0e0;">
                    ${actionButtonHTML}
                </div>
                ` : ''}
            </div>
        `;
        
        modal.appendChild(box);
        document.body.appendChild(modal);
        
        // Event listeners
        document.getElementById('account-close-btn').addEventListener('click', () => modal.remove());
        
        if (status === 'ACTIVE' && actionButtonHTML) {
            document.getElementById('account-close').addEventListener('click', () => {
                modal.remove();
                
                // Bakiye kontrolü
                if (balance > 0 || totalBalance > 0) {
                    showToast(
                        currentLang === 'tr' 
                            ? `Hesap kapatılamaz. Hesapta ${parseFloat(balance > 0 ? balance : totalBalance).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency} bulunmaktadır. Hesabı kapatmak için bakiyenin 0 olması gerekir.`
                            : `Account cannot be closed. Account has ${parseFloat(balance > 0 ? balance : totalBalance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}. Balance must be 0 to close the account.`,
                        'error'
                    );
                    return;
                }
                
                showConfirmModal(
                    currentLang === 'tr' ? 'Hesabı Kapat' : 'Close Account',
                    currentLang === 'tr' ? 'Bu hesabı kapatmak istediğinize emin misiniz? Bu işlem geri alınamaz.' : 'Are you sure you want to close this account? This action cannot be undone.',
                    async () => {
                        try {
                            await apiCall(`/v1/accounts/${accountId}/status`, 'PATCH', {
                                status: 'CLOSED',
                                reason: 'Müşteri talimatı ile kapatıldı'
                            }, true);
                            showToast(currentLang === 'tr' ? 'Hesap başarıyla kapatıldı.' : 'Account closed successfully.', 'success');
                            // refresh list to remove closed account (force refresh to bypass cache)
                            await loadAccounts(true);
                        } catch (err) {
                            console.error('closeAccount error:', err);
                            const errorMsg = err.message || (err.response && err.response.message) || '';
                            if (errorMsg.includes('para') || errorMsg.includes('bakiye') || errorMsg.includes('balance')) {
                                showToast(
                                    currentLang === 'tr' 
                                        ? 'Hesap kapatılamaz. Hesapta para bulunmaktadır. Hesabı kapatmak için bakiyenin 0 olması gerekir.'
                                        : 'Account cannot be closed. Account has balance. Balance must be 0 to close the account.',
                                    'error'
                                );
                            } else {
                                showToast(currentLang === 'tr' ? 'Hesap kapatılırken hata oluştu.' : 'Error closing account.', 'error');
                            }
                        }
                    }
                );
            });
        }
    } catch (err) {
        console.error('viewAccountDetails error:', err);
        showToast(currentLang === 'tr' ? 'Hesap detayları yüklenirken hata oluştu.' : 'Error loading account details.', 'error');
    }
}

async function createAccount() {
    if (!currentUser || !currentUser.user_id) {
        showToast(currentLang === 'tr' ? 'Oturum bulunamadı. Lütfen giriş yapın.' : 'No active session. Please login.', 'error');
        return;
    }

    // Simple flow: create a TRY checking account by default. Use productCode per API Gateway model.
    // NOTE: productCode may need to be adjusted to a real product code from /v1/products. This is a conservative placeholder.
    // Use client id as customerId if available; otherwise try to fetch client record first
    let customerId = (currentClient && (currentClient.id || currentClient.ID)) ? (currentClient.id || currentClient.ID) : null;
    if (!customerId) {
        // try to load client info once synchronously
        const ok = await loadClientInfo();
        if (ok && currentClient && (currentClient.id || currentClient.ID)) {
            customerId = currentClient.id || currentClient.ID;
        }
    }

    if (!customerId) {
        showToast(currentLang === 'tr' ? 'Müşteri kaydınız henüz oluşturulmamış. Lütfen profil oluşturulduktan sonra tekrar deneyin.' : 'Your client record is not yet created. Please try again after your profile is available.', 'error');
        return;
    }

    const payload = { customerId: customerId, currency: 'TRY', productCode: 'CHECKING_TRY' };
    try {
        const res = await apiCall('/v1/accounts', 'POST', payload, true);
        showToast(currentLang === 'tr' ? 'Hesap başarıyla oluşturuldu.' : 'Account created successfully.', 'success');
        // refresh list to show new account with updated balances (force refresh to bypass cache)
        await loadAccounts(true);
    } catch (err) {
        console.error('createAccount error:', err);
        const errorMsg = err.message || (err.response && err.response.message) || (err.status === 400 ? 'Geçersiz istek. Lütfen bilgileri kontrol edin.' : 'Hesap oluşturulurken hata oluştu.');
        showToast(currentLang === 'tr' ? errorMsg : (errorMsg.includes('Geçersiz') ? 'Invalid request. Please check the information.' : 'Error creating account.'), 'error');
    }
}

async function closeAccount(accountId) {
    if (!accountId) return;
    
    // Önce bakiyeyi kontrol et
    try {
        const balanceRes = await apiCall(`/api/v1/ledger/balances/${accountId}`, 'GET', null, true).catch(() => null);
        const balance = balanceRes ? (balanceRes.availableBalance || balanceRes.balance || 0) : 0;
        
        if (balance > 0) {
            showToast(
                currentLang === 'tr' 
                    ? `Hesap kapatılamaz. Hesapta ${parseFloat(balance).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${balanceRes?.currency || 'TRY'} bulunmaktadır. Hesabı kapatmak için bakiyenin 0 olması gerekir.`
                    : `Account cannot be closed. Account has ${parseFloat(balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${balanceRes?.currency || 'TRY'}. Balance must be 0 to close the account.`,
                'error'
            );
            return;
        }
    } catch (err) {
        console.warn('Failed to fetch balance for account close check:', err);
        // Bakiye çekilemezse devam et, backend kontrol edecek
    }
    
    showConfirmModal(
        currentLang === 'tr' ? 'Hesabı Kapat' : 'Close Account',
        currentLang === 'tr' ? 'Bu hesabı kapatmak istediğinize emin misiniz? Bu işlem geri alınamaz.' : 'Are you sure you want to close this account? This action cannot be undone.',
        async () => {
            try {
                await apiCall(`/v1/accounts/${accountId}/status`, 'PATCH', {
                    status: 'CLOSED',
                    reason: 'Müşteri talimatı ile kapatıldı'
                }, true);
                showToast(currentLang === 'tr' ? 'Hesap başarıyla kapatıldı.' : 'Account closed successfully.', 'success');
                // refresh list to remove closed account (force refresh to bypass cache)
                await loadAccounts(true);
    } catch (err) {
        console.error('closeAccount error:', err);
                const errorMsg = err.message || (err.response && err.response.message) || '';
                if (errorMsg.includes('para') || errorMsg.includes('bakiye') || errorMsg.includes('balance')) {
                    showToast(
                        currentLang === 'tr' 
                            ? 'Hesap kapatılamaz. Hesapta para bulunmaktadır. Hesabı kapatmak için bakiyenin 0 olması gerekir.'
                            : 'Account cannot be closed. Account has balance. Balance must be 0 to close the account.',
                        'error'
                    );
                } else {
                    showToast(currentLang === 'tr' ? 'Hesap kapatılırken hata oluştu.' : 'Error closing account.', 'error');
                }
            }
        }
    );
}

function updateDashboardData() {
    // Eski transaction-container için (transactions-page)
    const container = document.getElementById('transaction-container');
    if (container) {
        container.innerHTML = '';
        transactions.sort((a, b) => b.id - a.id).forEach(t => {
            const el = document.createElement('div');
            el.className = 'trans-item';
            const colorClass = t.type === 'incoming' ? 'positive' : 'negative';
            const prefix = t.type === 'incoming' ? '+' : '-';

            el.innerHTML = `
                <div style="font-weight:600; color:inherit;">${t.title}<br><span style="font-size:0.8rem; opacity:0.7;">${t.date}</span></div>
                <div class="${colorClass}" style="font-weight:bold;">${prefix}${t.amount.toLocaleString('tr-TR')} TL</div>
            `;
            container.appendChild(el);
        });
    }
    
    // Yeni dashboard güncellemeleri
    updateDashboardWelcome();
    updateDashboardTotalBalance();
    updateDashboardRecentTransactions();
}

// Dashboard hoş geldin mesajı ve tarih/saat
function updateDashboardWelcome() {
    const welcomeText = document.getElementById('dashboard-welcome-text');
    
    if (!welcomeText) return;
    
    welcomeText.textContent = 'İyi günler';
}

// Toplam bakiye hesapla ve göster
function updateDashboardTotalBalance() {
    const totalBalanceEl = document.getElementById('dashboard-total-balance');
    if (!totalBalanceEl) return;
    
    if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
        totalBalanceEl.textContent = '0,00';
        return;
    }
    
    const total = window.accountsState.accounts.reduce((sum, acc) => {
        const balance = (acc.availableBalance !== undefined && acc.availableBalance !== null) 
            ? Number(acc.availableBalance) 
            : ((acc.balance !== undefined && acc.balance !== null) ? Number(acc.balance) : 0);
        return sum + balance;
    }, 0);
    
    totalBalanceEl.textContent = total.toLocaleString('tr-TR', { 
        minimumFractionDigits: 2, 
        maximumFractionDigits: 2 
    });
}

// Ana hesap kartını güncelle
function updateDashboardMainAccount() {
    if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
        // Hesap yoksa kartı gizle veya mesaj göster
        const mainCard = document.getElementById('dashboard-main-account-card');
        if (mainCard) {
            mainCard.style.display = 'none';
        }
        return;
    }
    
    // İlk hesabı al (veya en çok kullanılan)
    const mainAccount = window.accountsState.accounts[0];
    if (!mainAccount) return;
    
    const accountTypeEl = document.getElementById('dashboard-account-type');
    const accountNumberEl = document.getElementById('dashboard-account-number');
    const branchNameEl = document.getElementById('dashboard-branch-name');
    const balanceEl = document.getElementById('total-balance-mobile');
    
    if (accountTypeEl) {
        const accountType = mainAccount.type || mainAccount.product || 'Vadesiz TL';
        accountTypeEl.textContent = accountType;
    }
    
    if (accountNumberEl) {
        const accountNumber = mainAccount.accountNumber || mainAccount.id || '';
        accountNumberEl.textContent = accountNumber;
    }
    
    if (branchNameEl) {
        branchNameEl.textContent = mainAccount.branchName || mainAccount.branch || 'KAYU ŞUBESİ';
    }
    
    if (balanceEl) {
        const balance = (mainAccount.availableBalance !== undefined && mainAccount.availableBalance !== null) 
            ? Number(mainAccount.availableBalance) 
            : ((mainAccount.balance !== undefined && mainAccount.balance !== null) ? Number(mainAccount.balance) : 0);
        balanceEl.textContent = balance.toLocaleString('tr-TR', { 
            minimumFractionDigits: 2, 
            maximumFractionDigits: 2 
        });
    }
    
    // IBAN'ı güncelle
    const ibanEl = document.getElementById('ibanTextMobile');
    if (ibanEl) {
        const iban = mainAccount.iban || mainAccount.Iban || mainAccount.accountNumber || '';
        ibanEl.textContent = iban;
    }
}

// Son işlemler önizlemesi (son 3-5 işlem)
async function updateDashboardRecentTransactions() {
    const transactionsListEl = document.getElementById('dashboard-transactions-list');
    if (!transactionsListEl) return;
    
    try {
        // Hesapları kontrol et
        if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
            transactionsListEl.innerHTML = `
                <div style="text-align: center; padding: 20px; color: #999;">
                    <i class="fas fa-info-circle" style="font-size: 1.5rem; margin-bottom: 10px;"></i>
                    <div>Hesap bilgileri yükleniyor...</div>
                </div>
            `;
            return;
        }
        
        // Tüm hesaplardan statement entry'lerini topla
        const allEntries = [];
        for (const account of window.accountsState.accounts) {
            const accountId = account.id || account.accountId || account.account_id;
            if (!accountId) continue;
            
            try {
                // Her hesaptan son 10 entry al (sonra en son 5'ini seçeceğiz)
                const statement = await fetchStatement(accountId, {
                    page: 0,
                    size: 10,
                    includeRunningBalance: true
                });
                
                if (statement && statement.entries && statement.entries.length > 0) {
                    statement.entries.forEach(entry => {
                        // Hesap bilgisini ekle
                        const entryWithAccount = {
                            ...entry,
                            accountId: accountId,
                            accountNumber: account.accountNumber || account.id || accountId
                        };
                        allEntries.push(entryWithAccount);
                    });
                }
            } catch (e) {
                console.debug('Failed to load statement for account', accountId, e);
                // Hata olsa bile devam et, diğer hesapları yükle
            }
        }
        
        if (allEntries.length === 0) {
            transactionsListEl.innerHTML = `
                <div style="text-align: center; padding: 20px; color: #999;">
                    <i class="fas fa-history" style="font-size: 1.5rem; margin-bottom: 10px;"></i>
                    <div>Henüz işlem bulunmuyor</div>
                </div>
            `;
            return;
        }
        
        // Tarihe göre sırala (en yeni en üstte)
        allEntries.sort((a, b) => {
            const dateA = a.transactionTime ? new Date(a.transactionTime).getTime() : (a.date ? new Date(a.date).getTime() : 0);
            const dateB = b.transactionTime ? new Date(b.transactionTime).getTime() : (b.date ? new Date(b.date).getTime() : 0);
            return dateB - dateA;
        });
        
        // İlk 3'ü al
        const recentEntries = allEntries.slice(0, 3);
        
        // Render et
        transactionsListEl.innerHTML = '';
        recentEntries.forEach(entry => {
            const isDebit = entry.entryType === 'DEBIT';
            const amount = entry.amount ? Number(entry.amount) : 0;
            const colorClass = isDebit ? '#d32f2f' : '#4caf50';
            const prefix = isDebit ? '-' : '+';
            const icon = isDebit ? 'fa-arrow-up' : 'fa-arrow-down';
            
            // Tarih formatla
            let dateStr = '';
            let timeStr = '';
            if (entry.transactionTime) {
                try {
                    const date = new Date(entry.transactionTime);
                    const now = new Date();
                    const diffMs = now - date;
                    const diffMins = Math.floor(diffMs / 60000);
                    const diffHours = Math.floor(diffMs / 3600000);
                    const diffDays = Math.floor(diffMs / 86400000);
                    
                    if (diffMins < 1) {
                        dateStr = 'Az önce';
                    } else if (diffMins < 60) {
                        dateStr = `${diffMins} dakika önce`;
                    } else if (diffHours < 24) {
                        dateStr = `${diffHours} saat önce`;
                    } else if (diffDays === 1) {
                        dateStr = 'Dün';
                    } else if (diffDays < 7) {
                        dateStr = `${diffDays} gün önce`;
                    } else {
                        dateStr = date.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
                        timeStr = date.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });
                    }
                } catch (e) {
                    dateStr = entry.date || '';
                }
            } else if (entry.date) {
                try {
                    const date = new Date(entry.date);
                    dateStr = date.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
                } catch (e) {
                    dateStr = entry.date;
                }
            }
            
            const item = document.createElement('div');
            item.style.cssText = 'display: flex; justify-content: space-between; align-items: flex-start; padding: 16px 0; border-bottom: 1px solid #f0f0f0; transition: all 0.3s;';
            item.onmouseover = () => item.style.background = '#fafafa';
            item.onmouseout = () => item.style.background = 'transparent';
            
            item.innerHTML = `
                <div style="display: flex; align-items: flex-start; gap: 12px; flex: 1; min-width: 0;">
                    <div style="width: 40px; height: 40px; background: ${isDebit ? 'rgba(211, 47, 47, 0.1)' : 'rgba(76, 175, 80, 0.1)'}; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                        <i class="fas ${icon}" style="color: ${colorClass}; font-size: 18px;"></i>
                    </div>
                    <div style="flex: 1; min-width: 0;">
                        <div style="font-weight: 600; color: #1a1a1a; font-size: 0.95rem; margin-bottom: 4px;">${escapeHtml(entry.description || 'İşlem')}</div>
                        <div style="font-size: 0.8rem; color: #666; margin-bottom: 2px; font-family: monospace;">${escapeHtml(entry.accountNumber || entry.accountId || '')}</div>
                        ${entry.referenceNumber ? `<div style="font-size: 0.75rem; color: #999; font-family: monospace;">Ref: ${escapeHtml(entry.referenceNumber)}</div>` : ''}
                        <div style="font-size: 0.75rem; color: #999; margin-top: 4px;">${dateStr}${timeStr ? ' ' + timeStr : ''}</div>
                    </div>
                </div>
                <div style="text-align: right; min-width: 100px; flex-shrink: 0;">
                    <div style="font-weight: 700; color: ${colorClass}; font-size: 1rem;">${prefix}${Math.abs(amount).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} TL</div>
                </div>
            `;
            transactionsListEl.appendChild(item);
        });
        
    } catch (err) {
        console.error('updateDashboardRecentTransactions error:', err);
        transactionsListEl.innerHTML = `
            <div style="text-align: center; padding: 20px; color: #999;">
                <i class="fas fa-exclamation-circle" style="font-size: 1.5rem; margin-bottom: 10px;"></i>
                <div>İşlemler yüklenemedi</div>
            </div>
        `;
    }
}

// --- TRANSACTIONS (integrate with Transaction Service via API Gateway) ---

// Create a transfer via Transaction Service
// payload: { fromAccountId, toAccountId, amount, currency, reference, description, idempotencyKey }
async function createTransfer(payload) {
    if (!payload || !payload.fromAccountId || !payload.toAccountId || !payload.amount) throw new Error('fromAccountId, toAccountId and amount required');
    // POST /v1/transactions/transfer
    const res = await apiCall('/v1/transactions/transfer', 'POST', payload, true);
    return res;
}

// Get transaction by id
async function getTransaction(txnId) {
    if (!txnId) throw new Error('txnId required');
    const res = await apiCall(`/v1/transactions/${encodeURIComponent(txnId)}`, 'GET', null, true);
    return res;
}

// List transactions for accountId with optional paging and filters
// opts: { page, size, status, sort }
async function listTransactions(accountId, opts = {}) {
    if (!accountId) throw new Error('accountId required');
    const q = { accountId };
    if (opts.page != null) q.page = opts.page;
    if (opts.size != null) q.size = opts.size;
    if (opts.status) q.status = opts.status;
    if (opts.sort) q.sort = opts.sort;

    const query = buildQuery(q);
    const endpoint = `/v1/transactions${query ? '?' + query : ''}`;
    const res = await apiCall(endpoint, 'GET', null, true);
    // Expect PageTransactionResponse { content: [...] }
    if (res && res.content) return res.content;
    return Array.isArray(res) ? res : (res.items || []);
}

// Load transactions for the current user (uses customer's first account if available)
async function loadTransactionsForCurrentUser() {
    const container = document.getElementById('transaction-container');
    if (!container) return;
    container.innerHTML = '<div style="padding:12px;">Yükleniyor...</div>';

    try {
        // Ensure accounts are loaded; get account list. Prefer client id.
        let accountsList = [];
        try {
            const customerId = (currentClient && (currentClient.id || currentClient.ID)) ? (currentClient.id || currentClient.ID) : (currentUser && currentUser.user_id);
            const accRes = await apiCall(`/v1/accounts?customerId=${customerId}`, 'GET', null, true);
            accountsList = Array.isArray(accRes) ? accRes : (accRes.items || []);
        } catch (e) {
            // fallback: try loadAccounts which will render accounts (and may attempt to fetch client first)
            try { await loadAccounts(); } catch (ee) { }
        }

        const accountId = (accountsList && accountsList.length) ? (accountsList[0].id || accountsList[0].accountId || accountsList[0].account_id) : null;
        if (!accountId) {
            container.innerHTML = '<div style="padding:12px;">Hesap bulunamadı. Lütfen önce bir hesap oluşturun veya yenileyin.</div>';
            return;
        }

        const txs = await listTransactions(accountId, { size: 20, sort: 'createdAt,desc' });
        transactions = txs.map(t => normalizeTransactionForUI(t));
        renderTransactions(transactions);
    } catch (err) {
        console.error('loadTransactionsForCurrentUser error', err);
        document.getElementById('transaction-container').innerHTML = '<div style="padding:12px;">Hareketler yüklenemedi.</div>';
    }
}

function normalizeTransactionForUI(t) {
    // Normalize fields for legacy UI expectations
    return {
        id: t.id || t.transactionId || t.txnId || t.transaction_id,
        date: t.createdAt || t.created_at || t.transactionTime || t.timestamp,
        title: t.description || t.referenceNumber || t.type || (t.transactionType || ''),
        amount: (t.totalAmount != null) ? Number(t.totalAmount) : (t.amount != null ? Number(t.amount) : 0),
        currency: t.currency || t.ccy || 'TRY',
        status: t.status || '',
        raw: t
    };
}

function renderTransactions(list) {
    const container = document.getElementById('transaction-container');
    if (!container) return;
    container.innerHTML = '';
    if (!list || list.length === 0) {
        container.innerHTML = '<div style="padding:12px;">İşlem bulunamadı.</div>';
        return;
    }

    list.forEach(t => {
        const el = document.createElement('div');
        el.className = 'trans-item';
        const colorClass = (t.amount >= 0) ? 'positive' : 'negative';
        const prefix = (t.amount >= 0) ? '+' : '-';
        el.style.display = 'flex';
        el.style.justifyContent = 'space-between';
        el.style.alignItems = 'center';
        el.style.padding = '10px';
        el.style.borderBottom = '1px solid rgba(255,255,255,0.04)';

        el.innerHTML = `
            <div style="flex:1"> <div style="font-weight:600; color:inherit;">${escapeHtml(t.title || t.id)}</div><div style="font-size:0.8rem; opacity:0.7;">${escapeHtml(t.date || '')}</div></div>
            <div style="margin-left:12px; text-align:right; min-width:120px;"><div class="${colorClass}" style="font-weight:bold;">${prefix}${Number(Math.abs(t.amount || 0)).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${t.currency || ''}</div><div style="margin-top:6px;"><button class=\"btn small\" onclick=\"(function(id){ showTransactionDetail(id); })('${t.id}')\">Detay</button></div></div>
        `;

        container.appendChild(el);
    });
}

async function showTransactionDetail(txnId) {
    if (!txnId) return;
    try {
        const txn = await getTransaction(txnId);
        const modal = document.createElement('div');
        modal.style.position = 'fixed'; modal.style.left='0'; modal.style.top='0'; modal.style.width='100%'; modal.style.height='100%'; modal.style.background='rgba(0,0,0,0.5)'; modal.style.display='flex'; modal.style.alignItems='center'; modal.style.justifyContent='center'; modal.style.zIndex='9999';
        modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

        const box = document.createElement('div');
        box.style.background = 'white'; box.style.color='#222'; box.style.padding='16px'; box.style.borderRadius='8px'; box.style.width='90%'; box.style.maxWidth='720px'; box.style.maxHeight='80vh'; box.style.overflow='auto';
        box.innerHTML = `<h3>İşlem Detayı</h3><pre style="white-space:pre-wrap;">${escapeHtml(JSON.stringify(txn, null, 2))}</pre><div style="display:flex; gap:8px; margin-top:10px; justify-content:flex-end;"><button class="btn" id="txn-close">Kapat</button></div>`;
        modal.appendChild(box);
        document.body.appendChild(modal);
        document.getElementById('txn-close').addEventListener('click', () => modal.remove());
    } catch (e) {
        console.error('showTransactionDetail error', e);
        showToast('İşlem detayı alınamadı.', 'error');
    }
}

// Hesaplarım Arası Transfer Modal
async function showBetweenAccountsTransfer() {
    // Önce hesapları yükle (state'ten veya backend'den)
    if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
        await loadAccounts();
    }
    
    // State'ten güncel bakiyeleri kullan
    if (window.accountsState && window.accountsState.accounts) {
        window.currentAccounts = window.accountsState.accounts;
    }
    
    if (!window.currentAccounts || window.currentAccounts.length < 2) {
        showToast('Hesaplarınız arasında transfer yapabilmek için en az 2 hesabınız olmalıdır.', 'error');
        return;
    }

    const modal = document.createElement('div');
    modal.style.position='fixed'; modal.style.left='0'; modal.style.top='0'; modal.style.width='100%'; modal.style.height='100%'; modal.style.background='rgba(0,0,0,0.5)'; modal.style.display='flex'; modal.style.alignItems='center'; modal.style.justifyContent='center'; modal.style.zIndex='9999';
    modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

    const box = document.createElement('div');
    box.style.background='white'; box.style.color='#222'; box.style.padding='24px'; box.style.borderRadius='12px'; box.style.width='95%'; box.style.maxWidth='520px'; box.style.maxHeight='90vh'; box.style.overflowY='auto';
    
    // Hesap seçenekleri için dropdown HTML
    const formatBalance = (bal) => {
        if (!bal && bal !== 0) return '0,00';
        return parseFloat(bal).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };
    const fromAccountOptions = window.currentAccounts.map(acc => 
        `<option value="${acc.id}">${acc.accountNumber || acc.id} - ${formatBalance(acc.balance || 0)} TL</option>`
    ).join('');
    const toAccountOptions = window.currentAccounts.map(acc => 
        `<option value="${acc.id}">${acc.accountNumber || acc.id} - ${formatBalance(acc.balance || 0)} TL</option>`
    ).join('');

    box.innerHTML = `
        <div style="text-align: center; margin-bottom: 24px;">
            <div style="width: 60px; height: 60px; background: linear-gradient(135deg, #d32f2f 0%, #f44336 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px;">
                <i class="fas fa-exchange-alt" style="color: white; font-size: 24px;"></i>
            </div>
            <h3 style="margin: 0 0 8px 0; color: #1a1a1a; font-size: 1.5rem; font-weight: 700;">Hesaplarım Arası Transfer</h3>
            <p style="margin: 0; color: #666; font-size: 0.9rem;">Hesaplarınız arasında para transferi yapın</p>
        </div>
        
        <div style="background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%); padding: 20px; border-radius: 12px; margin-bottom: 24px; border: 1px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-wallet" style="color: #d32f2f; margin-right: 6px;"></i>Gönderen Hesap
            </label>
            <select id="tr-between-from" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; background: white; cursor: pointer; transition: all 0.3s; font-weight: 500;" onfocus="this.style.borderColor='#d32f2f'; this.style.boxShadow='0 0 0 3px rgba(211, 47, 47, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                ${fromAccountOptions}
            </select>
        </div>
        
        <div style="text-align: center; margin: 16px 0;">
            <i class="fas fa-arrow-down" style="color: #d32f2f; font-size: 24px; opacity: 0.6;"></i>
        </div>
        
        <div style="background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%); padding: 20px; border-radius: 12px; margin-bottom: 24px; border: 1px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-wallet" style="color: #4caf50; margin-right: 6px;"></i>Alıcı Hesap
            </label>
            <select id="tr-between-to" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; background: white; cursor: pointer; transition: all 0.3s; font-weight: 500;" onfocus="this.style.borderColor='#4caf50'; this.style.boxShadow='0 0 0 3px rgba(76, 175, 80, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                ${toAccountOptions}
            </select>
        </div>
        
        <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 24px; border: 2px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-lira-sign" style="color: #d32f2f; margin-right: 6px;"></i>Tutar (TL)
            </label>
            <div style="position: relative;">
                <input type="number" id="tr-between-amount" placeholder="0,00" step="0.01" min="0.01" style="width: 100%; padding: 14px 16px; padding-left: 48px; border: 2px solid #ddd; border-radius: 10px; font-size: 1.2rem; font-weight: 600; transition: all 0.3s;" onfocus="this.style.borderColor='#d32f2f'; this.style.boxShadow='0 0 0 3px rgba(211, 47, 47, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                <span style="position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: #999; font-weight: 600; font-size: 1.2rem;">₺</span>
            </div>
        </div>
        
        <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 24px; border: 2px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-comment-alt" style="color: #666; margin-right: 6px;"></i>Açıklama <span style="color: #999; font-weight: normal; font-size: 0.85rem;">(opsiyonel)</span>
            </label>
            <input type="text" id="tr-between-desc" placeholder="Transfer açıklaması..." maxlength="200" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; transition: all 0.3s;" onfocus="this.style.borderColor='#d32f2f'; this.style.boxShadow='0 0 0 3px rgba(211, 47, 47, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
        </div>
        
        <div style="background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 8px; padding: 16px; margin-bottom: 24px;">
            <div style="display: flex; align-items: start; gap: 12px;">
                <i class="fas fa-shield-alt" style="color: #856404; font-size: 20px; margin-top: 2px;"></i>
                <div style="flex: 1;">
                    <div style="font-weight: 600; color: #856404; margin-bottom: 4px; font-size: 0.9rem;">Güvenli Transfer</div>
                    <div style="font-size: 0.85rem; color: #856404; line-height: 1.5;">Bu işlem SSL ile şifrelenmiş güvenli bağlantı üzerinden gerçekleştirilmektedir.</div>
                </div>
            </div>
        </div>
        
        <div style="display:flex; gap:12px; justify-content:flex-end; padding-top: 20px; border-top: 1px solid #e0e4e8;">
            <button class="btn" id="tr-between-cancel" style="padding: 14px 28px; font-weight: 600; border: 2px solid #ddd; background: white; color: #666;">İptal</button>
            <button class="btn primary" id="tr-between-submit" style="padding: 14px 28px; font-weight: 600; background: linear-gradient(135deg, #d32f2f 0%, #f44336 100%); border: none; box-shadow: 0 4px 12px rgba(211, 47, 47, 0.3);">
                <i class="fas fa-paper-plane" style="margin-right: 8px;"></i>Gönder
            </button>
        </div>
    `;
    modal.appendChild(box);
    document.body.appendChild(modal);

    // Aynı hesap seçilmesini engelle
    const fromSelect = document.getElementById('tr-between-from');
    const toSelect = document.getElementById('tr-between-to');
    fromSelect.addEventListener('change', () => {
        if (fromSelect.value === toSelect.value) {
            toSelect.selectedIndex = (toSelect.selectedIndex + 1) % toSelect.options.length;
        }
    });
    toSelect.addEventListener('change', () => {
        if (fromSelect.value === toSelect.value) {
            fromSelect.selectedIndex = (fromSelect.selectedIndex + 1) % fromSelect.options.length;
        }
    });

    document.getElementById('tr-between-cancel').addEventListener('click', () => modal.remove());
    
    let isSubmittingTransfer = false; // Double-click koruması
    document.getElementById('tr-between-submit').addEventListener('click', async () => {
        if (isSubmittingTransfer) {
            console.log('Transfer already in progress, ignoring click');
            return;
        }
        
        const from = fromSelect.value.trim();
        const to = toSelect.value.trim();
        const amount = parseFloat((document.getElementById('tr-between-amount').value || '').replace(',', '.'));
        const desc = document.getElementById('tr-between-desc').value.trim();
        
        if (!from || !to || !amount || isNaN(amount) || amount <= 0) { 
            showToast('Lütfen doğru bilgiler girin.', 'error'); 
            return; 
        }
        if (from === to) {
            showToast('Gönderen ve alıcı hesap aynı olamaz.', 'error');
            return;
        }

        // Disable button and set flag
        const submitBtn = document.getElementById('tr-between-submit');
        submitBtn.disabled = true;
        submitBtn.style.opacity = '0.6';
        submitBtn.style.cursor = 'not-allowed';
        isSubmittingTransfer = true;

        try {
            const idempotencyKey = 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2,8);
            const payload = { fromAccountId: from, toAccountId: to, amount: amount, currency: 'TRY', description: desc || undefined, idempotencyKey };
            const res = await createTransfer(payload);
            showToast('Transfer talebi gönderildi. İşlem ID: ' + (res && (res.id || res.transactionId || res.transaction_id) || ''), 'success');
            modal.remove();
            const txId = res && (res.id || res.transactionId || res.transaction_id);
            
            // Update balances in state immediately (optimistic update)
            updateBalancesAfterTransfer(from, to, amount);
            
            if (txId) {
                pollTransactionStatus(txId);
            }
            setTimeout(() => { loadTransactionsForCurrentUser(); }, 1500);
        } catch (e) {
            console.error('transfer error', e);
            showToast('Transfer işlemi başarısız: ' + (e.message || 'Bilinmeyen hata'), 'error');
        } finally {
            // Re-enable button
            isSubmittingTransfer = false;
            const submitBtn = document.getElementById('tr-between-submit');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.style.opacity = '1';
                submitBtn.style.cursor = 'pointer';
            }
        }
    });
}

// IBAN validasyon fonksiyonu (Mod-97 algoritması ile)
function validateIBAN(iban) {
    if (!iban) return { valid: false, message: 'IBAN boş olamaz' };
    
    // Boşlukları temizle ve büyük harfe çevir
    const cleanIban = iban.replace(/\s/g, '').toUpperCase();
    
    // TR ile başlamalı
    if (!cleanIban.startsWith('TR')) {
        return { valid: false, message: 'IBAN TR ile başlamalıdır' };
    }
    
    // Toplam uzunluk 26 karakter olmalı (TR + 24 rakam)
    if (cleanIban.length !== 26) {
        return { valid: false, message: 'IBAN 26 karakter olmalıdır (TR + 24 rakam)' };
    }
    
    // Sadece harf ve rakam içermeli
    if (!/^[A-Z0-9]+$/.test(cleanIban)) {
        return { valid: false, message: 'IBAN sadece harf ve rakam içerebilir' };
    }
    
    // İlk 2 karakter TR, sonraki 24 karakter rakam olmalı
    const countryCode = cleanIban.substring(0, 2);
    const rest = cleanIban.substring(2);
    
    if (countryCode !== 'TR') {
        return { valid: false, message: 'IBAN TR ile başlamalıdır' };
    }
    
    if (!/^\d{24}$/.test(rest)) {
        return { valid: false, message: 'IBAN TR\'den sonra 24 rakam olmalıdır' };
    }
    
    // Mod-97 kontrolü (ISO 13616 standardı)
    // 1. İlk 4 karakteri (TR + 2 kontrol hanesi) sona al
    const reordered = cleanIban.substring(4) + cleanIban.substring(0, 4);
    
    // 2. Harfleri sayıya çevir (A=10, B=11, ..., T=29, R=27)
    let numericIban = '';
    for (let i = 0; i < reordered.length; i++) {
        const char = reordered[i];
        if (char >= '0' && char <= '9') {
            numericIban += char;
        } else if (char >= 'A' && char <= 'Z') {
            numericIban += (char.charCodeAt(0) - 'A'.charCodeAt(0) + 10).toString();
        } else {
            return { valid: false, message: 'IBAN geçersiz karakter içeriyor' };
        }
    }
    
    // 3. Mod-97 hesapla (büyük sayılar için chunk'lara böl)
    let remainder = '';
    for (let i = 0; i < numericIban.length; i += 7) {
        const chunk = remainder + numericIban.substring(i, i + 7);
        remainder = (parseInt(chunk) % 97).toString();
    }
    
    // Sonuç 1 olmalı
    if (parseInt(remainder) !== 1) {
        return { valid: false, message: 'IBAN kontrol hanesi geçersiz' };
    }
    
    return { valid: true, cleanIban: cleanIban };
}

// IBAN formatlama (4-4-4-4-4-4-2 formatında)
function formatIBAN(iban) {
    const clean = iban.replace(/\s/g, '').toUpperCase();
    if (clean.length !== 26) return iban;
    return clean.match(/.{1,4}/g).join(' ');
}

// Ad Soyad validasyonu
function validateName(name) {
    if (!name || name.trim().length === 0) {
        return { valid: false, message: 'Ad Soyad boş olamaz' };
    }
    if (name.trim().length < 3) {
        return { valid: false, message: 'Ad Soyad en az 3 karakter olmalıdır' };
    }
    if (name.trim().length > 100) {
        return { valid: false, message: 'Ad Soyad en fazla 100 karakter olabilir' };
    }
    // Sadece harf, boşluk ve Türkçe karakterler
    if (!/^[a-zA-ZçğıöşüÇĞIİÖŞÜ\s]+$/.test(name.trim())) {
        return { valid: false, message: 'Ad Soyad sadece harf içerebilir' };
    }
    return { valid: true };
}

// Başka Hesaba Transfer - Aşama 1: IBAN ve Ad Soyad Girişi
async function showExternalAccountTransfer() {
    console.log('showExternalAccountTransfer called');
    try {
        // Önce hesapları yükle (state'ten veya backend'den)
        if (!window.accountsState || !window.accountsState.accounts || window.accountsState.accounts.length === 0) {
            await loadAccounts();
        }
        
        // State'ten güncel bakiyeleri kullan
        if (window.accountsState && window.accountsState.accounts) {
            window.currentAccounts = window.accountsState.accounts;
        }
        
        if (!window.currentAccounts || window.currentAccounts.length === 0) {
            showToast('Transfer yapabilmek için en az bir hesabınız olmalıdır.', 'error');
            return;
        }

        const modal = document.createElement('div');
        modal.id = 'external-transfer-modal';
        modal.style.position='fixed'; modal.style.left='0'; modal.style.top='0'; modal.style.width='100%'; modal.style.height='100%'; modal.style.background='rgba(0,0,0,0.5)'; modal.style.display='flex'; modal.style.alignItems='center'; modal.style.justifyContent='center'; modal.style.zIndex='9999';
        modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

        const box = document.createElement('div');
        box.style.background='white'; box.style.color='#222'; box.style.padding='24px'; box.style.borderRadius='12px'; box.style.width='95%'; box.style.maxWidth='520px'; box.style.maxHeight='90vh'; box.style.overflowY='auto';
        
        // Gönderen hesap seçenekleri
        const formatBalance = (bal) => {
            if (!bal && bal !== 0) return '0,00';
            return parseFloat(bal).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        };
        const fromAccountOptions = window.currentAccounts.map(acc => 
            `<option value="${acc.id}">${acc.accountNumber || acc.id} - ${formatBalance(acc.balance || 0)} TL</option>`
        ).join('');

        box.innerHTML = `
            <div style="text-align: center; margin-bottom: 24px;">
                <div style="width: 60px; height: 60px; background: linear-gradient(135deg, #d32f2f 0%, #f44336 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px;">
                    <i class="fas fa-paper-plane" style="color: white; font-size: 24px;"></i>
                </div>
                <h3 style="margin: 0 0 8px 0; color: #1a1a1a; font-size: 1.5rem; font-weight: 700;">Başka Hesaba Transfer</h3>
                <p style="margin: 0; color: #666; font-size: 0.9rem;">Alıcı bilgilerini girin</p>
            </div>
            
            <div style="background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%); padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 1px solid #e0e4e8;">
                <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                    <i class="fas fa-wallet" style="color: #d32f2f; margin-right: 6px;"></i>Gönderen Hesap
                </label>
                <select id="tr-external-from" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; background: white; cursor: pointer; transition: all 0.3s; font-weight: 500;" onfocus="this.style.borderColor='#d32f2f'; this.style.boxShadow='0 0 0 3px rgba(211, 47, 47, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                    ${fromAccountOptions}
                </select>
            </div>
            
            <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid #e0e4e8;">
                <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                    <i class="fas fa-university" style="color: #1976d2; margin-right: 6px;"></i>Alıcı IBAN
                </label>
                <div style="position: relative; display: flex; align-items: center;">
                    <div style="background: #1976d2; color: white; padding: 14px 16px; border-radius: 10px 0 0 10px; font-weight: 700; font-size: 1rem; letter-spacing: 1px; border: 2px solid #1976d2; border-right: none;">TR</div>
                    <input type="text" id="tr-external-iban" placeholder="00 0000 0000 0000 0000 0000 00" maxlength="27" style="flex: 1; padding: 14px 16px; padding-left: 12px; border: 2px solid #ddd; border-left: none; border-radius: 0 10px 10px 0; font-size: 1rem; font-weight: 600; letter-spacing: 2px; font-family: 'Courier New', monospace; transition: all 0.3s;" autocomplete="off" onfocus="this.style.borderColor='#1976d2'; this.style.boxShadow='0 0 0 3px rgba(25, 118, 210, 0.1)'; this.previousElementSibling.style.borderColor='#1976d2'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'; this.previousElementSibling.style.borderColor='#1976d2'">
                </div>
                <div id="tr-iban-error" style="color: #d32f2f; font-size: 0.85rem; margin-top: 8px; min-height: 20px; display: flex; align-items: center; gap: 6px;">
                    <i class="fas fa-info-circle" style="font-size: 0.75rem;"></i>
                    <span></span>
                </div>
                <div style="margin-top: 8px; font-size: 0.8rem; color: #666; display: flex; align-items: center; gap: 6px;">
                    <i class="fas fa-check-circle" style="color: #4caf50; font-size: 0.75rem;"></i>
                    <span>IBAN otomatik olarak doğrulanacaktır</span>
                </div>
            </div>
            
            <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid #e0e4e8;">
                <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                    <i class="fas fa-user" style="color: #1976d2; margin-right: 6px;"></i>Alıcı Ad Soyad
                </label>
                <input type="text" id="tr-external-name" placeholder="Alıcının tam adı ve soyadı" maxlength="100" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; transition: all 0.3s; text-transform: capitalize;" autocomplete="off" onfocus="this.style.borderColor='#1976d2'; this.style.boxShadow='0 0 0 3px rgba(25, 118, 210, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                <div id="tr-name-error" style="color: #d32f2f; font-size: 0.85rem; margin-top: 8px; min-height: 20px; display: flex; align-items: center; gap: 6px;">
                    <i class="fas fa-info-circle" style="font-size: 0.75rem;"></i>
                    <span></span>
                </div>
            </div>
            
            <div style="background: #e3f2fd; border-left: 4px solid #1976d2; border-radius: 8px; padding: 16px; margin-bottom: 24px;">
                <div style="display: flex; align-items: start; gap: 12px;">
                    <i class="fas fa-info-circle" style="color: #1976d2; font-size: 20px; margin-top: 2px;"></i>
                    <div style="flex: 1;">
                        <div style="font-weight: 600; color: #1976d2; margin-bottom: 4px; font-size: 0.9rem;">Bilgi</div>
                        <div style="font-size: 0.85rem; color: #1976d2; line-height: 1.5;">IBAN ve alıcı bilgilerini doğru girdiğinizden emin olun. Bu işlem geri alınamaz.</div>
                    </div>
                </div>
            </div>
            
            <div style="display:flex; gap:12px; justify-content:flex-end; padding-top: 20px; border-top: 1px solid #e0e4e8;">
                <button class="btn" id="tr-external-cancel" style="padding: 14px 28px; font-weight: 600; border: 2px solid #ddd; background: white; color: #666;">İptal</button>
                <button class="btn" id="tr-external-continue" style="padding: 14px 28px; font-weight: 600; background: linear-gradient(135deg, #d32f2f 0%, #f44336 100%); color: white; opacity: 0.5; cursor: not-allowed; border: none; box-shadow: 0 4px 12px rgba(211, 47, 47, 0.2); transition: all 0.3s;" disabled>
                    <i class="fas fa-arrow-right" style="margin-right: 8px;"></i>Devam
                </button>
            </div>
        `;
        modal.appendChild(box);
        document.body.appendChild(modal);

        const ibanInput = document.getElementById('tr-external-iban');
        const nameInput = document.getElementById('tr-external-name');
        const ibanError = document.getElementById('tr-iban-error');
        const nameError = document.getElementById('tr-name-error');
        const continueBtn = document.getElementById('tr-external-continue');

        // IBAN formatlama (TR sabit, sadece rakamlar ve boşluklar)
        ibanInput.addEventListener('input', (e) => {
            // Sadece rakam ve boşluk kabul et
            let value = e.target.value.replace(/[^0-9\s]/g, '');
            // Maksimum 24 rakam (TR hariç)
            value = value.replace(/\s/g, '');
            if (value.length > 24) value = value.substring(0, 24);
            // 4'erli gruplara böl
            if (value.length > 0) {
                value = value.match(/.{1,4}/g)?.join(' ') || value;
            }
            e.target.value = value;
            validateInputs();
        });
        
        // IBAN input'una odaklanınca TR'yi vurgula
        ibanInput.addEventListener('focus', () => {
            const trBadge = ibanInput.previousElementSibling;
            if (trBadge) {
                trBadge.style.background = '#1565c0';
                trBadge.style.transform = 'scale(1.05)';
            }
        });
        
        ibanInput.addEventListener('blur', () => {
            const trBadge = ibanInput.previousElementSibling;
            if (trBadge) {
                trBadge.style.background = '#1976d2';
                trBadge.style.transform = 'scale(1)';
            }
        });

        // Ad Soyad validasyonu
        nameInput.addEventListener('input', () => {
            validateInputs();
        });

        function validateInputs() {
            const iban = ibanInput.value.trim();
            const name = nameInput.value.trim();
            
            // IBAN validasyonu (TR + kullanıcı girdisi)
            const fullIban = 'TR' + iban.replace(/\s/g, '');
            const ibanValidation = validateIBAN(fullIban);
            if (iban && !ibanValidation.valid) {
                const errorSpan = ibanError.querySelector('span');
                if (errorSpan) errorSpan.textContent = ibanValidation.message;
                ibanError.style.display = 'flex';
                ibanError.style.color = '#d32f2f';
            } else if (iban && ibanValidation.valid) {
                const errorSpan = ibanError.querySelector('span');
                if (errorSpan) errorSpan.textContent = '';
                ibanError.style.display = 'none';
            } else {
                const errorSpan = ibanError.querySelector('span');
                if (errorSpan) errorSpan.textContent = '';
                ibanError.style.display = 'none';
            }
            
            // Ad Soyad validasyonu
            const nameValidation = validateName(name);
            if (name && !nameValidation.valid) {
                const errorSpan = nameError.querySelector('span');
                if (errorSpan) errorSpan.textContent = nameValidation.message;
                nameError.style.display = 'flex';
                nameError.style.color = '#d32f2f';
            } else {
                const errorSpan = nameError.querySelector('span');
                if (errorSpan) errorSpan.textContent = '';
                nameError.style.display = 'none';
            }
            
            // Devam butonunu aktif/pasif yap
            const fullIbanForValidation = 'TR' + iban.replace(/\s/g, '');
            const ibanValidationForBtn = validateIBAN(fullIbanForValidation);
            if (ibanValidationForBtn.valid && nameValidation.valid) {
                continueBtn.disabled = false;
                continueBtn.style.opacity = '1';
                continueBtn.style.cursor = 'pointer';
                continueBtn.style.boxShadow = '0 4px 12px rgba(211, 47, 47, 0.4)';
            } else {
                continueBtn.disabled = true;
                continueBtn.style.opacity = '0.5';
                continueBtn.style.cursor = 'not-allowed';
                continueBtn.style.boxShadow = '0 4px 12px rgba(211, 47, 47, 0.2)';
            }
        }

        document.getElementById('tr-external-cancel').addEventListener('click', () => modal.remove());
        
        continueBtn.addEventListener('click', () => {
            const iban = ibanInput.value.trim();
            const name = nameInput.value.trim();
            const fullIban = 'TR' + iban.replace(/\s/g, '');
            const ibanValidation = validateIBAN(fullIban);
            const nameValidation = validateName(name);
            
            if (!ibanValidation.valid || !nameValidation.valid) {
                showToast('Lütfen tüm alanları doğru şekilde doldurun.', 'error');
                return;
            }
            
        // Aşama 2'ye geç - fromAccountId'yi de geçir
        const fromAccountId = document.getElementById('tr-external-from').value;
        showExternalTransferStep2(modal, ibanValidation.cleanIban, name, fromAccountId);
    });
    } catch (error) {
        console.error('showExternalAccountTransfer error:', error);
        showToast('Bir hata oluştu: ' + error.message, 'error');
    }
}

// Başka Hesaba Transfer - Aşama 2: Tutar, Açıklama ve Onay
function showExternalTransferStep2(modal, iban, receiverName, fromAccountId) {
    const box = modal.querySelector('div');
    const formatBalance = (bal) => {
        if (!bal && bal !== 0) return '0,00';
        return parseFloat(bal).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };
    const fromAccount = window.currentAccounts.find(acc => acc.id === fromAccountId);
    
    box.innerHTML = `
        <div style="text-align: center; margin-bottom: 24px;">
            <div style="width: 60px; height: 60px; background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px;">
                <i class="fas fa-check-circle" style="color: white; font-size: 24px;"></i>
            </div>
            <h3 style="margin: 0 0 8px 0; color: #1a1a1a; font-size: 1.5rem; font-weight: 700;">Transfer Bilgileri</h3>
            <p style="margin: 0; color: #666; font-size: 0.9rem;">Bilgileri kontrol edin ve tutarı girin</p>
        </div>
        
        <div style="background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%); padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 1px solid #e0e4e8;">
            <div style="margin-bottom: 16px; padding-bottom: 16px; border-bottom: 2px solid #e0e4e8;">
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                    <i class="fas fa-wallet" style="color: #d32f2f; font-size: 18px;"></i>
                    <div style="font-size: 0.85rem; color: #666; font-weight: 600;">Gönderen Hesap</div>
                </div>
                <div style="font-weight: 700; font-size: 1.1rem; color: #1a1a1a; margin-bottom: 4px;">${fromAccount ? (fromAccount.accountNumber || fromAccount.id) : fromAccountId}</div>
                <div style="font-size: 0.95rem; color: #4caf50; font-weight: 600;">
                    <i class="fas fa-lira-sign" style="font-size: 0.85rem;"></i> ${formatBalance(fromAccount?.balance || 0)} TL
                </div>
            </div>
            <div style="margin-bottom: 16px; padding-bottom: 16px; border-bottom: 2px solid #e0e4e8;">
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                    <i class="fas fa-university" style="color: #1976d2; font-size: 18px;"></i>
                    <div style="font-size: 0.85rem; color: #666; font-weight: 600;">Alıcı IBAN</div>
                </div>
                <div style="font-weight: 700; font-size: 1.1rem; color: #1a1a1a; font-family: 'Courier New', monospace; letter-spacing: 1px;">${formatIBAN(iban)}</div>
            </div>
            <div>
                <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                    <i class="fas fa-user" style="color: #1976d2; font-size: 18px;"></i>
                    <div style="font-size: 0.85rem; color: #666; font-weight: 600;">Alıcı Ad Soyad</div>
                </div>
                <div style="font-weight: 700; font-size: 1.1rem; color: #1a1a1a;">${receiverName}</div>
            </div>
        </div>
        
        <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-lira-sign" style="color: #d32f2f; margin-right: 6px;"></i>Tutar (TL)
            </label>
            <div style="position: relative;">
                <input type="number" id="tr-external-amount" placeholder="0,00" step="0.01" min="0.01" style="width: 100%; padding: 14px 16px; padding-left: 48px; border: 2px solid #ddd; border-radius: 10px; font-size: 1.2rem; font-weight: 600; transition: all 0.3s;" onfocus="this.style.borderColor='#d32f2f'; this.style.boxShadow='0 0 0 3px rgba(211, 47, 47, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
                <span style="position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: #999; font-weight: 600; font-size: 1.2rem;">₺</span>
            </div>
        </div>
        
        <div style="background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 2px solid #e0e4e8;">
            <label style="display: block; margin-bottom: 10px; font-weight: 600; color: #333; font-size: 0.9rem;">
                <i class="fas fa-comment-alt" style="color: #666; margin-right: 6px;"></i>Açıklama <span style="color: #999; font-weight: normal; font-size: 0.85rem;">(opsiyonel)</span>
            </label>
            <input type="text" id="tr-external-desc" placeholder="Transfer açıklaması..." maxlength="500" style="width: 100%; padding: 14px 16px; border: 2px solid #ddd; border-radius: 10px; font-size: 1rem; transition: all 0.3s;" onfocus="this.style.borderColor='#d32f2f'; this.style.boxShadow='0 0 0 3px rgba(211, 47, 47, 0.1)'" onblur="this.style.borderColor='#ddd'; this.style.boxShadow='none'">
        </div>
        
        <div style="background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 8px; padding: 16px; margin-bottom: 24px;">
            <div style="display: flex; align-items: start; gap: 12px;">
                <i class="fas fa-exclamation-triangle" style="color: #856404; font-size: 20px; margin-top: 2px;"></i>
                <div style="flex: 1;">
                    <div style="font-weight: 600; color: #856404; margin-bottom: 4px; font-size: 0.9rem;">Önemli Uyarı</div>
                    <div style="font-size: 0.85rem; color: #856404; line-height: 1.5;">Bu transfer işlemi geri alınamaz. Lütfen tüm bilgileri dikkatlice kontrol edin.</div>
                </div>
            </div>
        </div>
        
        <div style="display:flex; gap:12px; justify-content:flex-end; padding-top: 20px; border-top: 1px solid #e0e4e8;">
            <button class="btn" id="tr-external-back" style="padding: 14px 28px; font-weight: 600; border: 2px solid #ddd; background: white; color: #666;">
                <i class="fas fa-arrow-left" style="margin-right: 8px;"></i>Geri
            </button>
            <button class="btn primary" id="tr-external-submit" style="padding: 14px 28px; font-weight: 600; background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%); border: none; box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);">
                <i class="fas fa-check-circle" style="margin-right: 8px;"></i>Onayla ve Gönder
            </button>
        </div>
    `;
    
    // IBAN, isim ve fromAccountId'yi sakla (data attribute olarak)
    box.setAttribute('data-iban', iban);
    box.setAttribute('data-receiver-name', receiverName);
    box.setAttribute('data-from-account-id', fromAccountId);
    
    document.getElementById('tr-external-back').addEventListener('click', () => {
        showExternalAccountTransfer();
        modal.remove();
    });
    
    document.getElementById('tr-external-submit').addEventListener('click', async () => {
        const submitBtn = document.getElementById('tr-external-submit');
        const backBtn = document.getElementById('tr-external-back');
        const amountInput = document.getElementById('tr-external-amount');
        const descInput = document.getElementById('tr-external-desc');
        
        // Element kontrolü
        if (!submitBtn || !backBtn || !amountInput || !descInput) {
            console.error('Modal elements not found');
            showToast('Form elemanları bulunamadı. Lütfen sayfayı yenileyin.', 'error');
            return;
        }
        
        // Butonları disable et (çift tıklamayı önle)
        submitBtn.disabled = true;
        backBtn.disabled = true;
        const originalBtnText = submitBtn.innerText;
        submitBtn.innerText = 'Gönderiliyor...';
        submitBtn.style.opacity = '0.6';
        submitBtn.style.cursor = 'not-allowed';
        
        try {
            const amount = parseFloat((amountInput.value || '').replace(',', '.'));
            const desc = (descInput.value || '').trim();
            // fromAccountId parametre olarak geldi, data attribute'dan al
            const fromAccountIdToUse = box.getAttribute('data-from-account-id') || fromAccountId;
            
            if (!fromAccountIdToUse) {
                showToast('Gönderen hesap bilgisi bulunamadı. Lütfen tekrar deneyin.', 'error');
                submitBtn.disabled = false;
                backBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
                submitBtn.style.opacity = '1';
                submitBtn.style.cursor = 'pointer';
                return;
            }
            
            if (!amount || isNaN(amount) || amount <= 0) {
                showToast('Lütfen geçerli bir tutar girin.', 'error');
                submitBtn.disabled = false;
                backBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
                submitBtn.style.opacity = '1';
                submitBtn.style.cursor = 'pointer';
                return;
            }
            
            // Bakiye kontrolü
            const fromAccount = window.currentAccounts.find(acc => acc.id === fromAccountIdToUse);
            if (fromAccount && (fromAccount.balance || 0) < amount) {
                showToast('Yetersiz bakiye. Mevcut bakiye: ' + formatBalance(fromAccount.balance || 0) + ' TL', 'error');
                submitBtn.disabled = false;
                backBtn.disabled = false;
                submitBtn.innerText = originalBtnText;
                submitBtn.style.opacity = '1';
                submitBtn.style.cursor = 'pointer';
                return;
            }
            
            // Gateway'e istek gönder
            const idempotencyKey = 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2,8);
            const payload = { 
                fromAccountId: fromAccountIdToUse, 
                toAccountId: iban, // IBAN'ı direkt gönder
                amount: amount, 
                currency: 'TRY', 
                description: desc || `Transfer: ${receiverName}`, 
                idempotencyKey 
            };
            
            console.log('Sending transfer request to Gateway:', payload);
            const res = await createTransfer(payload);
            console.log('Transfer response from Gateway:', res);
            
            // Response kontrolü
            if (res && (res.id || res.transactionId || res.transaction_id)) {
                const txId = res.id || res.transactionId || res.transaction_id;
                const status = res.status || res.transactionStatus || 'PENDING';
                
                // Başarılı mesajı göster
                showToast(`Transfer başarıyla oluşturuldu! İşlem ID: ${txId}`, 'success');
                
                // Modal'ı kapat
                modal.remove();
                
                // Transaction status'ü poll et (PENDING ise)
                if (status === 'PENDING' || status === 'PROCESSING') {
                    pollTransactionStatus(txId);
                } else if (status === 'COMPLETED') {
                    showToast('Transfer başarıyla tamamlandı!', 'success');
                } else if (status === 'FAILED') {
                    const failureReason = res.failureReason || res.failureDetails || 'Bilinmeyen neden';
                    showToast(`Transfer başarısız: ${failureReason}`, 'error');
                }
                
                // Update balances in state immediately (optimistic update)
                updateBalancesAfterTransfer(fromAccountIdToUse, iban, amount);
                
                // Hesapları ve işlemleri yenile (sadece transactions, accounts state'ten güncellendi)
                setTimeout(() => { 
                    loadTransactionsForCurrentUser(); 
                }, 1500);
            } else {
                // Response'da ID yoksa ama hata da yoksa
                showToast('Transfer talebi gönderildi, ancak işlem ID alınamadı.', 'info');
                modal.remove();
                // Update balances in state immediately (optimistic update)
                updateBalancesAfterTransfer(fromAccountIdToUse, iban, amount);
                
                setTimeout(() => { 
                    loadTransactionsForCurrentUser(); 
                }, 1500);
            }
        } catch (e) {
            console.error('Transfer error:', e);
            
            // Hata mesajını parse et
            let errorMessage = 'Transfer işlemi başarısız';
            if (e.message) {
                errorMessage = e.message;
            } else if (e.response) {
                // API response hatası
                try {
                    const errorData = typeof e.response === 'string' ? JSON.parse(e.response) : e.response;
                    errorMessage = errorData.message || errorData.error || errorData.details || errorMessage;
                } catch (parseErr) {
                    errorMessage = e.response || errorMessage;
                }
            }
            
            // HTTP status koduna göre özel mesajlar
            if (e.status === 400) {
                errorMessage = 'Geçersiz istek: ' + (errorMessage || 'Lütfen bilgileri kontrol edin');
            } else if (e.status === 401) {
                errorMessage = 'Oturum süresi dolmuş. Lütfen tekrar giriş yapın.';
            } else if (e.status === 403) {
                errorMessage = 'Bu işlem için yetkiniz bulunmamaktadır.';
            } else if (e.status === 404) {
                errorMessage = 'Hesap bulunamadı. Lütfen hesap bilgilerini kontrol edin.';
            } else if (e.status === 409) {
                errorMessage = 'Bu işlem daha önce yapılmış (duplicate request).';
            } else if (e.status === 429) {
                errorMessage = 'Çok fazla istek gönderildi. Lütfen bir süre sonra tekrar deneyin.';
            } else if (e.status === 500) {
                errorMessage = 'Sunucu hatası. Lütfen daha sonra tekrar deneyin.';
            } else if (e.status >= 500) {
                errorMessage = 'Sunucu hatası (' + e.status + '). Lütfen daha sonra tekrar deneyin.';
            }
            
            showToast(errorMessage, 'error');
            
            // Butonları tekrar aktif et
            submitBtn.disabled = false;
            backBtn.disabled = false;
            submitBtn.innerText = originalBtnText;
            submitBtn.style.opacity = '1';
            submitBtn.style.cursor = 'pointer';
        }
    });
}

// Minimal transfer modal to create a transaction (legacy - kept for backward compatibility)
function showTransferModal() {
    showExternalAccountTransfer();
}

// Poll transaction status until it's not PENDING (or until attempts exhausted)
async function pollTransactionStatus(txnId, attempts = 10, interval = 1500) {
    let a = 0;
    while (a < attempts) {
        try {
            const t = await getTransaction(txnId);
            const status = t && (t.status || t.transactionStatus || t.state);
            if (status && status !== 'PENDING' && status !== 'PROCESSING') {
                showToast('İşlem durumu: ' + status, status === 'COMPLETED' ? 'success' : 'error');
                return t;
            }
        } catch (e) {
            console.debug('poll status error', e);
        }
        await sleep(interval);
        a += 1;
    }
}

// simple HTML escape
function escapeHtml(s) {
    if (!s && s !== 0) return '';
    return String(s).replace(/[&<>\"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; });
}

// --- end transactions ---

// Transfer removed (no backend endpoint).

// Payments removed (no backend endpoint).

function copyIBANMobile() {
    const text = document.getElementById('ibanTextMobile').innerText;
    navigator.clipboard.writeText(text);
    showToast('IBAN Kopyalandı', 'success');
}

function toggleMenu() {
    // Bu fonksiyon artık kullanılmıyor
}

function logout() {
    // perform logout via API (no confirm). Clear local session and return to home.
    (async () => {
        try {
            // Logout endpoint'ini çağır - credentials: 'include' ile cookie'leri temizle
            await fetch(`${API_URL}/auth/logout`, { 
                method: 'POST', 
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
        } catch (err) {
            // ignore errors from logout endpoint - yine de local state'i temizle
            console.warn('Logout endpoint error (ignored):', err);
        }
        // Local state'i temizle
        localStorage.removeItem('triobank_token');
        currentUser = null;
        showToast(currentLang === 'tr' ? 'Çıkış gerçekleştirildi.' : 'Logged out.', 'success');
        showPage('home');
    })();
}

// callStaff removed (no backend endpoint)
// callStaff removed (no backend endpoint)
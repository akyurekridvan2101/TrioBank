// ========================================
// UI Utility Functions
// ========================================

import { currentLang } from '../core/state.js';

/**
 * Toast Notification Helper
 */
export function showToast(message, type = 'info') {
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

/**
 * Global Loader (shows while API requests are in-flight)
 */
export function showLoader() {
    let el = document.getElementById('global-loader');
    if (!el) {
        el = document.createElement('div');
        el.id = 'global-loader';
        el.innerHTML = '<div class="loader-spinner"></div>';
        document.body.appendChild(el);
    }
    el.style.display = 'flex';
}

export function hideLoader() {
    const el = document.getElementById('global-loader');
    if (el) el.style.display = 'none';
}

/**
 * Show confirmation modal (Evet/Hayır)
 */
export function showConfirmModal(title, message, onConfirm, onCancel = null) {
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

// Expose to window for inline onclick handlers
if (typeof window !== 'undefined') {
    window.showToast = showToast;
    window.showConfirmModal = showConfirmModal;
}

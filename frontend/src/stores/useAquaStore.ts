import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User, UserRole } from '../types';

/**
 * Aqua Store (Simplified)
 * Handles global/cross-cutting concerns like Auth and Notifications.
 * Feature-specific state is delegated to feature stores.
 */
export const useAquaStore = defineStore('aqua', () => {
    // --- Auth State ---
    const currentUser = ref<User>({ id: 'u1', name: 'Admin User', role: 'Admin' });

    // --- Global Notifications ---
    type NotificationType = 'success' | 'businessError' | 'systemError';

    const globalMessage = ref<string | null>(null);
    const globalNotificationType = ref<NotificationType>('success');
    const isGlobalNotificationVisible = ref(false);
    const notificationRetryCallback = ref<(() => void) | null>(null);

    const notificationConfig = {
        success: {
            color: 'success',
            timeout: 3000,
            icon: 'mdi-check-circle'
        },
        businessError: {
            color: 'error',
            timeout: 6000,
            icon: 'mdi-alert-circle'
        },
        systemError: {
            color: 'error',
            timeout: 0, // No auto-dismiss for system errors
            icon: 'mdi-alert-octagon'
        }
    };

    function notify(message: string, type: NotificationType = 'success', retryCallback?: () => void) {
        globalMessage.value = message;
        globalNotificationType.value = type;
        isGlobalNotificationVisible.value = true;
        notificationRetryCallback.value = retryCallback || null;

        // Auto-dismiss for success and business errors
        const config = notificationConfig[type];
        if (config.timeout > 0) {
            setTimeout(() => {
                clearNotification();
            }, config.timeout);
        }
    }

    function clearNotification() {
        isGlobalNotificationVisible.value = false;
        notificationRetryCallback.value = null;
        setTimeout(() => {
            globalMessage.value = null;
        }, 300);
    }

    function retryAction() {
        if (notificationRetryCallback.value) {
            notificationRetryCallback.value();
            clearNotification();
        }
    }

    // --- Auth Getters ---
    const isAdmin = computed(() => currentUser.value.role === 'Admin');
    const isManager = computed(() => currentUser.value.role === 'Manager' || currentUser.value.role === 'Admin');
    const isOperator = computed(() => currentUser.value.role === 'Operator' || currentUser.value.role === 'Manager' || currentUser.value.role === 'Admin');

    function setUserRole(role: UserRole) {
        currentUser.value.role = role;
    }

    return {
        // Auth
        currentUser,
        isAdmin,
        isManager,
        isOperator,
        setUserRole,

        // Notifications
        globalMessage,
        globalNotificationType,
        isGlobalNotificationVisible,
        notificationRetryCallback,
        notificationConfig,
        notify,
        clearNotification,
        retryAction,
    };
});

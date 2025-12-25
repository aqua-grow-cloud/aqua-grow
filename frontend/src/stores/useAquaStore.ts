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
    const globalError = ref<string | null>(null);
    const globalErrorType = ref<string>('error'); // error, warning, info
    const isGlobalErrorVisible = ref(false);

    function notify(message: string, type: 'error' | 'warning' | 'info' = 'error') {
        globalError.value = message;
        globalErrorType.value = type;
        isGlobalErrorVisible.value = true;
    }

    function clearNotification() {
        isGlobalErrorVisible.value = false;
        setTimeout(() => {
            globalError.value = null;
        }, 300);
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
        globalError,
        globalErrorType,
        isGlobalErrorVisible,
        notify,
        clearNotification
    };
});

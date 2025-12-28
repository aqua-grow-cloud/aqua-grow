import { ref, type Ref } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import type { AxiosError } from 'axios';

interface ErrorResponse {
    message: string;
    type: 'VALIDATION_ERROR' | 'BUSINESS_ERROR' | 'SYSTEM_ERROR';
    fieldErrors?: Record<string, string>;
}

interface ApiActionOptions {
    successMessage?: string;
    showSuccessNotification?: boolean;
}

interface ApiActionResult<T> {
    data: Ref<T | null>;
    error: Ref<AxiosError<ErrorResponse> | null>;
    fieldErrors: Ref<Record<string, string>>;
    execute: () => Promise<void>;
    isLoading: Ref<boolean>;
}

/**
 * Composable wrapper for API actions with automatic error handling
 * 
 * @param action - The async function to execute
 * @param options - Configuration options
 * @returns Object with data, error, fieldErrors, execute function, and loading state
 */
export function useApiAction<T>(
    action: () => Promise<T>,
    options: ApiActionOptions = {}
): ApiActionResult<T> {
    const store = useAquaStore();
    const data = ref<T | null>(null);
    const error = ref<AxiosError<ErrorResponse> | null>(null);
    const fieldErrors = ref<Record<string, string>>({});
    const isLoading = ref(false);

    const {
        successMessage = 'Operation completed successfully',
        showSuccessNotification = true
    } = options;

    async function execute() {
        isLoading.value = true;
        error.value = null;
        fieldErrors.value = {};

        try {
            const result = await action();
            data.value = result;

            // Show success notification
            if (showSuccessNotification) {
                store.notify(successMessage, 'success');
            }
        } catch (err: any) {
            error.value = err;
            const response = err.response?.data as ErrorResponse;

            if (response) {
                const { type, message, fieldErrors: errors } = response;

                // Handle field-level validation errors
                if (errors) {
                    fieldErrors.value = errors;
                }

                // Determine notification type based on error type and status code
                if (type === 'VALIDATION_ERROR' || type === 'BUSINESS_ERROR') {
                    store.notify(message || 'Validation failed', 'businessError');
                } else {
                    // System error - provide retry callback
                    store.notify(
                        message || 'An unexpected error occurred. Please try again.',
                        'systemError',
                        execute // Retry callback
                    );
                }
            } else {
                // No structured error response - treat as system error
                store.notify(
                    'An unexpected error occurred. Please try again.',
                    'systemError',
                    execute
                );
            }
        } finally {
            isLoading.value = false;
        }
    }

    return {
        data: data as Ref<T | null>,
        error,
        fieldErrors,
        execute,
        isLoading
    };
}

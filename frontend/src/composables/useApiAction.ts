import {ref, type Ref} from 'vue';
import {useAquaStore} from '../stores/useAquaStore';
import type {AxiosError} from 'axios';

interface ErrorResponse {
    message: string;
    type: 'VALIDATION_ERROR' | 'BUSINESS_ERROR' | 'SYSTEM_ERROR';
    fieldErrors?: Record<string, string>;
}

interface ApiActionOptions {
    successMessage?: string;
    showSuccessNotification?: boolean;
}

interface ApiActionResult<T, Args extends unknown[]> {
    data: Ref<T | null>;
    error: Ref<AxiosError<ErrorResponse> | null>;
    fieldErrors: Ref<Record<string, string>>;
    execute: (...args: Args) => Promise<void>;
    isLoading: Ref<boolean>;
}

/**
 * Composable wrapper for API actions with automatic error handling.
 * Supports optional arguments forwarded to the action function.
 *
 * @param action - The async function to execute (may accept arguments)
 * @param options - Configuration options
 * @returns Object with data, error, fieldErrors, execute function, and loading state
 */
export function useApiAction<T, Args extends unknown[] = []>(
    action: (...args: Args) => Promise<T>,
    options: ApiActionOptions = {}
): ApiActionResult<T, Args> {
    const store = useAquaStore();
    const data = ref<T | null>(null);
    const error = ref<AxiosError<ErrorResponse> | null>(null);
    const fieldErrors = ref<Record<string, string>>({});
    const isLoading = ref(false);

    const {
        successMessage = 'Operation completed successfully',
        showSuccessNotification = true
    } = options;

    // Store last args so the retry callback can replay the same call
    let lastArgs: Args = [] as unknown as Args;

    async function execute(...args: Args) {
        lastArgs = args;
        isLoading.value = true;
        error.value = null;
        fieldErrors.value = {};

        try {
            data.value = await action(...args);

            if (showSuccessNotification) {
                store.notify(successMessage, 'success');
            }
        } catch (err: any) {
            error.value = err;
            const response = err.response?.data as ErrorResponse;

            if (response) {
                const { type, message, fieldErrors: errors } = response;

                if (errors) {
                    fieldErrors.value = errors;
                }

                if (type === 'VALIDATION_ERROR' || type === 'BUSINESS_ERROR') {
                    store.notify(message || 'Validation failed', 'businessError');
                } else {
                    store.notify(
                        message || 'An unexpected error occurred. Please try again.',
                        'systemError',
                        () => execute(...lastArgs)
                    );
                }
            } else {
                store.notify(
                    'An unexpected error occurred. Please try again.',
                    'systemError',
                    () => execute(...lastArgs)
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

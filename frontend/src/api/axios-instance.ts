import axios, { type AxiosRequestConfig } from 'axios';
import { keycloak } from '../plugins/keycloak';

export const AXIOS_INSTANCE = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'https://api.aqua-grow.cloud',
});

AXIOS_INSTANCE.interceptors.request.use(async (config) => {
    if (keycloak.authenticated) {
        try {
            await keycloak.updateToken(70); // Refresh if expiring in < 70 seconds
        } catch (error) {
            console.error('Failed to refresh token', error);
            keycloak.login();
        }

        if (keycloak.token) {
            config.headers.Authorization = `Bearer ${keycloak.token}`;
        }
    }
    return config;
});

AXIOS_INSTANCE.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // check if 401 and not already retried
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                // Try to force refresh the token
                await keycloak.updateToken(-1);

                if (keycloak.token) {
                    originalRequest.headers.Authorization = `Bearer ${keycloak.token}`;
                    return AXIOS_INSTANCE(originalRequest);
                }
            } catch (refreshError) {
                console.error('Token refresh failed on 401', refreshError);
                keycloak.login();
                return Promise.reject(refreshError);
            }
        }

        // Dynamic import to avoid circular dependency if store imports api
        import('../stores/useAquaStore').then(({ useAquaStore }) => {
            const store = useAquaStore();
            const message = error.response?.data?.message || error.message || 'An unknown error occurred';

            // Don't show notification for 401s if we are handling them (or if login redirect happens)
            if (error.response?.status !== 401) {
                store.notify(message, 'error');
            }
        });
        return Promise.reject(error);
    }
);

export const customInstance = <T>(config: AxiosRequestConfig): Promise<T> => {
    const source = axios.CancelToken.source();

    // Keycloak token injection is handled by the interceptor now

    const promise = AXIOS_INSTANCE({ ...config, cancelToken: source.token }).then(
        ({ data }) => data,
    );

    // @ts-ignore
    promise.cancel = () => {
        source.cancel('Query was cancelled by Orval');
    };

    return promise;
};

export default customInstance;

import axios, { type AxiosRequestConfig } from 'axios';
import { keycloak } from '../plugins/keycloak';

export const AXIOS_INSTANCE = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'https://api.aqua-grow.cloud',
});

AXIOS_INSTANCE.interceptors.request.use((config) => {
    if (keycloak.token) {
        config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    return config;
});

AXIOS_INSTANCE.interceptors.response.use(
    (response) => response,
    (error) => {
        // Dynamic import to avoid circular dependency if store imports api
        import('../stores/useAquaStore').then(({ useAquaStore }) => {
            const store = useAquaStore();
            const message = error.response?.data?.message || error.message || 'An unknown error occurred';
            store.notify(message, 'error');
        });
        return Promise.reject(error);
    }
);

export const customInstance = <T>(config: AxiosRequestConfig): Promise<T> => {
    const source = axios.CancelToken.source();

    // Inject Keycloak token if authenticated
    if (keycloak.token) {
        config.headers = {
            ...config.headers,
            Authorization: `Bearer ${keycloak.token}`,
        };
    }

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

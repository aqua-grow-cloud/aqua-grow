import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL || 'https://sso.aqua-grow.cloud/',
    realm: 'aqua-grow',
    clientId: 'aqua-grow-frontend'
})

export default {
    install: async (app: any) => {
        try {
            await keycloak.init({
                onLoad: 'login-required',
                checkLoginIframe: false
            })
            app.config.globalProperties.$keycloak = keycloak
            app.provide('keycloak', keycloak)
        } catch (error) {
            console.error('Keycloak init failed', error)
        }
    },
    keycloak
}

export { keycloak }

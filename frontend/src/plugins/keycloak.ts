import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080/',
    realm: 'aqua-grow',
    clientId: 'aqua-grow-web'
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

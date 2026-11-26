import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import keycloakPlugin from './plugins/keycloak'
import './style.css'

const queryClient = new QueryClient()

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(vuetify)
app.use(VueQueryPlugin, { queryClient })

// Wait for Keycloak before mounting
keycloakPlugin.install(app).then(() => {
    app.mount('#app')
}).catch(() => {
    console.error("Failed to install Keycloak plugin")
})

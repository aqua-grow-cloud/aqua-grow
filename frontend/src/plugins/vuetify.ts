// src/plugins/vuetify.ts
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import '@mdi/font/css/materialdesignicons.css'

export default createVuetify({
    components,
    directives,
    icons: {
        defaultSet: 'mdi',
        aliases,
        sets: {
            mdi,
        },
    },
    theme: {
        defaultTheme: 'aquaTheme',
        themes: {
            aquaTheme: {
                dark: false,
                colors: {
                    background: '#F0F4F8',
                    surface: '#FFFFFF',
                    primary: '#00695C', // Deep Teal
                    'primary-darken-1': '#004D40',
                    secondary: '#0097A7', // Cyan
                    text: '#263238',
                    error: '#D32F2F',
                    info: '#0288D1',
                    success: '#388E3C',
                    warning: '#FBC02D',
                },
            },
        },
    },
})

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
        defaultTheme: 'dark',
        themes: {
            dark: {
                colors: {
                    primary: '#00bcd4', // Azure Blue / Cyan
                    secondary: '#4caf50', // Green
                    accent: '#009688', // Teal
                    error: '#f44336',
                    info: '#2196f3',
                    success: '#4caf50',
                    warning: '#ffc107',
                },
            },
        },
    },
})

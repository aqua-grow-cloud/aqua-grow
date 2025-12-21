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
        defaultTheme: 'professionalTheme',
        themes: {
            professionalTheme: {
                dark: false,
                colors: {
                    background: '#F1F5F9', // Slate 100
                    surface: '#FFFFFF',
                    primary: '#0891B2', // Cyan 600 (Pro Teal/Blue)
                    'primary-darken-1': '#0E7490',
                    secondary: '#334155', // Slate 700
                    text: '#0F172A', // Slate 900
                    error: '#EF4444',
                    info: '#3B82F6',
                    success: '#10B981',
                    warning: '#F59E0B',
                },
            },
        },
    },
})

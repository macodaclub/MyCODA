import 'primeicons/primeicons.css'
import './assets/main.css'

import {createApp} from 'vue'
import {createPinia} from 'pinia'
import PrimeVue from 'primevue/config';
import theme from '@/theme';
import App from './App.vue'
import router from "@/router";


createApp(App).use(createPinia()).use(router).use(PrimeVue, {
    theme: {
        preset: theme,
        options: {
            cssLayer: {
                name: 'primevue',
                order: 'tailwind-base, primevue, tailwind-utilities'
            },
            darkModeSelector: '.my-app-dark'
        }
    }
}).mount('#app')

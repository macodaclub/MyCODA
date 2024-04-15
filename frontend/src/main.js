import 'primevue/resources/themes/aura-light-amber/theme.css'
import 'primeicons/primeicons.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config';
import App from './App.vue'
import router from "@/router";


createApp(App).use(PrimeVue).use(router).use(createPinia()).mount('#app')

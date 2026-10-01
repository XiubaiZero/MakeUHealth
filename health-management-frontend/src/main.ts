import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/css/health-styles.css'
import './i18n'

const app = createApp(App)

app.use(router)

app.mount('#app')

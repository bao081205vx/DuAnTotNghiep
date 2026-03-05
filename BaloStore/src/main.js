// Global styles
import './assets/main.css'
// Tailwind is already configured in the project via PostCSS

// Add Bootstrap and Font Awesome global CSS
import 'bootstrap/dist/css/bootstrap.min.css'
import '@fortawesome/fontawesome-free/css/all.min.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Optionally load Chart.js auto-registration (components can also import it)
import 'chart.js/auto'

// Monkey-patch fetch to automatically add Authorization header when calling /api/* endpoints
try{
	const _fetch = window.fetch.bind(window)
	window.fetch = function(input, init){
		try{
			const tokenRaw = localStorage.getItem('bagistore_current_user')
			let token = null
			if(tokenRaw){ try{ token = JSON.parse(tokenRaw).token }catch(e){ token = null } }
			// Determine url string
			let url = null
			if(typeof input === 'string') url = input
			else if(input && input.url) url = input.url
			const sameApi = url && (url.startsWith('/api') || url.indexOf('/api/') !== -1 || url.indexOf('/api') !== -1)
			if(token && sameApi){
				init = init || {}
				init.headers = init.headers || {}
				// don't overwrite Authorization if present
				if(!init.headers['Authorization'] && !init.headers['authorization']){
					init.headers['Authorization'] = 'Bearer ' + token
				}
			}
		}catch(e){}
		return _fetch(input, init)
	}
}catch(e){ /* ignore in non-browser env */ }

const app = createApp(App)
app.use(router)
app.mount('#app')

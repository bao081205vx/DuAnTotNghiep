<template>
  <div class="legacy-page-wrapper">
    <!-- Render the original legacy HTML inside an iframe for perfect fidelity. The iframe src includes a hash so the legacy app shows the requested section. -->
    <iframe :src="iframeSrc" class="legacy-iframe" frameborder="0" title="Legacy Admin" ref="legacyIframe" @load="onIframeLoad"></iframe>

    <!-- When the user is not authenticated, show a blocking overlay that prompts to login. This keeps the legacy iframe untouched but prevents interaction. -->
    <div v-if="!isLoggedIn" class="auth-overlay">
      <div class="auth-box">
        <h2>Yêu cầu đăng nhập</h2>
        <p>Bạn phải đăng nhập để truy cập trang quản trị.</p>
        <div class="buttons">
          <button @click="goLogin" class="btn-login">Đăng nhập</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const legacyIframe = ref(null)

// Map route names/paths to legacy hashes
function mapRouteToHash(routeOrName){
  // prefer explicit mapping by route name
  const name = (routeOrName && routeOrName.name) ? String(routeOrName.name) : (typeof routeOrName === 'string' ? routeOrName : (route && route.name ? String(route.name) : ''))
  if(name === 'products') return 'product-list'
  if(name === 'variants') return 'product-variants'
  if(name === 'sales') return 'sales'
  if(name === 'statistics') return 'statistics'
  if(name === 'customers') return 'customers'
  if(name === 'invoiceDetail' || name === 'invoice-detail') return 'invoice-detail'
  if(name === 'customerEdit' || name === 'edit-customer') return 'edit-customer'
  if(name === 'invoices') return 'invoices'
  if(name === 'producer') return 'producer'
  if(name === 'origin') return 'origin'
  if(name === 'capacity') return 'capacity'
  if(name === 'employees') return 'employees'
  if(name === 'materials') return 'materials'
  if(name === 'brands') return 'brands'
  if(name === 'discounts') return 'discounts'
  if(name === 'productAdd' || name === 'add-product') return 'add-product'
  if(name === 'discountAdd' || name === 'add-discount') return 'add-discount'
  if(name === 'employeeAdd' || name === 'add-employee') return 'add-employee'
  if(name === 'customerAdd' || name === 'add-customer') return 'add-customer'
  // fallback based on path
  const p = (routeOrName && routeOrName.path) ? String(routeOrName.path) : (route && route.path ? String(route.path) : '')
  if(p.startsWith('/products')) return 'product-list'
  if(p.startsWith('/variants')) return 'product-variants'
  if(p.startsWith('/sales')) return 'sales'
  if(p.startsWith('/statistics')) return 'statistics'
  if(p.startsWith('/customers')) return 'customers'
  if(p.startsWith('/invoices/') ) return 'invoice-detail'
  if(p.startsWith('/customers/') && p.indexOf('/edit') !== -1) return 'edit-customer'
  if(p.startsWith('/invoices')) return 'invoices'
  if(p.startsWith('/producer')) return 'producer'
  if(p.startsWith('/origin')) return 'origin'
  if(p.startsWith('/capacity')) return 'capacity'
  if(p.startsWith('/employees')) return 'employees'
  if(p.startsWith('/materials')) return 'materials'
  if(p.startsWith('/brands')) return 'brands'
  if(p.startsWith('/discounts')) return 'discounts'
  if(p.startsWith('/products/add')) return 'add-product'
  if(p.startsWith('/discounts/add')) return 'add-discount'
  if(p.startsWith('/employees/add')) return 'add-employee'
  if(p.startsWith('/customers/add')) return 'add-customer'
  return 'home'
}

const iframeSrc = computed(() => {
  const hash = mapRouteToHash(route)
  // ensure we append hash to the legacy index file
  return `/legacy_index.html#${encodeURIComponent(hash)}`
})

// simple reactive flag for authentication presence
const state = reactive({ logged: !!(typeof window !== 'undefined' && window.localStorage && localStorage.getItem('bagistore_current_user')) })
const isLoggedIn = computed(() => state.logged)

function goLogin(){
  // navigate to SPA /login which redirects to the standalone login page
  try{ router.push('/login') }catch(e){ window.location.href = '/login.html' }
}

// listen for storage events so different tabs update login state
function handleStorageEvent(e){
  try{ if(e && (e.key === 'bagistore_current_user' || e.key === null)){ state.logged = !!localStorage.getItem('bagistore_current_user') } }catch(err){}
}


// Poll iframe hash and sync to router so clicking inside iframe updates parent URL
let pollId = null

// Listen for postMessage events from the iframe (injected script will post navigation events)
function handleLegacyMessage(e){
  try{
    const d = e && e.data
    if(!d || d.type !== 'legacy-navigate') return
    const page = String(d.page || 'home')
    console.debug('[PageWrapper] received legacy-navigate ->', page)
    const map = {
      'product-list': '/products',
      'product-variants': '/variants',
      'invoices': '/invoices',
      'invoice-detail': '/invoices',
      'add-product': '/products/add',
      'add-discount': '/discounts/add',
      'add-employee': '/employees/add',
      'add-customer': '/customers/add',
      'statistics': '/statistics',
      'sales': '/sales',
      'customers': '/customers',
      'producer': '/producer',
      'origin': '/origin',
      'capacity': '/capacity',
      'employees': '/employees',
      'materials': '/materials',
      'brands': '/brands',
      'discounts': '/discounts',
      'home': '/'
    }
    // if the iframe provides a param (e.g., invoice id), navigate to the detailed path
    // Only navigate when we have an explicit mapping. Avoid falling back to '/'+page because
    // that can create unknown routes which trigger the router catch-all redirect to '/'.
    let desired = map[page] || null
    try{
      if(d && d.param){
        if(page === 'invoice-detail') desired = '/invoices/' + encodeURIComponent(String(d.param))
        if(page === 'edit-customer') desired = '/customers/' + encodeURIComponent(String(d.param)) + '/edit'
      }
    }catch(e){}
    if(!desired){
      console.debug('[PageWrapper] Ignoring navigation to unknown legacy page:', page)
      return
    }
    if(router && router.currentRoute && router.currentRoute.value && router.currentRoute.value.path !== desired){
      router.replace(desired).catch(()=>{})
    }
  }catch(err){ }
}

onMounted(() => {
  window.addEventListener('storage', handleStorageEvent)
  window.addEventListener('message', handleLegacyMessage)
  // keep a lightweight poll as a fallback for hash-only navigation
  pollId = setInterval(() => {
    try{
      const el = legacyIframe.value
      if(!el) return
      const win = el.contentWindow
      if(!win) return
      const h = (win.location && win.location.hash) ? String(win.location.hash || '') : ''
      if(!h) return
      const hash = h.replace(/^#/, '')
      const map = {
        'product-list': '/products',
        'product-variants': '/variants',
        'invoices': '/invoices',
        'statistics': '/statistics',
        'sales': '/sales',
        'customers': '/customers',
        'producer': '/producer',
        'origin': '/origin',
        'capacity': '/capacity',
        'employees': '/employees',
        'materials': '/materials',
        'brands': '/brands',
        'discounts': '/discounts',
        'home': '/'
      }
      const desired = map[hash] || null
      if(!desired){
        // ignore unknown hashes rather than navigating to '/'+hash which may cause a fallback
        // redirect to home and a confusing brief view change
        // (this is a best-effort safeguard; known pages must still be added to the mapping)
        //console.debug('[PageWrapper] poll: unknown iframe hash ->', hash)
        return
      }
      if(router && router.currentRoute && router.currentRoute.value && router.currentRoute.value.path !== desired){
        router.replace(desired).catch(()=>{})
      }
    }catch(e){ /* ignore cross-position errors */ }
  }, 400)
})

onBeforeUnmount(() => { if(pollId) clearInterval(pollId); pollId = null })

onBeforeUnmount(() => { try{ window.removeEventListener('storage', handleStorageEvent) }catch(e){} })

// also watch route changes to update iframe src (if user navigates via address bar)
watch(() => route.fullPath, (newVal) => {
  // when route changes, iframeSrc computed will update automatically
  // but force reload the iframe to ensure legacy app sees new hash
  try{ if(legacyIframe.value && legacyIframe.value.contentWindow){ legacyIframe.value.src = iframeSrc.value } }catch(e){}
})

// Called when iframe finishes loading; inject a small script into same-origin iframe to
// patch the legacy `showPage` function (or other navigation hooks) so it posts messages
// to the parent window when navigation happens. This makes navigation inside the
// legacy iframe update the parent URL so F5 refreshes the specific page.
function onIframeLoad(){
  try{
    const el = legacyIframe.value
    if(!el) return
    const doc = el.contentDocument || (el.contentWindow && el.contentWindow.document)
    if(!doc) return
    // inject a script that wraps window.showPage to postMessage to parent
    const script = doc.createElement('script')
    script.type = 'text/javascript'
    // Enhanced injection: wrap showPage to post navigation events, and listen for parent 'set-active' messages
    script.text = `(function(){
      try{
        // wrap showPage so parent gets notified when legacy changes page
        var _o = window.showPage;
        window.showPage = function(p){ try{ if(typeof _o === 'function') _o.apply(this, arguments); }catch(e){} try{ window.parent.postMessage({type:'legacy-navigate', page: p}, '*'); }catch(e){} };
      }catch(e){}
      try{
        // wrap viewInvoiceDetail to notify parent with the invoice id/code when invoked
        if(typeof window.viewInvoiceDetail === 'function' || typeof window.viewInvoiceDetail === 'undefined'){
          var _vid = window.viewInvoiceDetail;
          window.viewInvoiceDetail = function(id){ try{ if(typeof _vid === 'function') _vid.apply(this, arguments); }catch(e){} try{ window.parent.postMessage({type:'legacy-navigate', page: 'invoice-detail', param: id}, '*'); }catch(e){} };
        }
      }catch(e){}
      try{
        // wrap loadCustomerForEdit so parent can be informed when edit is opened with an id
        var _lce = window.loadCustomerForEdit;
        window.loadCustomerForEdit = function(id){ try{ if(typeof _lce === 'function') _lce.apply(this, arguments); }catch(e){} try{ window.parent.postMessage({type:'legacy-navigate', page: 'edit-customer', param: id}, '*'); }catch(e){} };
      }catch(e){}
      try{
        window.addEventListener('message', function(ev){
          try{
            var d = ev && ev.data;
            if(!d) return;
            if(d.type === 'set-active'){
              var p = d.page;
              try{
                if(typeof window.showPage === 'function'){
                  window.showPage(p);
                } else {
                  // fallback: find a button whose onclick calls showPage with the page
                  var btns = document.querySelectorAll('button[onclick]');
                  for(var i=0;i<btns.length;i++){
                    var a = btns[i].getAttribute('onclick') || '';
                    if(a.indexOf("showPage('" + p + "')") !== -1 || a.indexOf('showPage(\"' + p + '\"') !== -1){ try{ btns[i].click(); break; }catch(e){} }
                  }
                }
                // if parent provided a param, try to call appropriate loader functions
                if(d.param){
                  try{
                    if(p === 'invoice-detail' && typeof window.viewInvoiceDetail === 'function'){
                      window.viewInvoiceDetail(d.param);
                    }
                    if(p === 'edit-customer'){
                      if(typeof window.loadCustomerForEdit === 'function'){
                        window.loadCustomerForEdit(d.param);
                      } else if(typeof window.initializeEditCustomerPage === 'function'){
                        try{ window.initializeEditCustomerPage(); }catch(e){}
                        if(typeof window.loadCustomerForEdit === 'function') try{ window.loadCustomerForEdit(d.param); }catch(e){}
                      }
                    }
                  }catch(err){}
                }
              }catch(err){}
            }
          }catch(e){}
        }, false);
      }catch(e){}
    })();`
    doc.head.appendChild(script)
    console.debug('[PageWrapper] injected navigation wrapper into iframe')

    // Ask the iframe to set its active menu based on current route hash (best-effort)
    try{
      var desired = mapRouteToHash(route);
      var payload = { type: 'set-active', page: desired };
      try{ if(route && route.params && route.params.id) payload.param = route.params.id }catch(e){}
      try{ if(el.contentWindow && el.contentWindow.postMessage){ el.contentWindow.postMessage(payload, '*'); } }catch(e){}
    }catch(e){ /* ignore injection errors (inner) */ }
  }catch(e){ /* ignore onIframeLoad outer */ }
}
</script>

<style scoped>
.legacy-page-wrapper { position: fixed; inset: 0; width: 100vw; height: 100vh; margin: 0; padding: 0; }
.legacy-iframe { width: 100%; height: 100%; border: none; }
</style>

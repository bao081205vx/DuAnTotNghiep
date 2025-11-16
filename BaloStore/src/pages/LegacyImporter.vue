<template>
  <div class="legacy-importer" ref="root">
    <!-- Render header and sidebar as native Vue components for easier migration -->
    <div v-if="headerHtml" class="legacy-header-wrapper"><LegacyHeader :html="headerHtml" /></div>
    <div v-if="sidebarHtml" class="legacy-sidebar-wrapper"><LegacySidebar :html="sidebarHtml" /></div>

    <!-- The main legacy body (rest of content) injected here -->
    <div class="legacy-main" v-html="bodyHtml" ref="bodyContainer"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import LegacyHeader from '@/components/legacy/LegacyHeader.vue'
import LegacySidebar from '@/components/legacy/LegacySidebar.vue'

const headerHtml = ref('')
const sidebarHtml = ref('')
const bodyHtml = ref('')
const injectedScripts = []
const injectedLinks = []
const injectedStyles = []

onMounted(async () => {
  try{
    const res = await fetch('/legacy_index.html')
    const text = await res.text()
    const parser = new DOMParser()
    const doc = parser.parseFromString(text, 'text/html')

    // Inject link styles
    const links = Array.from(doc.querySelectorAll('link[rel="stylesheet"]'))
    links.forEach(l => {
      const href = l.getAttribute('href')
      if(!href) return
      const already = Array.from(document.querySelectorAll('link[rel="stylesheet"]')).some(x => x.getAttribute('href') === href)
      if(already) return
      const linkEl = document.createElement('link')
      linkEl.rel = 'stylesheet'
      linkEl.href = href
      document.head.appendChild(linkEl)
      injectedLinks.push(linkEl)
    })

    // Inject inline styles
    const styles = Array.from(doc.querySelectorAll('style'))
    styles.forEach(s => {
      const txt = s.textContent || ''
      const already = Array.from(document.querySelectorAll('style')).some(x => x.textContent && x.textContent.includes((txt||'').slice(0,40)))
      if(already) return
      const styleEl = document.createElement('style')
      styleEl.type = 'text/css'
      styleEl.textContent = txt
      document.head.appendChild(styleEl)
      injectedStyles.push(styleEl)
    })

    // Extract header and aside and the rest of body
    const header = doc.querySelector('header')
    const aside = doc.querySelector('aside')
    const body = doc.querySelector('body')

    headerHtml.value = header ? header.outerHTML : ''
    sidebarHtml.value = aside ? aside.outerHTML : ''

    // Remove header and aside from a cloned body so we can inject only the rest
    let mainHtml = ''
    if(body){
      const bodyClone = body.cloneNode(true)
      const h = bodyClone.querySelector('header')
      const a = bodyClone.querySelector('aside')
      if(h) h.remove()
      if(a) a.remove()
      // remove any <script> tags from mainHtml (we'll inject scripts later)
      Array.from(bodyClone.querySelectorAll('script')).forEach(s=>s.remove())
      mainHtml = bodyClone.innerHTML
    } else {
      mainHtml = text
    }
    bodyHtml.value = mainHtml

    // Inject scripts (ordered)
    const scripts = Array.from(doc.querySelectorAll('script'))
    scripts.forEach(s => {
      const src = s.getAttribute('src')
      const type = s.getAttribute('type') || 'text/javascript'
      if(src && src.includes('cdn-cgi')) return
      const scriptEl = document.createElement('script')
      scriptEl.type = type
      if(src){ scriptEl.src = src; const cross = s.getAttribute('crossorigin'); if(cross) scriptEl.setAttribute('crossorigin', cross) }
      else { scriptEl.text = s.textContent || '' }
      document.body.appendChild(scriptEl)
      injectedScripts.push(scriptEl)
    })

    // Patch legacy showPage to update browser path so refresh preserves current screen
    try{
      const patchScript = document.createElement('script')
      patchScript.type = 'text/javascript'
      patchScript.text = `
        (function(){
          try{
            const orig = window.showPage;
            window.showPage = function(page, ev){
              try{
                const map = { 'product-list':'/products', 'product-variants':'/variants', 'sales':'/sales', 'customers':'/customers', 'materials':'/materials', 'brands':'/brands', 'discounts':'/discounts', 'home':'/' };
                const p = map[page] || ('/' + page);
                // use replaceState to avoid filling history on each tab switch
                history.replaceState({}, '', p);
              }catch(e){}
              if(typeof orig === 'function'){
                try{ return orig(page, ev); }catch(e){ console.debug('patched showPage orig failed', e); }
              }
            };
          }catch(e){ console.debug('showPage patch failed', e); }
        })();
      `
      document.body.appendChild(patchScript)
      injectedScripts.push(patchScript)
    }catch(e){ console.debug('failed to append showPage patch', e) }

    setTimeout(()=>{
      try{ if(window.initializeAddCustomerPage) window.initializeAddCustomerPage(); }catch(e){}
      try{ if(window.initializeMaterialsPage) window.initializeMaterialsPage(); }catch(e){}
      try{ if(window.initializeBrandsPage) window.initializeBrandsPage(); }catch(e){}
    }, 300)

  }catch(err){
    console.error('Failed to import legacy page', err)
    bodyHtml.value = '<pre style="white-space:pre-wrap;color:#900">Failed to load legacy page: '+String(err)+'</pre>'
  }
})

onBeforeUnmount(() => {
  injectedScripts.forEach(s => { try{ s.remove(); }catch(e){} })
  injectedLinks.forEach(l => { try{ l.remove(); }catch(e){} })
  injectedStyles.forEach(s => { try{ s.remove(); }catch(e){} })
})
</script>

<style scoped>
.legacy-importer { min-height: 100vh; }
.legacy-header-wrapper { position: relative; z-index: 40; }
.legacy-sidebar-wrapper { position: relative; z-index: 30; }
.legacy-main { position: relative; z-index: 20; }
</style>

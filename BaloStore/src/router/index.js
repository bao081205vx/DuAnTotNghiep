import { createRouter, createWebHistory } from 'vue-router'
import PageWrapper from '@/pages/PageWrapper.vue'

const routes = [
  { path: '/', name: 'home', component: PageWrapper, props: { page: 'home' } },
  { path: '/statistics', name: 'statistics', component: PageWrapper, props: { page: 'statistics' }, meta: { requiresAuth: true } },
  { path: '/sales', name: 'sales', component: PageWrapper, props: { page: 'sales' }, meta: { requiresAuth: true } },
  { path: '/invoices', name: 'invoices', component: PageWrapper, props: { page: 'invoices' }, meta: { requiresAuth: true } },
  // invoice detail route (deep link to a specific invoice)
  { path: '/invoices/:id', name: 'invoiceDetail', component: PageWrapper, props: route => ({ page: 'invoice-detail', id: route.params.id }) },
  { path: '/products', name: 'products', component: PageWrapper, props: { page: 'product-list' }, meta: { requiresAuth: true } },
  { path: '/products/add', name: 'productAdd', component: PageWrapper, props: { page: 'add-product' }, meta: { requiresAuth: true } },
  { path: '/variants', name: 'variants', component: PageWrapper, props: { page: 'product-variants' }, meta: { requiresAuth: true } },
  { path: '/customers', name: 'customers', component: PageWrapper, props: { page: 'customers' }, meta: { requiresAuth: true } },
  // edit customer deep link
  { path: '/customers/:id/edit', name: 'customerEdit', component: PageWrapper, props: route => ({ page: 'edit-customer', id: route.params.id }), meta: { requiresAuth: true } },
  { path: '/customers/add', name: 'customerAdd', component: PageWrapper, props: { page: 'add-customer' }, meta: { requiresAuth: true } },
  { path: '/materials', name: 'materials', component: PageWrapper, props: { page: 'materials' }, meta: { requiresAuth: true } },
  { path: '/brands', name: 'brands', component: PageWrapper, props: { page: 'brands' }, meta: { requiresAuth: true } },
  { path: '/producer', name: 'producer', component: PageWrapper, props: { page: 'producer' }, meta: { requiresAuth: true } },
  { path: '/origin', name: 'origin', component: PageWrapper, props: { page: 'origin' }, meta: { requiresAuth: true } },
  { path: '/capacity', name: 'capacity', component: PageWrapper, props: { page: 'capacity' }, meta: { requiresAuth: true } },
  { path: '/employees', name: 'employees', component: PageWrapper, props: { page: 'employees' }, meta: { requiresAuth: true } },
  { path: '/employees/add', name: 'employeeAdd', component: PageWrapper, props: { page: 'add-employee' }, meta: { requiresAuth: true } },
  { path: '/discounts', name: 'discounts', component: PageWrapper, props: { page: 'discounts' }, meta: { requiresAuth: true } },
  { path: '/discounts/add', name: 'discountAdd', component: PageWrapper, props: { page: 'add-discount' }, meta: { requiresAuth: true } },
  // legacy standalone login page (redirect to public/login.html)
  { path: '/login', name: 'login', component: () => import('@/pages/LoginRedirect.vue') },
  { path: '/login-required', name: 'login-required', component: () => import('@/pages/LoginRequired.vue') },
  // catch-all fallback to home
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Global guard: redirect to /login-required when accessing admin routes while not authenticated
router.beforeEach((to, from, next) => {
  try{
    const requires = to && to.meta && to.meta.requiresAuth
    if(!requires) return next()
    const raw = (typeof window !== 'undefined' && window.localStorage) ? localStorage.getItem('bagistore_current_user') : null
    const logged = !!raw
    if(!logged){
      return next({ name: 'login-required', query: { redirect: to.fullPath } })
    }
    // role-based restrictions: Sales staff ('Nhân viên bán hàng') cannot access employees, discounts, statistics
    try{
      const user = JSON.parse(raw || 'null')
      const role = (user && (user.vaiTroTen || user.vai_tro_ten || user.role)) ? String(user.vaiTroTen || user.vai_tro_ten || user.role) : null
      const restrictedForSales = ['employees','discounts','statistics']
      if(role && role.toLowerCase().indexOf('nhân viên bán') !== -1){
        if(restrictedForSales.indexOf(to.name) !== -1){
          return next({ name: 'login-required', query: { forbidden: 1, redirect: to.fullPath } })
        }
      }
    }catch(err){}
    return next()
  }catch(e){ return next() }
})

export default router

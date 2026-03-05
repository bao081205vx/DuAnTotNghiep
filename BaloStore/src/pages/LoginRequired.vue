<template>
  <div class="login-required-wrap">
    <div class="notice">
        <h2 v-if="!forbidden">Yêu cầu đăng nhập</h2>
        <h2 v-else>Không có quyền</h2>
        <p v-if="!forbidden">Bạn phải đăng nhập để truy cập trang quản trị.</p>
        <p v-else>Bạn không có quyền truy cập trang này. Vui lòng liên hệ quản trị viên nếu cần.</p>
        <div class="actions">
          <button v-if="!forbidden" @click="goLogin" class="btn btn-primary">Đăng nhập</button>
          <button v-else @click="goHome" class="btn btn-primary">Về Trang chủ</button>
          <button @click="goHome" class="btn btn-link">Quay lại Trang chủ</button>
        </div>
      </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
const router = useRouter()
const route = useRoute()
const forbidden = route.query && (route.query.forbidden === '1' || route.query.forbidden === 1 || route.query.forbidden === 'true')
function goLogin(){
  // preserve redirect target
  const target = route.query.redirect || '/'
  try{ router.push({ name: 'login', query: { redirect: target } }) }catch(e){ window.location.href = '/login.html' }
}
function goHome(){ router.replace('/') }
</script>

<style scoped>
/* Full-screen backdrop and centered card */
.login-required-wrap{
  position: fixed;
  inset: 0;
  display:flex;
  align-items:center;
  justify-content:center;
  background: rgba(0,0,0,0.28);
  z-index: 1100;
}
.notice{
  width: min(680px, 92%);
  max-width: 680px;
  text-align:center;
  background: #fff;
  padding:28px;
  border-radius:10px;
  box-shadow:0 10px 30px rgba(2,6,23,0.18);
  transform: translateZ(0);
}
.notice h2{ margin:0 0 8px; color:#111827; font-size:20px }
.notice p{ color:#6b7280; margin-bottom:18px }
.actions{ display:flex; gap:8px; justify-content:center }
.btn{ padding:8px 14px; border-radius:6px }
.btn-primary{ background:#f97316; color:white; border:none }
.btn-link{ background:transparent; border:none; color:#6b7280 }
</style>

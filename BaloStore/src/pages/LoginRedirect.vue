<template>
  <div class="login-redirect" style="height:100vh;display:flex;align-items:center;justify-content:center;">
    <div style="text-align:center;color:#374151">
      <div style="font-size:18px;font-weight:600;margin-bottom:8px">Đang chuyển tới trang đăng nhập…</div>
      <div style="color:#6b7280">Nếu không tự động, <a href="/login.html">bấm vào đây</a>.</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

// Navigate the top-level window to the legacy login page.
// The login.html will then rewrite the URL to '/login' (history.replaceState)
// so the final address displayed is 'http://.../login' while the UI remains the original login UI.
onMounted(() => {
  try {
    // force top-level navigation (exit SPA iframe if present)
    if (window.top) window.top.location.href = '/login.html'
    else window.location.href = '/login.html'
  } catch (e) {
    // fallback
    window.location.href = '/login.html'
  }
})
</script>

<style scoped>
.login-redirect { background: #f3f4f6; }
</style>

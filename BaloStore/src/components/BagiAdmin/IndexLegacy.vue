<template>
  <div class="legacy-container">
    <!-- Use an iframe to render the original legacy_index.html verbatim. -->
    <iframe src="/legacy_index.html" class="legacy-iframe" frameborder="0" title="Legacy Admin"></iframe>
  </div>
</template>

<script>
export default {
  name: 'IndexLegacy'
}
</script>

<style scoped>
.legacy-container {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  z-index: 9999;
}
.legacy-iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: none;
}
</style>

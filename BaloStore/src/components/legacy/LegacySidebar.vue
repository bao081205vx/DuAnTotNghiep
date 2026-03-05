<template>
  <div class="legacy-sidebar" v-html="html"></div>
</template>

<script setup>
import { defineProps } from 'vue'
const props = defineProps({ html: { type: String, default: '' } })
</script>

<style scoped>
/* minimal wrapper for sidebar; legacy CSS provides full styling */
.legacy-sidebar { width: 100%; }
</style>

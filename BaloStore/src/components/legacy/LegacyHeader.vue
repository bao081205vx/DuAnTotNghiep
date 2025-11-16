<template>
  <div class="legacy-header" v-html="html"></div>
</template>

<script setup>
import { defineProps } from 'vue'
const props = defineProps({ html: { type: String, default: '' } })
</script>

<style scoped>
/* keep header styles scoped minimal; real styles come from legacy CSS injected globally */
.legacy-header { width: 100%; }
</style>

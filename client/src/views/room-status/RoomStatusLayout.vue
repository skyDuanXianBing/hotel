<template>
  <div class="room-status-layout">
    <div class="content-area">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.room-status-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.content-area {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
</style>

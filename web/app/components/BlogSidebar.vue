<script setup lang="ts">
import { mdiBookOpenVariant } from '@mdi/js'

const appStore = useAppStore()
</script>

<template>
  <div>
    <NuxtLink to="/" class="text-decoration-none d-flex align-center">
      <VAvatar color="primary-soft" size="40">
        <VImg height="28" width="28" :src="'/img/saubhagya-mark.png'" />
      </VAvatar>
      <h3 class="text-headline-small ml-2 mb-3 mt-3">Saubhagya SMS</h3>
    </NuxtLink>
    <p class="mt-0">
      httpSMS is an
      <a
        class="text-decoration-none"
        href="https://github.com/NdoleStudio/httpsms"
        >open source</a
      >
      application that converts your android phone into an SMS gateway so you
      can send and receive SMS messages using a simple HTTP API.
    </p>
    <VBtn variant="tonal" :href="appStore.appData.documentationUrl">
      <VIcon start :icon="mdiBookOpenVariant" />
      Documentation
    </VBtn>
  </div>
</template>

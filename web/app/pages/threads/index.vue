<script setup lang="ts">
definePageMeta({
  middleware: ['auth'],
})

useHead({
  title: 'Threads - Saubhagya SMS',
})

const { lgAndUp } = useVDisplay()
const authStore = useAuthStore()
const phonesStore = usePhonesStore()
const threadsStore = useThreadsStore()

onMounted(async () => {
  await authStore.loadUser()
  await phonesStore.loadPhones()
  await threadsStore.loadThreads()
})
</script>

<template>
  <VContainer class="pa-0" fluid :class="{ 'fill-height': lgAndUp }">
    <VRow
      v-if="lgAndUp"
      align="center"
      :class="{ 'fill-height': lgAndUp }"
      justify="center"
    >
      <div :class="{ 'mt-n16': lgAndUp }">
        <div class="sg-empty text-center">
          <div class="sg-empty__badge">
            <img src="/img/saubhagya-mark.png" alt="" width="72" height="72" />
          </div>
          <h3 class="text-headline-medium mt-6 mb-2">Select a conversation</h3>
          <p class="text-medium-emphasis mt-0 mb-6">
            Pick a thread on the left, or start a new message.
          </p>
          <VBtn color="primary" size="large" :to="{ name: 'messages' }">
            New message
          </VBtn>
        </div>
      </div>
    </VRow>
    <VRow v-else justify="end">
      <VCol class="px-0 py-0">
        <MessageThreadHeader />
        <MessageThread />
      </VCol>
    </VRow>
  </VContainer>
</template>

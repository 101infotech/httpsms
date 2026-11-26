<script setup lang="ts">
definePageMeta({
  middleware: ['guest'],
  layout: 'blank',
})

useSeoMeta({
  title: 'Log in to your account - Saubhagya SMS',
  description:
    'Log in to Saubhagya SMS to send and receive SMS messages through your own Android phone with a simple HTTP API.',
})

const route = useRoute()
const to = computed(() => (route.query.to as string) || '/threads')
</script>

<template>
  <VContainer fluid class="fill-height d-flex align-center justify-center">
    <VRow align="center" justify="center">
      <VCol cols="12" md="4" xl="3" class="mt-n16">
        <div class="text-center mb-5">
          <VAvatar color="primary-soft" size="56" class="mt-n4 mr-4">
            <VImg
              :src="'/img/saubhagya-mark.png'"
              alt="Saubhagya"
              width="40"
              height="40"
            />
          </VAvatar>
          <span class="text-display-small">Welcome</span>
        </div>
        <p class="text-body-large text-center text-medium-emphasis mt-1 mb-4">
          Sign in to Saubhagya SMS to manage your phones,
          <br />
          messages and API keys.
        </p>
        <VCard max-width="500" class="mx-auto pa-4">
          <VCardText class="px-4 pt-2 pb-0">
            <ClientOnly>
              <FirebaseAuth :to="to" />
            </ClientOnly>
          </VCardText>
        </VCard>
        <div class="text-center mt-4">
          <BackButton />
        </div>
      </VCol>
    </VRow>
  </VContainer>
</template>

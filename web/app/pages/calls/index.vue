<script setup lang="ts">
import {
  mdiArrowLeft,
  mdiMessageTextOutline,
  mdiPhone,
  mdiPhoneMissed,
  mdiRefresh,
} from '@mdi/js'
import Pusher from 'pusher-js'
import type { Channel } from 'pusher-js'
import type {
  EntitiesContact,
  EntitiesMessage,
  EntitiesMessageThread,
  EntitiesPhone,
} from '~~/shared/types/api'
import { getApiErrorMessage } from '~/utils/api-error'

definePageMeta({
  middleware: ['auth'],
})

useHead({
  title: 'Missed calls - Saubhagya SMS',
})

const PAGE_SIZE = 100

const router = useRouter()
const config = useRuntimeConfig()
const { lgAndUp } = useVDisplay()
const { formatPhoneNumber, formatTimestamp, humanizeTimeShort } = useFilters()
const authStore = useAuthStore()
const phonesStore = usePhonesStore()
const threadsStore = useThreadsStore()
const { useApi } = useApiComposable()
const apiFetch = useApi()

const calls = ref<EntitiesMessage[]>([])
const selectedOwner = ref<string | null>(null)
const contactNames = ref<Record<string, string>>({})
const loading = ref(true)
const loadingMore = ref(false)
const refreshing = ref(false)
const hasMore = ref(false)
const errorMessage = ref('')

let skip = 0
let webhookChannel: Channel | null = null

const phoneFilters = computed(() =>
  phonesStore.phones.map((phone: EntitiesPhone) => ({
    title: phonesStore.phoneLabel(phone),
    value: phone.phone_number,
  })),
)

const visibleCalls = computed(() =>
  selectedOwner.value
    ? calls.value.filter((call) => call.owner === selectedOwner.value)
    : calls.value,
)

const todayCount = computed(() => {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  return countSince(start)
})

const lastSevenDaysCount = computed(() => {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - 6)
  return countSince(start)
})

function countSince(start: Date): number {
  return visibleCalls.value.filter(
    (call) => new Date(call.order_timestamp) >= start,
  ).length
}

function contactName(call: EntitiesMessage): string {
  return contactNames.value[call.contact] ?? ''
}

function callerLabel(call: EntitiesMessage): string {
  return contactName(call) || formatPhoneNumber(call.contact)
}

function callPhone(call: EntitiesMessage): EntitiesPhone | undefined {
  return phonesStore.phones.find((phone) => phone.phone_number === call.owner)
}

function ownerLabel(call: EntitiesMessage): string {
  const phone = callPhone(call)
  return phone ? phonesStore.phoneLabel(phone) : formatPhoneNumber(call.owner)
}

function simLabel(call: EntitiesMessage): string {
  return call.sim === 'DEFAULT' ? 'default SIM' : call.sim
}

function isCurrentFilter(owner: string | null): boolean {
  return selectedOwner.value === owner
}

async function searchMissedCalls(offset: number): Promise<EntitiesMessage[]> {
  const owners = phonesStore.phones.map((phone) => phone.phone_number)
  if (owners.length === 0) return []

  const response = await apiFetch<{ data: EntitiesMessage[] }>(
    '/v1/messages/calls/missed',
    {
      params: {
        owners,
        skip: offset,
        limit: PAGE_SIZE,
      },
    },
  )
  return response.data ?? []
}

async function loadCalls(reset = false) {
  if (reset) {
    loading.value = true
    errorMessage.value = ''
  } else {
    loadingMore.value = true
  }

  const offset = reset ? 0 : skip

  try {
    const results = await searchMissedCalls(offset)
    calls.value = reset ? results : [...calls.value, ...results]
    skip = offset + results.length
    hasMore.value = results.length === PAGE_SIZE
    errorMessage.value = ''
  } catch (error: unknown) {
    errorMessage.value = getApiErrorMessage(
      error,
      'Could not load missed calls',
    )
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  await loadCalls()
}

async function prependLatestCall() {
  if (loading.value || loadingMore.value || refreshing.value) return

  refreshing.value = true
  try {
    const latest = await searchMissedCalls(0)
    const known = new Set(calls.value.map((call) => call.id))
    const fresh = latest.filter((call) => !known.has(call.id))
    if (fresh.length === 0) return

    calls.value = [...fresh, ...calls.value]
    // The server list shifted by the number of new calls; keep the next
    // "load more" offset pointing past everything already displayed.
    skip += fresh.length
    errorMessage.value = ''
  } catch (error: unknown) {
    errorMessage.value = getApiErrorMessage(
      error,
      'Could not load missed calls',
    )
  } finally {
    refreshing.value = false
  }
}

async function loadContactNames() {
  const names: Record<string, string> = {}

  try {
    for (let page = 0; page < 10; page++) {
      const response = await apiFetch<{
        data: EntitiesContact[]
        total?: number
      }>('/v1/contacts', {
        params: {
          skip: page * 100,
          limit: 100,
          sort_by: 'name',
          sort_descending: false,
        },
      })
      for (const contact of response.data ?? []) {
        if (!contact.name?.trim()) continue
        for (const number of contact.phone_numbers) {
          names[number] = contact.name.trim()
        }
      }
      if ((response.data?.length ?? 0) < 100) break
    }
  } catch (error: unknown) {
    // Contact names are a nicety; the page still works with phone numbers.
    console.error(error)
  }

  contactNames.value = names
}

async function openConversation(call: EntitiesMessage) {
  const cached = threadsStore.threads.find(
    (thread) => thread.owner === call.owner && thread.contact === call.contact,
  )
  if (cached) {
    await router.push(`/threads/${cached.id}`)
    return
  }

  try {
    const response = await apiFetch<{ data: EntitiesMessageThread[] }>(
      '/v1/message-threads',
      {
        params: {
          owner: call.owner,
          query: call.contact,
          skip: 0,
          limit: 20,
          is_archived: false,
        },
      },
    )
    const thread = (response.data ?? []).find(
      (item) => item.contact === call.contact,
    )
    if (thread) {
      await router.push(`/threads/${thread.id}`)
      return
    }
  } catch (error: unknown) {
    console.error(error)
  }

  await router.push({ path: '/messages', query: { to: call.contact } })
}

async function refresh() {
  if (loading.value) return

  if (phonesStore.phones.length === 0) {
    loading.value = true
    try {
      await phonesStore.loadPhones()
    } catch (error: unknown) {
      errorMessage.value = getApiErrorMessage(
        error,
        'Could not load missed calls',
      )
      loading.value = false
      return
    }
  }

  await loadCalls(true)
}

onMounted(async () => {
  try {
    await authStore.loadUser()
    await phonesStore.loadPhones()
    await loadCalls(true)
  } catch (error: unknown) {
    errorMessage.value = getApiErrorMessage(
      error,
      'Could not load missed calls',
    )
  } finally {
    loading.value = false
    loadingMore.value = false
  }

  void loadContactNames()

  if (!authStore.user?.id) return
  const pusher = new Pusher(config.public.pusherKey as string, {
    cluster: config.public.pusherCluster as string,
  })
  webhookChannel = pusher.subscribe(authStore.user.id)
  webhookChannel.bind('message.call.missed', () => {
    void prependLatestCall()
  })
})

onBeforeUnmount(() => {
  if (webhookChannel) webhookChannel.unsubscribe()
})
</script>

<template>
  <VContainer fluid class="px-0 pt-0" :class="{ 'fill-height': lgAndUp }">
    <div class="w-100">
      <VAppBar>
        <VBtn icon to="/threads">
          <VIcon :icon="mdiArrowLeft" />
        </VBtn>
        <VToolbarTitle>Missed calls</VToolbarTitle>
        <VProgressLinear
          :active="loading"
          color="primary"
          :indeterminate="loading"
          location="bottom"
          absolute
        />
      </VAppBar>

      <VContainer class="pt-6">
        <div class="d-flex flex-wrap align-center mb-6">
          <div class="flex-grow-1">
            <h1 class="sg-page-title">Missed calls</h1>
            <p class="text-body-large text-medium-emphasis mt-2 mb-0">
              Calls your phones missed, with a one-tap way back to the caller.
            </p>
          </div>
          <div class="d-flex mt-4 mt-md-0" style="gap: 12px">
            <div class="sg-stat">
              <span class="sg-stat__value">{{ todayCount }}</span>
              <span class="sg-stat__label">Today</span>
            </div>
            <div class="sg-stat">
              <span class="sg-stat__value">{{ lastSevenDaysCount }}</span>
              <span class="sg-stat__label">Last 7 days</span>
            </div>
          </div>
        </div>

        <VAlert
          v-if="errorMessage && calls.length > 0"
          variant="tonal"
          prominent
          type="warning"
        >
          <h6 class="text-title-large font-weight-bold">{{ errorMessage }}</h6>
        </VAlert>

        <div class="d-flex flex-wrap align-center mb-5" style="gap: 8px">
          <VBtn
            class="sg-chip"
            :class="{ 'sg-chip--on': isCurrentFilter(null) }"
            rounded="pill"
            variant="flat"
            @click="selectedOwner = null"
          >
            All
          </VBtn>
          <VBtn
            v-for="phone in phoneFilters"
            :key="phone.value"
            class="sg-chip"
            :class="{ 'sg-chip--on': isCurrentFilter(phone.value) }"
            rounded="pill"
            variant="flat"
            @click="selectedOwner = phone.value"
          >
            {{ phone.title }}
          </VBtn>
          <VSpacer />
          <VBtn
            variant="text"
            color="primary"
            :loading="loading"
            :disabled="loading"
            :prepend-icon="mdiRefresh"
            @click="refresh"
          >
            Refresh
          </VBtn>
        </div>

        <div v-if="loading && calls.length === 0" class="py-16 text-center">
          <VProgressCircular indeterminate color="primary" size="40" />
        </div>

        <div
          v-else-if="errorMessage && calls.length === 0"
          class="sg-empty text-center py-16"
        >
          <div class="sg-empty__badge">
            <img src="/img/saubhagya-mark.png" alt="" width="72" height="72" />
          </div>
          <h3 class="text-headline-medium mt-6 mb-2">
            Could not load missed calls
          </h3>
          <p class="text-medium-emphasis mt-0 mb-6">
            Check your connection and try again.
          </p>
          <VBtn color="primary" size="large" rounded="pill" @click="refresh">
            Try again
          </VBtn>
        </div>

        <div
          v-else-if="visibleCalls.length === 0"
          class="sg-empty text-center py-16"
        >
          <div class="sg-empty__badge">
            <img src="/img/saubhagya-mark.png" alt="" width="72" height="72" />
          </div>
          <h3 class="text-headline-medium mt-6 mb-2">No missed calls</h3>
          <p class="text-medium-emphasis mt-0 mb-0">
            When one of your phones misses a call, it will show up here.
          </p>
        </div>

        <VRow v-else>
          <VCol v-for="call in visibleCalls" :key="call.id" cols="12">
            <VCard class="sg-card sg-call-card" rounded="xl" variant="flat">
              <VCardText
                class="d-flex flex-wrap align-center"
                style="gap: 16px"
              >
                <VAvatar color="primary-soft" size="48">
                  <VIcon color="primary" :icon="mdiPhoneMissed" />
                </VAvatar>

                <div class="flex-grow-1" style="min-width: 200px">
                  <div class="text-title-medium font-weight-bold">
                    <VTooltip
                      v-if="contactName(call)"
                      :text="formatPhoneNumber(call.contact)"
                      location="bottom"
                    >
                      <template #activator="{ props }">
                        <span v-bind="props">{{ callerLabel(call) }}</span>
                      </template>
                    </VTooltip>
                    <span v-else>{{ callerLabel(call) }}</span>
                  </div>
                  <div class="text-body-medium text-medium-emphasis">
                    Received on {{ ownerLabel(call) }} · {{ simLabel(call) }}
                  </div>
                </div>

                <div class="text-body-medium text-medium-emphasis">
                  <VTooltip :text="formatTimestamp(call.order_timestamp)">
                    <template #activator="{ props }">
                      <span v-bind="props">{{
                        humanizeTimeShort(call.order_timestamp)
                      }}</span>
                    </template>
                  </VTooltip>
                </div>

                <div class="d-flex flex-wrap" style="gap: 8px">
                  <VBtn
                    variant="text"
                    color="primary"
                    size="small"
                    :prepend-icon="mdiMessageTextOutline"
                    @click="openConversation(call)"
                  >
                    Open conversation
                  </VBtn>
                  <VBtn
                    variant="tonal"
                    color="primary"
                    size="small"
                    :prepend-icon="mdiPhone"
                    :href="`tel:${call.contact}`"
                  >
                    Call back
                  </VBtn>
                </div>
              </VCardText>
            </VCard>
          </VCol>
        </VRow>

        <div v-if="hasMore && !loading" class="text-center mt-6">
          <VBtn
            color="primary"
            variant="tonal"
            rounded="pill"
            :loading="loadingMore"
            :disabled="loadingMore"
            @click="loadMore"
          >
            Load more
          </VBtn>
        </div>
      </VContainer>
    </div>
  </VContainer>
</template>

<style lang="scss" scoped>
.sg-stat {
  min-width: 96px;
  padding: 12px 18px;
  border-radius: 18px;
  background: var(--sg-surface);
  box-shadow: var(--sg-shadow-sm);
  text-align: center;
}

.sg-stat__value {
  display: block;
  font-family: var(--sg-font-display);
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--sg-primary);
}

.sg-stat__label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--sg-muted);
}

.sg-call-card {
  transition: box-shadow 0.15s ease;

  &:hover {
    box-shadow: var(--sg-shadow);
  }
}
</style>

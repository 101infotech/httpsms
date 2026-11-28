<script setup lang="ts">
import { mdiRefresh } from '@mdi/js'
import { formatDistanceToNow } from 'date-fns'
import type { EntitiesHeartbeat, EntitiesPhone } from '~~/shared/types/api'

definePageMeta({
  middleware: ['auth'],
})

useHead({
  title: 'Phone status - Saubhagya SMS',
})

const REFRESH_MS = 60_000
const ONLINE_MINUTES = 10
const DELAYED_MINUTES = 60

const phonesStore = usePhonesStore()
const { formatPhoneNumber } = useFilters()

type Status = 'online' | 'delayed' | 'offline' | 'unknown'

interface PhoneRow {
  phone: EntitiesPhone
  last: EntitiesHeartbeat | null
  minutesAgo: number | null
  status: Status
}

const rows = ref<PhoneRow[]>([])
const loading = ref(true)
const updatedAt = ref<Date | null>(null)
let timer: ReturnType<typeof setInterval> | undefined

const statusMeta: Record<Status, { label: string; color: string }> = {
  online: { label: 'Online', color: 'success' },
  delayed: { label: 'Delayed', color: 'warning' },
  offline: { label: 'Offline', color: 'error' },
  unknown: { label: 'No heartbeat yet', color: 'secondary' },
}

function statusFor(minutesAgo: number | null): Status {
  if (minutesAgo === null) return 'unknown'
  if (minutesAgo <= ONLINE_MINUTES) return 'online'
  if (minutesAgo <= DELAYED_MINUTES) return 'delayed'
  return 'offline'
}

async function latestHeartbeat(
  phone: EntitiesPhone,
): Promise<EntitiesHeartbeat | null> {
  try {
    const response = await apiFetch<{ data: EntitiesHeartbeat[] }>(
      '/v1/heartbeats',
      { query: { limit: 1, owner: phone.phone_number } },
    )
    return response.data[0] ?? null
  } catch {
    return null
  }
}

async function load(force = false) {
  loading.value = true
  try {
    await phonesStore.loadPhones(force)
    const results = await Promise.all(
      phonesStore.phones.map(async (phone) => {
        const last = await latestHeartbeat(phone)
        const minutesAgo = last
          ? Math.max(
              0,
              Math.round(
                (Date.now() - new Date(last.timestamp).getTime()) / 60000,
              ),
            )
          : null
        return { phone, last, minutesAgo, status: statusFor(minutesAgo) }
      }),
    )
    // Offline phones first, then delayed, then online
    const order: Record<Status, number> = {
      offline: 0,
      unknown: 1,
      delayed: 2,
      online: 3,
    }
    rows.value = results.sort((a, b) => order[a.status] - order[b.status])
    updatedAt.value = new Date()
  } finally {
    loading.value = false
  }
}

const counts = computed(() => {
  const c: Record<Status, number> = {
    online: 0,
    delayed: 0,
    offline: 0,
    unknown: 0,
  }
  rows.value.forEach((r) => c[r.status]++)
  return c
})

function ago(row: PhoneRow): string {
  if (!row.last) return 'Never'
  return formatDistanceToNow(new Date(row.last.timestamp), { addSuffix: true })
}

function displayName(phone: EntitiesPhone): string {
  return phone.name || formatPhoneNumber(phone.phone_number)
}

onMounted(() => {
  load(true)
  timer = setInterval(() => load(true), REFRESH_MS)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <VContainer class="py-6">
    <div class="d-flex flex-wrap align-center justify-space-between mb-6 ga-3">
      <div>
        <h1 class="text-headline-large">Phone status</h1>
        <p class="text-body-medium text-medium-emphasis">
          Each gateway phone reports a heartbeat while the app is running.
          <span v-if="updatedAt">
            Updated {{ updatedAt.toLocaleTimeString() }} · refreshes every
            minute.
          </span>
        </p>
      </div>
      <VBtn
        variant="tonal"
        color="primary"
        :loading="loading"
        :prepend-icon="mdiRefresh"
        @click="load(true)"
      >
        Refresh
      </VBtn>
    </div>

    <VRow class="mb-2">
      <VCol
        v-for="key in ['online', 'delayed', 'offline', 'unknown'] as Status[]"
        :key="key"
        cols="6"
        md="3"
      >
        <VCard class="sg-card pa-4 text-center">
          <div class="text-headline-medium font-weight-bold">
            {{ counts[key] }}
          </div>
          <div class="text-body-small text-medium-emphasis">
            {{ statusMeta[key].label }}
          </div>
        </VCard>
      </VCol>
    </VRow>

    <VRow>
      <VCol v-if="!loading && rows.length === 0" cols="12">
        <VCard class="sg-card pa-6 text-center">
          <p class="text-body-large">No gateway phones yet.</p>
          <p class="text-body-medium text-medium-emphasis">
            Connect a phone from the Android app to see it here.
          </p>
        </VCard>
      </VCol>

      <VCol v-for="row in rows" :key="row.phone.id" cols="12" md="6" lg="4">
        <VCard class="sg-card pa-4 h-100">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="text-title-large font-weight-bold text-truncate">
              {{ displayName(row.phone) }}
            </div>
            <VChip
              size="small"
              variant="flat"
              :color="statusMeta[row.status].color"
            >
              {{ statusMeta[row.status].label }}
            </VChip>
          </div>
          <div class="text-body-medium text-medium-emphasis mb-3">
            {{ formatPhoneNumber(row.phone.phone_number) }}
          </div>
          <div class="text-body-medium">
            Last heartbeat: <strong>{{ ago(row) }}</strong>
          </div>
          <div
            v-if="row.status === 'offline' || row.status === 'unknown'"
            class="text-body-small text-error mt-2"
          >
            Check that the app is running on this phone, has Autostart on and
            battery set to No restrictions.
          </div>
          <VBtn
            class="mt-3"
            variant="text"
            color="primary"
            :to="`/heartbeats/${row.phone.id}`"
          >
            View heartbeat history
          </VBtn>
        </VCard>
      </VCol>
    </VRow>
  </VContainer>
</template>

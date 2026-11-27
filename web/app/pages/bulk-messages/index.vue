<script setup lang="ts">
import {
  mdiAccountGroupOutline,
  mdiAlertCircleOutline,
  mdiArrowLeft,
  mdiMicrosoftExcel,
  mdiOpenInNew,
  mdiSendCheck,
  mdiTextBoxOutline,
} from '@mdi/js'
import { parsePhoneNumberFromString } from 'libphonenumber-js'
import type { CountryCode } from 'libphonenumber-js'
import type {
  EntitiesBulkMessage,
  EntitiesContact,
  EntitiesPhone,
} from '~~/shared/types/api'
import { ErrorMessages } from '~/utils/errors'
import { getApiErrorMessage, toApiError } from '~/utils/api-error'
import capitalize from '~/utils/capitalize'

definePageMeta({
  middleware: ['auth'],
})

useHead({
  title: 'Send Bulk Messages - Saubhagya SMS',
})

const BULK_SEND_CHUNK_SIZE = 1000

const router = useRouter()
const { mdAndUp } = useVDisplay()
const authStore = useAuthStore()
const notificationsStore = useNotificationsStore()
const phonesStore = usePhonesStore()
const contactsStore = useContactsStore()
const { formatTimestamp } = useFilters()
const { useApi } = useApiComposable()

const tab = ref<'compose' | 'upload'>('compose')
const formFile = ref<File | null>(null)
const loading = ref(true)
const loadingHistory = ref(true)
const errorTitle = ref('')
const errorMessages = ref(new ErrorMessages())
const bulkOrders = ref<EntitiesBulkMessage[]>([])

const composeFrom = ref('')
const selectedGroups = ref<string[]>([])
const typedNumbers = ref('')
const composeContent = ref('')
const groupPhoneNumbers = ref<string[]>([])
const resolvingGroups = ref(false)
const sendingCompose = ref(false)
const sendProgress = ref(0)
const confirmSendDialog = ref(false)
const lastRequestId = ref('')

let resolveGroupsGeneration = 0

const phoneItems = computed(() =>
  phonesStore.phones.map((phone: EntitiesPhone) => ({
    title: phonesStore.phoneLabel(phone),
    value: phone.phone_number,
  })),
)

const selectedPhone = computed<EntitiesPhone | null>(
  () =>
    phonesStore.phones.find(
      (phone) => phone.phone_number === composeFrom.value,
    ) ?? null,
)

const fromLabel = computed(() =>
  selectedPhone.value
    ? phonesStore.phoneLabel(selectedPhone.value)
    : composeFrom.value,
)

const groupItems = computed(() =>
  contactsStore.groups.map((group) => ({
    title: `${group.name} (${group.count})`,
    value: group.name,
  })),
)

const defaultRegion = computed<CountryCode | undefined>(() => {
  if (!composeFrom.value) return undefined
  return parsePhoneNumberFromString(composeFrom.value)?.country
})

const recipients = computed(() => {
  const candidates = [
    ...groupPhoneNumbers.value,
    ...splitTypedNumbers(typedNumbers.value),
  ]
  const valid: string[] = []
  const invalid: string[] = []
  const seen = new Set<string>()

  for (const candidate of candidates) {
    const normalized = normalizeRecipient(candidate)
    if (!normalized) {
      if (!composeFrom.value && isLocalNumber(candidate)) continue
      if (!invalid.includes(candidate)) invalid.push(candidate)
      continue
    }
    if (seen.has(normalized)) continue
    seen.add(normalized)
    valid.push(normalized)
  }

  return { valid, invalid }
})

const smsInfo = computed(() => {
  const text = composeContent.value
  const gsm7 = [...text].every(
    (character) =>
      GSM7_BASIC_CHARACTERS.has(character) ||
      GSM7_EXTENSION_CHARACTERS.has(character),
  )
  const length = gsm7
    ? [...text].reduce(
        (total, character) =>
          total + (GSM7_EXTENSION_CHARACTERS.has(character) ? 2 : 1),
        0,
      )
    : text.length
  const singlePartLimit = gsm7 ? 160 : 70
  const multipartLimit = gsm7 ? 153 : 67
  const parts =
    length === 0
      ? 0
      : length <= singlePartLimit
        ? 1
        : Math.ceil(length / multipartLimit)

  return { length, parts, encoding: gsm7 ? 'GSM-7' : 'UCS-2' }
})

const canSend = computed(
  () =>
    composeFrom.value !== '' &&
    recipients.value.valid.length > 0 &&
    composeContent.value.trim().length > 0 &&
    !sendingCompose.value &&
    !resolvingGroups.value,
)

// GSM-7 basic and extension character sets used to estimate SMS segments.
const GSM7_BASIC_CHARACTERS = new Set(
  '@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !"#¤%&\'()*+,-./0123456789:;<=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà',
)
const GSM7_EXTENSION_CHARACTERS = new Set('^{}\\[~]|€')

function splitTypedNumbers(value: string): string[] {
  return value
    .split(/[\s,;]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

function normalizeRecipient(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null

  const parsed = parsePhoneNumberFromString(trimmed, defaultRegion.value)
  if (!parsed?.isValid()) return null
  return parsed.number
}

function isLocalNumber(value: string): boolean {
  return !value.trim().startsWith('+')
}

function pluralize(count: number, singular: string, plural: string): string {
  return `${count} ${count === 1 ? singular : plural}`
}

watch(
  [() => phonesStore.owner, () => phonesStore.phones],
  () => {
    if (composeFrom.value) return
    composeFrom.value =
      phonesStore.owner ?? phonesStore.phones[0]?.phone_number ?? ''
  },
  { immediate: true },
)

async function resolveGroupRecipients() {
  const groups = [...selectedGroups.value]
  const generation = ++resolveGroupsGeneration

  if (groups.length === 0) {
    groupPhoneNumbers.value = []
    resolvingGroups.value = false
    return
  }

  resolvingGroups.value = true
  const numbers = new Set<string>()

  try {
    const api = useApi()
    for (const group of groups) {
      let skip = 0
      for (;;) {
        const response = await api<{ data: EntitiesContact[] }>(
          '/v1/contacts',
          {
            params: {
              group,
              skip,
              limit: 100,
            },
          },
        )
        const page = response.data ?? []
        for (const contact of page) {
          for (const number of contact.phone_numbers ?? []) {
            numbers.add(number)
          }
        }
        if (page.length < 100) break
        skip += page.length
      }
    }
    if (generation === resolveGroupsGeneration) {
      groupPhoneNumbers.value = [...numbers]
    }
  } catch (error: unknown) {
    if (generation === resolveGroupsGeneration) {
      groupPhoneNumbers.value = []
      notificationsStore.addNotification({
        message: getApiErrorMessage(
          error,
          'Error while loading the contacts of the selected groups',
        ),
        type: 'error',
      })
    }
  } finally {
    if (generation === resolveGroupsGeneration) {
      resolvingGroups.value = false
    }
  }
}

watch(
  selectedGroups,
  () => {
    void resolveGroupRecipients()
  },
  { deep: true },
)

function openConfirmSend() {
  if (!canSend.value) return
  confirmSendDialog.value = true
}

async function sendCompose() {
  const recipientsToSend = [...recipients.value.valid]
  if (
    !composeFrom.value ||
    recipientsToSend.length === 0 ||
    composeContent.value.trim() === ''
  ) {
    return
  }

  const api = useApi()
  const requestId = `bulk-compose-${Date.now()}`
  const chunks: string[][] = []
  for (
    let index = 0;
    index < recipientsToSend.length;
    index += BULK_SEND_CHUNK_SIZE
  ) {
    chunks.push(recipientsToSend.slice(index, index + BULK_SEND_CHUNK_SIZE))
  }

  sendingCompose.value = true
  sendProgress.value = 0
  lastRequestId.value = ''

  let sent = 0
  try {
    for (let index = 0; index < chunks.length; index++) {
      await api('/v1/messages/bulk-send', {
        method: 'POST',
        body: {
          from: composeFrom.value,
          to: chunks[index],
          content: composeContent.value,
          request_id: requestId,
        },
      })
      sent += chunks[index]?.length ?? 0
      sendProgress.value = Math.round(((index + 1) / chunks.length) * 100)
    }

    lastRequestId.value = requestId
    confirmSendDialog.value = false
    notificationsStore.addNotification({
      message: `${sent} messages added to the queue`,
      type: 'success',
    })
  } catch (error: unknown) {
    notificationsStore.addNotification({
      message: getApiErrorMessage(
        error,
        `Error while sending bulk messages. ${sent} of ${recipientsToSend.length} were queued.`,
      ),
      type: 'error',
    })
  } finally {
    sendingCompose.value = false
  }
}

function parseErrors(error: unknown): ErrorMessages {
  const bag = new ErrorMessages()
  const data = toApiError(error).data?.data
  if (data && typeof data === 'object') {
    Object.entries(data).forEach(([key, values]) => bag.addMany(key, values))
  }
  return bag
}

function cleanName(requestId: string): string {
  if (requestId.startsWith('bulk-csv-'))
    return requestId.replace(/^bulk-csv-/, '') + '.csv'
  if (requestId.startsWith('bulk-xls-'))
    return requestId.replace(/^bulk-xls-/, '') + '.xlsx'
  if (requestId.startsWith('bulk-compose-')) return 'Composed message'
  const newFormatMatch = requestId.match(/^bulk-[0-9A-Za-z]+-(.+)$/)
  if (newFormatMatch?.[1]) return newFormatMatch[1]
  return requestId.replace(/^bulk-/, '')
}

async function fetchBulkOrders() {
  loadingHistory.value = true
  try {
    const api = useApi()
    const response = await api<{ data: EntitiesBulkMessage[] }>(
      '/v1/bulk-messages',
      {
        method: 'GET',
      },
    )
    bulkOrders.value = response.data ?? []
  } catch {
    notificationsStore.addNotification({
      message: 'Error while fetching bulk messages history',
      type: 'error',
    })
  } finally {
    loadingHistory.value = false
  }
}

async function sendBulkMessages() {
  loading.value = true
  errorMessages.value = new ErrorMessages()
  errorTitle.value = ''

  try {
    const api = useApi()
    const formData = new FormData()
    if (formFile.value) formData.append('document', formFile.value)
    const response = await api<{ message?: string }>('/v1/bulk-messages', {
      method: 'POST',
      body: formData,
    })
    notificationsStore.addNotification({
      message: response?.message ?? 'Bulk messages sent successfully',
      type: 'success',
    })
    loading.value = false
    formFile.value = null
    fetchBulkOrders()
  } catch (error: unknown) {
    errorTitle.value = capitalize(
      toApiError(error).data?.message ?? 'Error while sending bulk messages',
    )
    errorMessages.value = parseErrors(error)
    notificationsStore.addNotification({
      message:
        toApiError(error).data?.message ?? 'Errors while sending bulk messages',
      type: 'error',
    })
    loading.value = false
  }
}

onMounted(async () => {
  await authStore.loadUser()
  await Promise.allSettled([
    phonesStore.loadPhones(),
    contactsStore.loadGroups(),
  ])
  loading.value = false
  fetchBulkOrders()
})
</script>

<template>
  <VContainer fluid class="px-0 pt-0" :class="{ 'fill-height': true }">
    <div class="w-100 h-100">
      <VAppBar>
        <VBtn icon to="/threads">
          <VIcon :icon="mdiArrowLeft" />
        </VBtn>
        <VToolbarTitle>
          <div class="py-16">Bulk Messages</div>
        </VToolbarTitle>
        <VProgressLinear
          :active="loading"
          color="primary"
          :indeterminate="loading"
          location="bottom"
          absolute
        />
      </VAppBar>
      <VContainer>
        <VRow>
          <VCol cols="12" md="10" offset-md="1" xxl="8" offset-xxl="2">
            <h5 class="text-headline-large mb-3 mt-3">Bulk Messages</h5>
            <p class="text-medium-emphasis">
              Compose a message for contact groups or typed numbers, or upload
              our bulk SMS
              <a
                class="text-decoration-none hover:text-decoration-underline"
                download
                href="/templates/httpsms-bulk.csv"
                >CSV template</a
              >
              or
              <a
                class="text-decoration-none hover:text-decoration-underline"
                download
                href="/templates/httpsms-bulk.xlsx"
                >Excel template</a
              >. You can also configure
              <NuxtLink
                class="text-decoration-none hover:text-decoration-underline"
                to="/settings/#send-schedules"
                >send schedules</NuxtLink
              >
              on your phone to make sure messages are sent out at specific times
              of the day e.g
              <span class="text-medium-emphasis">Mon - Fri 9am - 5pm.</span>
            </p>

            <VTabs v-model="tab" color="primary" class="mb-6">
              <VTab value="compose" :prepend-icon="mdiTextBoxOutline">
                Compose
              </VTab>
              <VTab value="upload" :prepend-icon="mdiMicrosoftExcel">
                Upload file
              </VTab>
            </VTabs>

            <VWindow v-model="tab">
              <VWindowItem value="compose">
                <VCard class="sg-card pa-2 pa-sm-4" variant="flat">
                  <VCardText>
                    <VRow density="comfortable">
                      <VCol cols="12" md="6">
                        <VSelect
                          v-model="composeFrom"
                          :items="phoneItems"
                          label="From phone"
                          color="primary"
                          variant="outlined"
                          density="comfortable"
                          hide-details
                        />
                      </VCol>
                      <VCol cols="12" md="6">
                        <VSelect
                          v-model="selectedGroups"
                          :items="groupItems"
                          label="Contact groups"
                          color="primary"
                          variant="outlined"
                          density="comfortable"
                          multiple
                          chips
                          closable-chips
                          hide-details
                          no-data-text="No contact groups yet. Add groups to your contacts first."
                        />
                      </VCol>
                    </VRow>

                    <VTextarea
                      v-model="typedNumbers"
                      label="Or type numbers"
                      placeholder="Commas, spaces or new lines: +18005550199, +18005550100"
                      color="primary"
                      variant="outlined"
                      density="comfortable"
                      rows="2"
                      auto-grow
                      max-rows="6"
                      hide-details
                      class="mt-4"
                    />
                    <p
                      v-if="!composeFrom"
                      class="text-caption text-medium-emphasis mt-1 mb-0"
                    >
                      Pick a From phone to validate local numbers
                    </p>

                    <div class="d-flex flex-wrap align-center ga-2 mt-4">
                      <VChip
                        :color="
                          recipients.invalid.length ? 'warning' : 'primary'
                        "
                        variant="tonal"
                        :prepend-icon="
                          recipients.invalid.length
                            ? mdiAlertCircleOutline
                            : mdiAccountGroupOutline
                        "
                      >
                        {{
                          pluralize(
                            recipients.valid.length,
                            'recipient',
                            'recipients',
                          )
                        }}<span v-if="recipients.invalid.length">
                          ·
                          {{
                            pluralize(
                              recipients.invalid.length,
                              'invalid number',
                              'invalid numbers',
                            )
                          }}
                          skipped</span
                        >
                      </VChip>
                      <VProgressCircular
                        v-if="resolvingGroups"
                        indeterminate
                        color="primary"
                        size="20"
                      />
                      <span
                        v-if="resolvingGroups"
                        class="text-caption text-medium-emphasis"
                      >
                        Loading group members…
                      </span>
                    </div>

                    <VExpansionPanels
                      v-if="recipients.invalid.length"
                      variant="accordion"
                      class="mt-3"
                    >
                      <VExpansionPanel
                        :title="`${pluralize(recipients.invalid.length, 'invalid number', 'invalid numbers')} skipped`"
                      >
                        <VExpansionPanelText>
                          <div class="d-flex flex-wrap ga-2">
                            <VChip
                              v-for="number in recipients.invalid"
                              :key="number"
                              size="small"
                              variant="outlined"
                              color="warning"
                            >
                              {{ number }}
                            </VChip>
                          </div>
                        </VExpansionPanelText>
                      </VExpansionPanel>
                    </VExpansionPanels>

                    <VTextarea
                      v-model="composeContent"
                      label="Message"
                      color="primary"
                      variant="outlined"
                      density="comfortable"
                      rows="4"
                      auto-grow
                      max-rows="10"
                      hide-details
                      class="mt-4"
                    />
                    <div
                      class="d-flex flex-wrap justify-space-between ga-2 mt-1 text-caption text-medium-emphasis"
                    >
                      <span>{{ smsInfo.length }} characters</span>
                      <span>
                        {{ smsInfo.parts }} SMS
                        {{ smsInfo.parts === 1 ? 'part' : 'parts' }} ·
                        {{ smsInfo.encoding }}
                      </span>
                    </div>

                    <div class="d-flex flex-wrap align-center ga-2 mt-5">
                      <VBtn
                        color="primary"
                        size="large"
                        :prepend-icon="mdiSendCheck"
                        :disabled="!canSend"
                        @click="openConfirmSend"
                      >
                        Send Messages
                      </VBtn>
                      <VProgressLinear
                        v-if="sendingCompose"
                        :model-value="sendProgress"
                        color="primary"
                        height="8"
                        rounded
                        class="flex-grow-1"
                        style="min-width: 160px"
                      />
                      <VBtn
                        v-if="lastRequestId"
                        variant="text"
                        color="primary"
                        :append-icon="mdiOpenInNew"
                        :to="{
                          path: '/search-messages',
                          query: { query: lastRequestId },
                        }"
                      >
                        View in Search
                      </VBtn>
                      <VSpacer />
                      <VBtn
                        v-if="mdAndUp"
                        variant="plain"
                        color="info"
                        href="mailto:arnold@httpsms.com?subject=I'm having trouble with the bulk messages"
                      >
                        I Need Help
                      </VBtn>
                    </div>
                  </VCardText>
                </VCard>
              </VWindowItem>

              <VWindowItem value="upload">
                <VCard class="sg-card pa-2 pa-sm-4" variant="flat">
                  <VCardText>
                    <VAlert
                      v-if="errorTitle"
                      variant="tonal"
                      type="warning"
                      prominent
                    >
                      <h6 class="text-title-large font-weight-bold">
                        {{ errorTitle }}
                      </h6>
                      <ul class="text-body-medium">
                        <li
                          v-for="message in errorMessages.get('document')"
                          :key="message"
                        >
                          {{ message }}
                        </li>
                      </ul>
                    </VAlert>
                    <form>
                      <VFileInput
                        v-model="formFile"
                        label="File"
                        color="primary"
                        accept=".csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        :error-messages="errorMessages.get('document')"
                        :append-inner-icon="mdiMicrosoftExcel"
                        variant="outlined"
                      />
                      <div class="d-flex">
                        <loading-button
                          color="primary"
                          type="submit"
                          size="large"
                          :loading="loading"
                          :disabled="loading"
                          :icon="mdiSendCheck"
                          @click="sendBulkMessages"
                        >
                          Send Bulk Messages
                        </loading-button>
                        <VSpacer />
                        <VBtn
                          v-if="mdAndUp"
                          variant="plain"
                          color="info"
                          href="mailto:arnold@httpsms.com?subject=I'm having trouble with the bulk messages"
                        >
                          I Need Help
                        </VBtn>
                      </div>
                    </form>
                  </VCardText>
                </VCard>
              </VWindowItem>
            </VWindow>
          </VCol>
        </VRow>
        <VRow class="mt-8">
          <VCol cols="12" md="10" offset-md="1" xxl="8" offset-xxl="2">
            <h4 class="text-headline-large mb-3">Bulk Message History</h4>
            <p class="text-medium-emphasis">
              Your 10 most recent bulk SMS uploads are shown below, including a
              delivery status breakdown for each batch. Click on a row to see
              individual messages on the search page.
            </p>
            <VProgressLinear
              v-if="loadingHistory"
              color="primary"
              indeterminate
              class="mb-4"
            />
            <VTable v-else density="comfortable">
              <thead>
                <tr class="text-uppercase text-medium-emphasis">
                  <th class="text-left">Name</th>
                  <th class="text-center">Created At</th>
                  <th class="text-center">Total</th>
                  <th class="text-center">Pending</th>
                  <th class="text-center">Scheduled</th>
                  <th class="text-center">Sent</th>
                  <th class="text-center">Delivered</th>
                  <th class="text-center">Failed</th>
                  <th class="text-center">Expired</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="order in bulkOrders"
                  :key="order.request_id"
                  class="clickable-row"
                  @click="
                    router.push(`/search-messages?query=${order.request_id}`)
                  "
                >
                  <td class="text-left">{{ cleanName(order.request_id) }}</td>
                  <td class="text-center">
                    {{ formatTimestamp(order.created_at) }}
                  </td>
                  <td class="text-center">{{ order.total }}</td>
                  <td class="text-center">{{ order.pending_count }}</td>
                  <td class="text-center">{{ order.scheduled_count }}</td>
                  <td class="text-center">{{ order.sent_count }}</td>
                  <td class="text-center">{{ order.delivered_count }}</td>
                  <td class="text-center">{{ order.failed_count }}</td>
                  <td class="text-center">{{ order.expired_count }}</td>
                </tr>
              </tbody>
            </VTable>
          </VCol>
        </VRow>
      </VContainer>
    </div>

    <VDialog v-model="confirmSendDialog" max-width="480" opacity="0.9">
      <VCard>
        <VCardTitle>Send bulk messages?</VCardTitle>
        <VCardText class="text-medium-emphasis">
          Send to
          <strong>{{ recipients.valid.length }} recipients</strong>
          from
          <strong>{{ fromLabel }}</strong
          >? Messages are queued on your phone and may take a while to send.
        </VCardText>
        <VCardActions class="pb-4">
          <VBtn
            color="primary"
            variant="flat"
            :prepend-icon="mdiSendCheck"
            :loading="sendingCompose"
            :disabled="sendingCompose"
            @click="sendCompose"
          >
            Send Messages
          </VBtn>
          <VSpacer />
          <VBtn
            color="warning"
            variant="text"
            :disabled="sendingCompose"
            @click="confirmSendDialog = false"
          >
            Cancel
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </VContainer>
</template>

<style scoped>
.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: rgb(0 0 0 / 4%);
}
</style>

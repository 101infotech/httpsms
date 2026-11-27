<script setup lang="ts">
import {
  mdiAccountGroupOutline,
  mdiAccountPlus,
  mdiAlertCircleOutline,
  mdiArrowLeft,
  mdiClose,
  mdiDelete,
  mdiDotsVertical,
  mdiFileUpload,
  mdiMagnify,
  mdiPencilOutline,
  mdiPhoneCheck,
  mdiEmailCheckOutline,
  mdiSquareEditOutline,
  mdiTagOutline,
  mdiTrashCanOutline,
} from '@mdi/js'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { EntitiesContact, EntitiesContactGroup } from '~~/shared/types/api'
import { useContactsStore } from '~/stores/contacts'
import { useFilters } from '~/composables/useFilters'
import { toApiError } from '~/utils/api-error'

definePageMeta({
  middleware: ['auth'],
})

useHead({
  title: 'Contacts - Saubhagya SMS',
})

const contactsStore = useContactsStore()
const { formatPhoneNumber, formatTimestamp, humanizeTimeShort } = useFilters()

const headers = [
  { title: 'Name', key: 'name', sortable: true },
  { title: 'Phone Numbers', key: 'phone_numbers', sortable: false },
  { title: 'Emails', key: 'emails', sortable: false },
  { title: 'Created', key: 'created_at', sortable: false },
  { title: 'Updated', key: 'updated_at', sortable: true },
  { title: 'Actions', key: 'actions', sortable: false, align: 'end' as const },
]

const itemsPerPageOptions = [
  { value: 10, title: '10' },
  { value: 25, title: '25' },
  { value: 50, title: '50' },
  { value: 100, title: '100' },
]

const contactDialog = ref(false)
const deleteDialog = ref(false)
const importDialog = ref(false)
const saving = ref(false)

// Server-driven pagination state for VDataTableServer.
const page = ref(1)
const itemsPerPage = ref(10)
const sortBy = ref<{ key: string; order: 'asc' | 'desc' }[]>([
  { key: 'updated_at', order: 'desc' },
])
// initialLoadComplete gates the table's initial @update:options emit so the
// first fetch is driven by onMounted rather than firing twice on mount.
const initialLoadComplete = ref(false)

const editingContact = ref<EntitiesContact | null>(null)
const pendingDelete = ref<EntitiesContact | null>(null)

const groupRenameDialog = ref(false)
const groupDeleteDialog = ref(false)
const groupRenameTarget = ref<EntitiesContactGroup | null>(null)
const groupPendingDelete = ref<EntitiesContactGroup | null>(null)
const groupName = ref('')

const importFile = ref<File | null>(null)
const importErrors = ref<string[]>([])

let searchTimer: ReturnType<typeof setTimeout> | undefined

const searchTerm = computed({
  get: () => contactsStore.search,
  set: (value: string | null) => {
    contactsStore.search = value ?? ''
  },
})

function openAdd() {
  editingContact.value = null
  contactDialog.value = true
}

function openEdit(contact: EntitiesContact) {
  editingContact.value = contact
  contactDialog.value = true
}

function openDelete(contact: EntitiesContact) {
  pendingDelete.value = contact
  deleteDialog.value = true
}

function openImport() {
  importFile.value = null
  importErrors.value = []
  importDialog.value = true
}

function selectGroup(name: string | null) {
  if (contactsStore.group === name) {
    return
  }
  contactsStore.group = name
  // A new group filter always resets to the first page. If we are already on
  // page 1 the page ref does not change, so fetch directly; otherwise the
  // reset drives the fetch via the table's update:options handler.
  if (page.value !== 1) {
    page.value = 1
  } else {
    fetchContacts()
  }
}

function openRenameGroup(group: EntitiesContactGroup) {
  groupRenameTarget.value = group
  groupName.value = group.name
  groupRenameDialog.value = true
}

async function confirmRenameGroup() {
  const target = groupRenameTarget.value
  const name = groupName.value.trim()
  if (!target || !name) {
    return
  }
  saving.value = true
  try {
    await contactsStore.renameGroup(target.name, name)
    groupRenameDialog.value = false
    groupRenameTarget.value = null
  } catch {
    // The store already surfaced the failure via a notification.
  } finally {
    saving.value = false
  }
}

function openDeleteGroup(group: EntitiesContactGroup) {
  groupPendingDelete.value = group
  groupDeleteDialog.value = true
}

async function confirmDeleteGroup() {
  const target = groupPendingDelete.value
  if (!target) {
    return
  }
  saving.value = true
  try {
    await contactsStore.deleteGroup(target.name)
    groupDeleteDialog.value = false
    groupPendingDelete.value = null
  } catch {
    // The store already surfaced the failure via a notification.
  } finally {
    saving.value = false
  }
}

function fieldErrorsFromApi(error: unknown): string[] {
  const data = toApiError(error).data?.data
  if (!data || typeof data !== 'object') {
    return []
  }
  return Object.values(data).flat()
}

async function confirmDelete() {
  if (!pendingDelete.value?.id) {
    return
  }
  saving.value = true
  try {
    await contactsStore.deleteContact(pendingDelete.value.id)
    deleteDialog.value = false
    pendingDelete.value = null
  } catch {
    // The store already surfaced the failure via a notification.
  } finally {
    saving.value = false
  }
}

async function submitImport() {
  if (!importFile.value) {
    return
  }
  importErrors.value = []
  saving.value = true
  try {
    await contactsStore.uploadCsv(importFile.value)
    importDialog.value = false
    importFile.value = null
  } catch (error: unknown) {
    // The store already surfaced a toast; keep the row-indexed messages inline
    // so the user can see exactly which CSV rows failed validation.
    importErrors.value = fieldErrorsFromApi(error)
  } finally {
    saving.value = false
  }
}

function fetchContacts() {
  const skip = (page.value - 1) * itemsPerPage.value
  const sort = sortBy.value[0]
  return contactsStore
    .loadContacts({
      force: true,
      skip,
      limit: itemsPerPage.value,
      sortBy: sort?.key === 'name' ? 'name' : 'updated_at',
      sortDescending: sort ? sort.order === 'desc' : true,
      group: contactsStore.group,
    })
    .catch(() => {
      // The store already surfaced the failure via a notification.
    })
}

function onUpdateOptions(options: {
  page: number
  itemsPerPage: number
  sortBy: { key: string; order: 'asc' | 'desc' }[]
}) {
  page.value = options.page
  itemsPerPage.value = options.itemsPerPage
  sortBy.value = options.sortBy.length
    ? options.sortBy
    : [{ key: 'updated_at', order: 'desc' }]

  // Ignore the initial emit fired while the table mounts; onMounted owns the
  // first fetch so the request is not duplicated.
  if (!initialLoadComplete.value) {
    return
  }
  fetchContacts()
}

watch(
  () => contactsStore.search,
  () => {
    if (searchTimer) {
      clearTimeout(searchTimer)
    }
    searchTimer = setTimeout(() => {
      // A new query always resets to the first page. If we are already on
      // page 1 the page ref does not change, so no @update:options fires and
      // we fetch directly; otherwise resetting the page drives the fetch via
      // onUpdateOptions. Either way exactly one request is made.
      if (page.value !== 1) {
        page.value = 1
      } else {
        fetchContacts()
      }
    }, 350)
  },
)

onMounted(() => {
  initialLoadComplete.value = true
  void contactsStore.loadGroups().catch(() => {
    // The contact list still works when groups cannot be loaded.
  })
  fetchContacts()
})

onBeforeUnmount(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
})
</script>

<template>
  <VContainer fluid class="px-0 pt-0">
    <VAppBar>
      <VBtn icon to="/threads" aria-label="Back to messages">
        <VIcon :icon="mdiArrowLeft" />
      </VBtn>
      <VToolbarTitle>Contacts</VToolbarTitle>
      <VProgressLinear
        :active="contactsStore.loading"
        :indeterminate="contactsStore.loading"
        color="primary"
        location="bottom"
        absolute
      />
    </VAppBar>

    <VContainer class="pt-0">
      <VRow>
        <VCol cols="12">
          <div class="d-flex align-center">
            <h1 class="text-display-large mb-1">Contacts</h1>
            <VSpacer />
            <div class="mt-12">
              <VBtn
                variant="tonal"
                class="ml-4 mb-4"
                :prepend-icon="mdiFileUpload"
                @click="openImport"
              >
                Import CSV
              </VBtn>
              <VBtn
                class="ml-4 mb-4"
                color="primary"
                variant="flat"
                :prepend-icon="mdiAccountPlus"
                @click="openAdd"
              >
                Add Contact
              </VBtn>
            </div>
          </div>
          <p class="text-medium-emphasis mb-6">
            Use Saubhagya SMS as a lightweight CRM by adding your contacts here.
            Your message threads will show contact names instead of phone
            numbers, making conversations easier to recognize and manage. Add
            contacts individually, or fill in our
            <a
              class="text-decoration-none hover:text-decoration-underline"
              href="/templates/httpsms-contacts.csv"
              download
              >CSV template</a
            >
            and upload it to import your contact list in bulk.
          </p>

          <VTextField
            v-model="searchTerm"
            :prepend-inner-icon="mdiMagnify"
            label="Search by name, phone number or email"
            variant="outlined"
            density="compact"
            autocomplete="search-query"
            clearable
            color="primary"
            hide-details
            class="mb-4"
          />

          <div
            v-if="contactsStore.groups.length"
            class="d-flex flex-wrap align-center mb-4"
            style="gap: 8px"
          >
            <div class="d-flex align-center text-medium-emphasis mr-1">
              <VIcon :icon="mdiTagOutline" size="small" class="mr-1" />
              <span class="text-label-large">Groups</span>
            </div>
            <VBtn
              class="sg-chip"
              :class="{ 'sg-chip--on': contactsStore.group === null }"
              rounded="pill"
              variant="flat"
              @click="selectGroup(null)"
            >
              All
            </VBtn>
            <div
              v-for="group in contactsStore.groups"
              :key="group.name"
              class="d-flex align-center"
            >
              <VBtn
                class="sg-chip"
                :class="{ 'sg-chip--on': contactsStore.group === group.name }"
                rounded="pill"
                variant="flat"
                @click="selectGroup(group.name)"
              >
                {{ group.name }}
                <span class="sg-group-count">{{ group.count }}</span>
              </VBtn>
              <VMenu location="bottom end">
                <template #activator="{ props }">
                  <VBtn
                    v-bind="props"
                    :icon="mdiDotsVertical"
                    variant="text"
                    size="x-small"
                    density="comfortable"
                    :aria-label="`Actions for group ${group.name}`"
                  />
                </template>
                <VList density="compact">
                  <VListItem
                    :prepend-icon="mdiPencilOutline"
                    title="Rename group"
                    @click="openRenameGroup(group)"
                  />
                  <VListItem
                    :prepend-icon="mdiTrashCanOutline"
                    title="Delete group"
                    @click="openDeleteGroup(group)"
                  />
                </VList>
              </VMenu>
            </div>
          </div>

          <VDataTableServer
            v-model:page="page"
            v-model:items-per-page="itemsPerPage"
            v-model:sort-by="sortBy"
            :headers="headers"
            :header-props="{
              class: 'text-uppercase text-medium-emphasis',
            }"
            :items="contactsStore.contacts"
            :items-length="contactsStore.total"
            :loading="contactsStore.loading"
            :items-per-page-options="itemsPerPageOptions"
            item-value="id"
            hover
            loading-text="Loading contacts…"
            @update:options="onUpdateOptions"
          >
            <template #[`item.name`]="{ item }">
              <div class="d-flex align-center py-2">
                <span class="font-weight-medium">{{ item.name }}</span>
              </div>
            </template>

            <template #[`item.phone_numbers`]="{ item }">
              <div
                v-if="item.phone_numbers?.length"
                class="d-flex flex-column ga-1 py-2"
              >
                <div
                  v-for="phone in item.phone_numbers ?? []"
                  :key="phone"
                  class="d-flex"
                >
                  <VIcon
                    :icon="mdiPhoneCheck"
                    size="small"
                    color="info"
                    class="mr-1 text-medium-emphasis"
                  />
                  {{ formatPhoneNumber(phone) }}
                </div>
              </div>
              <span v-else class="text-medium-emphasis">—</span>
            </template>

            <template #[`item.emails`]="{ item }">
              <div
                v-if="item.emails?.length"
                class="d-flex flex-column ga-1 py-2"
              >
                <span
                  v-for="email in item.emails ?? []"
                  :key="email"
                  class="d-flex align-center text-body-2"
                >
                  <VIcon
                    :icon="mdiEmailCheckOutline"
                    size="small"
                    color="info"
                    class="mr-1 text-medium-emphasis"
                  />
                  {{ email }}
                </span>
              </div>
              <span v-else class="text-medium-emphasis">—</span>
            </template>

            <template #[`item.created_at`]="{ item }">
              <VTooltip :text="formatTimestamp(item.created_at)">
                <template #activator="{ props }">
                  <span v-bind="props" class="text-medium-emphasis">
                    {{ humanizeTimeShort(item.created_at) }}
                  </span>
                </template>
              </VTooltip>
            </template>

            <template #[`item.updated_at`]="{ item }">
              <VTooltip :text="formatTimestamp(item.updated_at)">
                <template #activator="{ props }">
                  <span v-bind="props" class="text-medium-emphasis">
                    {{ humanizeTimeShort(item.updated_at) }}
                  </span>
                </template>
              </VTooltip>
            </template>

            <template #[`item.actions`]="{ item }">
              <div class="d-flex justify-end">
                <VBtn
                  :icon="mdiSquareEditOutline"
                  variant="text"
                  class="mr-2"
                  density="comfortable"
                  aria-label="Edit contact"
                  @click="openEdit(item)"
                />
                <VBtn
                  :icon="mdiDelete"
                  variant="text"
                  density="comfortable"
                  color="error"
                  aria-label="Delete contact"
                  @click="openDelete(item)"
                />
              </div>
            </template>

            <template #no-data>
              <div class="text-center py-12">
                <VIcon
                  :icon="mdiAccountGroupOutline"
                  size="64"
                  class="text-medium-emphasis mb-3"
                />
                <p class="text-title-medium mb-1">
                  {{
                    searchTerm
                      ? 'No contacts match your search'
                      : contactsStore.group
                        ? 'No contacts in this group'
                        : 'No contacts yet'
                  }}
                </p>
                <p class="text-medium-emphasis mb-4">
                  {{
                    searchTerm
                      ? 'Try a different name, phone number or email.'
                      : contactsStore.group
                        ? 'Assign this group to a contact or choose another group.'
                        : 'Add your first contact or import them from a CSV file.'
                  }}
                </p>
                <VBtn
                  v-if="!searchTerm && !contactsStore.group"
                  color="primary"
                  variant="flat"
                  :prepend-icon="mdiAccountPlus"
                  @click="openAdd"
                >
                  Add Contact
                </VBtn>
              </div>
            </template>
          </VDataTableServer>
        </VCol>
      </VRow>
    </VContainer>

    <ContactDialog v-model="contactDialog" :contact="editingContact" />

    <!-- Rename group dialog -->
    <VDialog v-model="groupRenameDialog" max-width="480" opacity="0.9">
      <VCard>
        <VCardTitle class="d-flex align-center">
          <span>Rename Group</span>
          <VSpacer />
          <VBtn
            :icon="mdiClose"
            variant="text"
            color="warning"
            size="small"
            aria-label="Close dialog"
            @click="groupRenameDialog = false"
          />
        </VCardTitle>
        <VCardText class="mt-n2">
          <p class="text-medium-emphasis mb-4">
            Rename
            <v-code>{{ groupRenameTarget?.name }}</v-code>
            on every contact that uses it.
          </p>
          <VTextField
            v-model="groupName"
            label="New group name"
            variant="outlined"
            density="comfortable"
            :prepend-inner-icon="mdiTagOutline"
            autofocus
            hide-details
            @keyup.enter="confirmRenameGroup"
          />
        </VCardText>
        <VCardActions class="mb-2">
          <VBtn
            color="primary"
            variant="flat"
            :prepend-icon="mdiPencilOutline"
            :loading="saving"
            :disabled="saving || !groupName.trim()"
            @click="confirmRenameGroup"
          >
            Rename Group
          </VBtn>
          <VSpacer />
          <VBtn
            color="warning"
            variant="text"
            @click="groupRenameDialog = false"
          >
            Close
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>

    <!-- Delete group dialog -->
    <VDialog v-model="groupDeleteDialog" max-width="480" opacity="0.9">
      <VCard>
        <VCardTitle class="d-flex align-center">
          <span>Delete Group</span>
          <VSpacer />
          <VBtn
            :icon="mdiClose"
            variant="text"
            color="warning"
            size="small"
            aria-label="Close dialog"
            @click="groupDeleteDialog = false"
          />
        </VCardTitle>
        <VCardText class="mt-n2 text-medium-emphasis">
          Remove the group
          <v-code>{{ groupPendingDelete?.name }}</v-code>
          from all contacts? The contacts themselves are kept.
        </VCardText>
        <VCardActions class="mb-2">
          <VBtn
            color="error"
            variant="flat"
            :prepend-icon="mdiDelete"
            :loading="saving"
            :disabled="saving"
            @click="confirmDeleteGroup"
          >
            Delete Group
          </VBtn>
          <VSpacer />
          <VBtn
            color="warning"
            variant="text"
            @click="groupDeleteDialog = false"
          >
            Close
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>

    <!-- Delete contact dialog -->
    <VDialog v-model="deleteDialog" max-width="480" opacity="0.9">
      <VCard>
        <VCardTitle class="d-flex align-center">
          <span>Delete Contact</span>
          <VSpacer />
          <VBtn
            :icon="mdiClose"
            variant="text"
            color="warning"
            size="small"
            aria-label="Close dialog"
            @click="deleteDialog = false"
          />
        </VCardTitle>
        <VCardText class="mt-n2 text-medium-emphasis">
          Are you sure you want to delete
          <v-code>{{ pendingDelete?.name }}</v-code
          >? This action cannot be undone.
        </VCardText>
        <VCardActions class="mb-2">
          <VBtn
            color="error"
            variant="flat"
            :prepend-icon="mdiDelete"
            :loading="saving"
            :disabled="saving"
            @click="confirmDelete"
          >
            Delete Contact
          </VBtn>
          <VSpacer />
          <VBtn color="warning" variant="text" @click="deleteDialog = false">
            Close
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>

    <!-- Import CSV dialog -->
    <VDialog v-model="importDialog" max-width="600" opacity="0.9">
      <VCard>
        <VCardTitle class="d-flex align-center">
          <span>Import Contacts from CSV</span>
          <VSpacer />
          <VBtn
            :icon="mdiClose"
            variant="text"
            color="warning"
            size="small"
            aria-label="Close dialog"
            @click="importDialog = false"
          />
        </VCardTitle>
        <VCardText>
          <p class="mb-4 mt-n2 text-medium-emphasis">
            Download the
            <a
              class="text-decoration-none hover:text-decoration-underline"
              href="/templates/httpsms-contacts.csv"
              download
              >CSV template</a
            >, fill it in and upload it here. Separate multiple emails or phone
            numbers within a cell using a semicolon (<code>;</code>).
          </p>
          <VFileInput
            v-model="importFile"
            label="CSV file"
            color="primary"
            accept=".csv,text/csv"
            variant="outlined"
            density="comfortable"
            :prepend-icon="mdiFileUpload"
            hide-details="auto"
          />
          <VAlert
            v-if="importErrors.length"
            type="error"
            variant="tonal"
            density="comfortable"
            class="mt-4"
            :icon="mdiAlertCircleOutline"
          >
            <p class="font-weight-medium mb-1">We couldn't import your file:</p>
            <ul class="pl-4 mb-0">
              <li v-for="message in importErrors" :key="message">
                {{ message }}
              </li>
            </ul>
          </VAlert>
        </VCardText>
        <VCardActions class="pb-4">
          <VBtn
            color="primary"
            variant="flat"
            :prepend-icon="mdiFileUpload"
            :loading="saving"
            :disabled="saving || !importFile"
            @click="submitImport"
          >
            Import
          </VBtn>
          <v-spacer />
          <VBtn color="warning" variant="text" @click="importDialog = false">
            Close
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </VContainer>
</template>

<style scoped>
.sg-group-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgb(31 26 28 / 8%);
  color: inherit;
  font-size: 11px;
  font-weight: 800;
}

/* stylelint-disable-next-line selector-class-pattern -- shared BEM modifier from saubhagya.css */
.sg-chip--on .sg-group-count {
  background: rgb(255 255 255 / 22%);
}
</style>

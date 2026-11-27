import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { EntitiesContact, EntitiesContactGroup } from '~~/shared/types/api'
import { getApiErrorMessage } from '~/utils/api-error'

export interface ContactInput {
  name: string
  emails: string[]
  phone_numbers: string[]
  groups: string[]
  properties?: Record<string, string>
}

export interface LoadContactsOptions {
  force?: boolean
  skip?: number
  limit?: number
  sortBy?: 'name' | 'updated_at'
  sortDescending?: boolean
  group?: string | null
}

export interface ContactMutationOptions {
  refresh?: boolean
}

// DEFAULT_LIMIT mirrors the contacts page's initial items-per-page. It is only
// used when a caller (e.g. a mutation refresh) does not specify its own limit.
const DEFAULT_LIMIT = 10

export const useContactsStore = defineStore('contacts', () => {
  const contacts = ref<EntitiesContact[]>([])
  const groups = ref<EntitiesContactGroup[]>([])
  const group = ref<string | null>(null)
  const total = ref(0)
  const loading = ref(false)
  const search = ref('')
  const { apiFetch } = useApi()
  const notificationsStore = useNotificationsStore()
  let loadContactsGeneration = 0

  // The pagination window last requested by the page. Mutation-triggered
  // refreshes reuse it so the user stays on the page they were viewing.
  let currentSkip = 0
  let currentLimit = DEFAULT_LIMIT
  let currentSortBy: 'name' | 'updated_at' = 'updated_at'
  let currentSortDescending = true
  let currentGroup: string | null = null

  function normalizeOptions(
    options: LoadContactsOptions | boolean,
  ): LoadContactsOptions {
    if (typeof options === 'boolean') {
      return { force: options }
    }
    return options
  }

  async function loadContacts(
    options: LoadContactsOptions | boolean = {},
  ): Promise<void> {
    const {
      force = false,
      skip,
      limit,
      sortBy,
      sortDescending,
      group: selectedGroup,
    } = normalizeOptions(options)

    if (skip !== undefined) {
      currentSkip = skip
    }
    if (limit !== undefined) {
      currentLimit = limit
    }
    if (sortBy !== undefined) {
      currentSortBy = sortBy
    }
    if (sortDescending !== undefined) {
      currentSortDescending = sortDescending
    }

    let groupChanged = false
    if (selectedGroup !== undefined) {
      const nextGroup = selectedGroup?.trim() || null
      groupChanged = nextGroup !== currentGroup
      currentGroup = nextGroup
      group.value = nextGroup
    }

    if (contacts.value.length > 0 && !force && !groupChanged) return

    const generation = ++loadContactsGeneration
    loading.value = true
    try {
      const term = search.value.trim()
      const params: Record<string, string | number | boolean> = {
        skip: currentSkip,
        limit: currentLimit,
        sort_by: currentSortBy,
        sort_descending: currentSortDescending,
      }
      if (term) {
        params.query = term
      }
      if (currentGroup) {
        params.group = currentGroup
      }
      const response = await apiFetch<{
        data: EntitiesContact[]
        total?: number
      }>('/v1/contacts', { params })
      if (generation === loadContactsGeneration) {
        contacts.value = response.data ?? []
        total.value = response.total ?? contacts.value.length
      }
    } catch (error: unknown) {
      if (generation !== loadContactsGeneration) {
        return
      }
      notificationsStore.addNotification({
        message: getApiErrorMessage(error, 'Error while loading contacts'),
        type: 'error',
      })
      throw error
    } finally {
      if (generation === loadContactsGeneration) {
        loading.value = false
      }
    }
  }

  async function saveContacts(
    items: ContactInput | ContactInput[],
    options: ContactMutationOptions = {},
  ): Promise<EntitiesContact[]> {
    const contactsToSave = Array.isArray(items) ? items : [items]
    const { refresh = true } = options

    loading.value = true
    try {
      let response: { data: EntitiesContact[] }
      try {
        response = await apiFetch<{ data: EntitiesContact[] }>('/v1/contacts', {
          method: 'POST',
          body: contactsToSave,
        })
      } catch (error: unknown) {
        notificationsStore.addNotification({
          message: getApiErrorMessage(error, 'Error while saving contacts'),
          type: 'error',
        })
        throw error
      }

      notificationsStore.addNotification({
        message:
          contactsToSave.length > 1
            ? 'Contacts created successfully'
            : 'Contact created successfully',
        type: 'success',
      })
      if (refresh) {
        await loadContacts(true)
      }
      return response.data
    } finally {
      loading.value = false
    }
  }

  async function updateContact(
    id: string,
    payload: ContactInput,
    options: ContactMutationOptions = {},
  ): Promise<EntitiesContact> {
    const { refresh = true } = options
    loading.value = true
    try {
      let response: { data: EntitiesContact }
      try {
        response = await apiFetch<{ data: EntitiesContact }>(
          `/v1/contacts/${id}`,
          {
            method: 'PUT',
            body: payload,
          },
        )
      } catch (error: unknown) {
        notificationsStore.addNotification({
          message: getApiErrorMessage(error, 'Error while updating contact'),
          type: 'error',
        })
        throw error
      }

      notificationsStore.addNotification({
        message: 'Contact updated',
        type: 'success',
      })
      if (refresh) {
        await loadContacts(true)
      }
      return response.data
    } finally {
      loading.value = false
    }
  }

  async function deleteContact(id: string): Promise<void> {
    loading.value = true
    try {
      await apiFetch(`/v1/contacts/${id}`, { method: 'DELETE' })
      // Invalidate any in-flight load so a stale response cannot resurrect the
      // just-deleted contact. Bumping the generation makes loadContacts skip
      // its assignment (and its finally toggling loading) for the older request.
      loadContactsGeneration++
      contacts.value = contacts.value.filter((contact) => contact.id !== id)
      total.value = Math.max(0, total.value - 1)
      notificationsStore.addNotification({
        message: 'Contact deleted successfully',
        type: 'success',
      })
    } catch (error: unknown) {
      notificationsStore.addNotification({
        message: getApiErrorMessage(error, 'Error while deleting contact'),
        type: 'error',
      })
      throw error
    } finally {
      loading.value = false
    }
  }

  async function uploadCsv(file: File): Promise<void> {
    loading.value = true
    try {
      try {
        const formData = new FormData()
        formData.append('document', file)

        await apiFetch('/v1/contacts/upload', {
          method: 'POST',
          body: formData,
        })
      } catch (error: unknown) {
        notificationsStore.addNotification({
          message: getApiErrorMessage(error, 'Error while importing contacts'),
          type: 'error',
        })
        throw error
      }

      notificationsStore.addNotification({
        message: 'Contacts imported successfully',
        type: 'success',
      })
      await loadContacts(true)
    } finally {
      loading.value = false
    }
  }

  async function loadGroups(): Promise<void> {
    try {
      const response = await apiFetch<{ data: EntitiesContactGroup[] }>(
        '/v1/contacts/groups',
      )
      groups.value = response.data ?? []
    } catch (error: unknown) {
      notificationsStore.addNotification({
        message: getApiErrorMessage(
          error,
          'Error while loading contact groups',
        ),
        type: 'error',
      })
      throw error
    }
  }

  async function renameGroup(oldName: string, newName: string): Promise<void> {
    const trimmedName = newName.trim()
    loading.value = true
    try {
      await apiFetch(`/v1/contacts/groups/${encodeURIComponent(oldName)}`, {
        method: 'PUT',
        body: { name: trimmedName },
      })
      notificationsStore.addNotification({
        message: 'Group renamed successfully',
        type: 'success',
      })
      if (currentGroup === oldName) {
        currentGroup = trimmedName
        group.value = trimmedName
      }
      await loadGroups()
      await loadContacts(true)
    } catch (error: unknown) {
      notificationsStore.addNotification({
        message: getApiErrorMessage(error, 'Error while renaming group'),
        type: 'error',
      })
      throw error
    } finally {
      loading.value = false
    }
  }

  async function deleteGroup(name: string): Promise<void> {
    loading.value = true
    try {
      await apiFetch(`/v1/contacts/groups/${encodeURIComponent(name)}`, {
        method: 'DELETE',
      })
      notificationsStore.addNotification({
        message: 'Group deleted successfully',
        type: 'success',
      })
      if (currentGroup === name) {
        currentGroup = null
        group.value = null
      }
      await loadGroups()
      await loadContacts(true)
    } catch (error: unknown) {
      notificationsStore.addNotification({
        message: getApiErrorMessage(error, 'Error while deleting group'),
        type: 'error',
      })
      throw error
    } finally {
      loading.value = false
    }
  }

  function resetState() {
    loadContactsGeneration++
    contacts.value = []
    groups.value = []
    group.value = null
    total.value = 0
    loading.value = false
    search.value = ''
    currentSkip = 0
    currentLimit = DEFAULT_LIMIT
    currentGroup = null
  }

  return {
    contacts,
    groups,
    group,
    total,
    loading,
    search,
    loadContacts,
    loadGroups,
    renameGroup,
    deleteGroup,
    saveContacts,
    updateContact,
    deleteContact,
    uploadCsv,
    resetState,
  }
})

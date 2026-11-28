<script setup lang="ts">
// App navigation in the Saubhagya / Griha Pravesh style.
// Desktop: a floating, rounded sidebar inset from the window edges that
// collapses to a 72px icon rail. Mobile: a floating pill tab bar where
// the active tab grows into a tinted pill with a filled icon and label.
import { useDisplay } from 'vuetify'
import { getAuth, signOut } from 'firebase/auth'
import {
  mdiHeartPulse,
  mdiMessageText,
  mdiMessageTextOutline,
  mdiPhoneMissed,
  mdiPhoneMissedOutline,
  mdiSend,
  mdiSendOutline,
  mdiAccountMultiple,
  mdiAccountMultipleOutline,
  mdiMagnify,
  mdiKey,
  mdiKeyOutline,
  mdiCreditCard,
  mdiCreditCardOutline,
  mdiCog,
  mdiCogOutline,
  mdiPencil,
  mdiPencilOutline,
  mdiDockLeft,
  mdiLogout,
  mdiUnfoldMoreHorizontal,
} from '@mdi/js'

interface NavItem {
  title: string
  to: string
  icon: string
  activeIcon: string
  match: string[]
}

const route = useRoute()
const router = useRouter()
const { smAndDown } = useDisplay()
const authStore = useAuthStore()
const phonesStore = usePhonesStore()
const threadsStore = useThreadsStore()
const notificationsStore = useNotificationsStore()
const redirectPreferenceStore = useRedirectPreferenceStore()

const primary: NavItem[] = [
  {
    title: 'Messages',
    to: '/threads',
    icon: mdiMessageTextOutline,
    activeIcon: mdiMessageText,
    match: ['threads'],
  },
  {
    title: 'Missed calls',
    to: '/calls',
    icon: mdiPhoneMissedOutline,
    activeIcon: mdiPhoneMissed,
    match: ['calls'],
  },
  {
    title: 'New message',
    to: '/messages',
    icon: mdiPencilOutline,
    activeIcon: mdiPencil,
    match: ['messages'],
  },
  {
    title: 'Bulk send',
    to: '/bulk-messages',
    icon: mdiSendOutline,
    activeIcon: mdiSend,
    match: ['bulk-messages'],
  },
  {
    title: 'Contacts',
    to: '/contacts',
    icon: mdiAccountMultipleOutline,
    activeIcon: mdiAccountMultiple,
    match: ['contacts'],
  },
  {
    title: 'Search',
    to: '/search-messages',
    icon: mdiMagnify,
    activeIcon: mdiMagnify,
    match: ['search-messages'],
  },
]

const developer: NavItem[] = [
  {
    title: 'API keys',
    to: '/phone-api-keys',
    icon: mdiKeyOutline,
    activeIcon: mdiKey,
    match: ['phone-api-keys'],
  },
  {
    title: 'Billing',
    to: '/billing',
    icon: mdiCreditCardOutline,
    activeIcon: mdiCreditCard,
    match: ['billing'],
  },
  {
    title: 'Settings',
    to: '/settings',
    icon: mdiCogOutline,
    activeIcon: mdiCog,
    match: ['settings', 'heartbeats'],
  },
  {
    title: 'Phone status',
    to: '/phone-status',
    icon: mdiHeartPulse,
    activeIcon: mdiHeartPulse,
    match: ['phone-status'],
  },
]

// Five destinations at most on the phone tab bar. Missed calls takes the
// Search tab so the bar keeps five items.
const mobileTabs = computed<NavItem[]>(() => [
  primary[0]!,
  primary[1]!,
  primary[3]!,
  primary[4]!,
  developer[2]!,
])

const RAIL_KEY = 'sg-nav-rail'
const userRail = ref(false)
onMounted(() => {
  try {
    userRail.value = localStorage.getItem(RAIL_KEY) === '1'
  } catch {
    userRail.value = false
  }
})

// Thread pages already show a 400px conversation list, so the nav
// tucks itself into a rail there to leave room for the conversation.
const onThreads = computed(() =>
  ['threads', 'threads-id'].includes((route.name as string) ?? ''),
)
const rail = computed(() => userRail.value || onThreads.value)

function toggleRail() {
  userRail.value = !userRail.value
  try {
    localStorage.setItem(RAIL_KEY, userRail.value ? '1' : '0')
  } catch {
    // Storage can be unavailable (private mode); the toggle still works.
  }
}

function isActive(item: NavItem): boolean {
  const name = (route.name as string) ?? ''
  return item.match.some((m) => name === m || name.startsWith(`${m}-`))
}

const initials = computed(() => {
  const source =
    authStore.authUser?.displayName || authStore.authUser?.email || 'SG'
  return source
    .split(/[\s@.]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((s) => s[0]!.toUpperCase())
    .join('')
})

async function logout() {
  await signOut(getAuth())
  authStore.resetState()
  phonesStore.resetState()
  threadsStore.resetState()
  redirectPreferenceStore.resetState()
  notificationsStore.addNotification({
    type: 'info',
    message: 'You have successfully logged out',
  })
  router.push({ name: 'index' })
}
</script>

<template>
  <!-- Desktop / tablet: floating sidebar -->
  <VNavigationDrawer
    v-if="!smAndDown"
    permanent
    floating
    :rail="rail"
    :width="272"
    :rail-width="104"
    color="transparent"
    class="sg-nav"
  >
    <nav class="sg-nav__panel" :class="{ 'sg-nav__panel--rail': rail }">
      <div class="sg-nav__brand">
        <div class="sg-nav__mark">
          <img src="/img/saubhagya-mark.png" alt="Saubhagya" />
        </div>
        <div v-if="!rail" class="sg-nav__wordmark">
          <div class="sg-nav__name">Saubhagya</div>
          <div class="sg-nav__sub">SMS GATEWAY</div>
        </div>
        <VBtn
          v-if="!rail"
          icon
          variant="text"
          size="small"
          :aria-label="'Collapse navigation'"
          @click="toggleRail"
        >
          <VIcon :icon="mdiDockLeft" color="muted" />
        </VBtn>
      </div>

      <div class="sg-nav__group">
        <NuxtLink
          v-for="item in primary"
          :key="item.to"
          :to="item.to"
          class="sg-nav__item"
          :class="{ 'sg-nav__item--active': isActive(item) }"
          :title="rail ? item.title : undefined"
          :aria-current="isActive(item) ? 'page' : undefined"
        >
          <VIcon
            :icon="isActive(item) ? item.activeIcon : item.icon"
            size="22"
          />
          <span v-if="!rail" class="sg-nav__label">{{ item.title }}</span>
        </NuxtLink>
      </div>

      <div class="sg-nav__group">
        <div v-if="!rail" class="sg-nav__heading">DEVELOPERS</div>
        <NuxtLink
          v-for="item in developer"
          :key="item.to"
          :to="item.to"
          class="sg-nav__item"
          :class="{ 'sg-nav__item--active': isActive(item) }"
          :title="rail ? item.title : undefined"
          :aria-current="isActive(item) ? 'page' : undefined"
        >
          <VIcon
            :icon="isActive(item) ? item.activeIcon : item.icon"
            size="22"
          />
          <span v-if="!rail" class="sg-nav__label">{{ item.title }}</span>
        </NuxtLink>
      </div>

      <div class="sg-nav__spacer" />

      <VBtn
        v-if="rail && !onThreads"
        icon
        variant="text"
        class="mx-auto mb-2"
        :aria-label="'Expand navigation'"
        @click="toggleRail"
      >
        <VIcon :icon="mdiDockLeft" color="muted" />
      </VBtn>

      <VMenu location="end bottom">
        <template #activator="{ props }">
          <button class="sg-nav__account" v-bind="props">
            <span class="sg-nav__avatar">{{ initials }}</span>
            <span v-if="!rail" class="sg-nav__account-text">
              <span class="sg-nav__account-name">{{
                authStore.authUser?.displayName || 'Saubhagya Group'
              }}</span>
              <span class="sg-nav__account-email">{{
                authStore.authUser?.email
              }}</span>
            </span>
            <VIcon
              v-if="!rail"
              :icon="mdiUnfoldMoreHorizontal"
              size="18"
              color="muted"
            />
          </button>
        </template>
        <VList density="comfortable" rounded="xl" class="pa-2">
          <VListItem
            :prepend-icon="mdiCogOutline"
            to="/settings"
            rounded="pill"
          >
            <VListItemTitle>Settings</VListItemTitle>
          </VListItem>
          <VListItem :prepend-icon="mdiLogout" rounded="pill" @click="logout">
            <VListItemTitle>Log out</VListItemTitle>
          </VListItem>
        </VList>
      </VMenu>
    </nav>
  </VNavigationDrawer>

  <!-- Phone: floating pill tab bar -->
  <nav
    v-else-if="route.name !== 'threads-id'"
    class="sg-tabbar"
    aria-label="Main"
  >
    <NuxtLink
      v-for="item in mobileTabs"
      :key="item.to"
      :to="item.to"
      class="sg-tabbar__tab"
      :class="{ 'sg-tabbar__tab--active': isActive(item) }"
      :aria-label="item.title"
      :aria-current="isActive(item) ? 'page' : undefined"
    >
      <VIcon :icon="isActive(item) ? item.activeIcon : item.icon" size="24" />
      <span v-if="isActive(item)" class="sg-tabbar__label">{{
        item.title
      }}</span>
    </NuxtLink>
  </nav>
</template>

<style lang="scss" scoped>
.sg-nav {
  border: none !important;

  :deep(.v-navigation-drawer__content) {
    padding: 16px 0 16px 16px;
    overflow: visible;
  }
}

.sg-nav__panel {
  height: 100%;
  border-radius: 28px;
  background: var(--sg-surface);
  box-shadow: var(--sg-shadow);
  padding: 20px 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  overflow-y: auto;

  &--rail {
    padding: 18px 12px 16px;
    align-items: center;
  }
}

.sg-nav__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.sg-nav__mark {
  width: 44px;
  height: 44px;
  flex: none;
  border-radius: 50%;
  background: var(--sg-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 32px;
    height: 32px;
  }
}

.sg-nav__wordmark {
  flex: 1;
  min-width: 0;
}

.sg-nav__name {
  font-family: 'Bricolage Grotesque', 'Manrope', sans-serif;
  font-size: 17px;
  font-weight: 800;
  color: var(--sg-ink);
  letter-spacing: -0.2px;
}

.sg-nav__sub {
  font-size: 11px;
  font-weight: 800;
  color: var(--sg-primary);
  letter-spacing: 1.6px;
}

.sg-nav__group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
}

.sg-nav__heading {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.2px;
  color: #a89d99;
  padding: 0 14px 6px;
}

.sg-nav__item {
  height: 44px;
  padding: 0 14px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--sg-muted);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;

  .sg-nav__label {
    color: #4a4245;
  }

  &:hover {
    background: var(--sg-canvas);
  }

  &:focus-visible {
    outline: 2px solid var(--sg-primary);
    outline-offset: 2px;
  }

  &--active,
  &--active:hover {
    background: var(--sg-primary-soft);
    color: var(--sg-primary);

    .sg-nav__label {
      color: var(--sg-primary);
      font-weight: 800;
    }
  }

  .sg-nav__panel--rail & {
    width: 48px;
    height: 48px;
    padding: 0;
    justify-content: center;
    border-radius: 24px;
  }
}

.sg-nav__spacer {
  flex: 1;
}

.sg-nav__account {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px;
  border-radius: 22px;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;

  &:hover {
    background: var(--sg-canvas);
  }

  .sg-nav__panel--rail & {
    width: auto;
  }
}

.sg-nav__avatar {
  width: 36px;
  height: 36px;
  flex: none;
  border-radius: 50%;
  background: var(--sg-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sg-nav__account-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sg-nav__account-name {
  font-size: 13px;
  font-weight: 800;
  color: var(--sg-ink);
}

.sg-nav__account-email {
  font-size: 12px;
  color: var(--sg-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sg-tabbar {
  position: fixed;
  left: 16px;
  right: 16px;
  bottom: calc(16px + env(safe-area-inset-bottom));
  height: 68px;
  border-radius: 34px;
  background: var(--sg-surface);
  box-shadow: var(--sg-shadow);
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 0 8px;
  z-index: 1006;
}

.sg-tabbar__tab {
  height: 52px;
  min-width: 44px;
  padding: 0 10px;
  border-radius: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--sg-muted);
  text-decoration: none;

  &--active {
    background: var(--sg-primary-soft);
    color: var(--sg-primary);
    padding: 0 16px;
  }
}

.sg-tabbar__label {
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}
</style>

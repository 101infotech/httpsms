<script setup lang="ts">
// Public website shell (landing, blog, legal pages) in the Saubhagya
// family style: cream canvas, pill navigation, maroon primary action.
import { useDisplay } from 'vuetify'
import { mdiArrowRight, mdiMenu } from '@mdi/js'

const route = useRoute()
const router = useRouter()
const { mdAndUp, xs, width } = useDisplay()
// The pill nav needs ~1100px next to the brand and actions; below that
// the sections move into the menu so labels never wrap.
const wideNav = computed(() => width.value >= 1100)
const authStore = useAuthStore()
const appStore = useAppStore()

const sections = [
  { title: 'How it works', id: 'how-it-works' },
  { title: 'Developers', id: 'developers' },
  { title: 'Security', id: 'security' },
  { title: 'FAQ', id: 'faq' },
]

// The "skip this page" prompt hangs off the sticky header; only show it
// while the visitor is near the top so it never covers page content.
const nearTop = ref(true)
function onScroll() {
  nearTop.value = window.scrollY < 120
}
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

const signedIn = computed(
  () => authStore.authStateChanged && authStore.authUser !== null,
)

async function goToSection(id: string) {
  if (route.name !== 'index') {
    await router.push({ path: '/', hash: `#${id}` })
  }
  setTimeout(() => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  }, 50)
}
</script>

<template>
  <v-app class="sg-site">
    <header class="sg-site__header">
      <NuxtLink to="/" class="sg-brand" aria-label="Saubhagya SMS home">
        <span class="sg-brand__mark">
          <img src="/img/saubhagya-mark.png" alt="" width="30" height="30" />
        </span>
        <span class="sg-brand__text">
          <span class="sg-brand__name">Saubhagya SMS</span>
          <span class="sg-brand__by">by Saubhagya Group</span>
        </span>
      </NuxtLink>

      <nav v-if="wideNav" class="sg-site__nav" aria-label="Sections">
        <button
          v-for="section in sections"
          :key="section.id"
          type="button"
          class="sg-site__nav-link"
          @click="goToSection(section.id)"
        >
          {{ section.title }}
        </button>
        <NuxtLink
          to="/blog"
          class="sg-site__nav-link"
          :class="{
            'sg-site__nav-link--active': route.path.startsWith('/blog'),
          }"
          >Blog</NuxtLink
        >
      </nav>

      <div class="sg-site__actions">
        <template v-if="signedIn">
          <div class="position-relative d-inline-block">
            <v-btn
              color="primary"
              :to="{ name: 'threads' }"
              :size="xs ? 'default' : 'large'"
            >
              {{ xs ? 'Dashboard' : 'Open dashboard' }}
              <v-icon v-if="!xs" end :icon="mdiArrowRight" />
            </v-btn>
            <RedirectPromptPopover :hidden="!nearTop" />
          </div>
        </template>
        <template v-else>
          <v-btn
            v-if="mdAndUp"
            variant="text"
            size="large"
            :to="{ name: 'login' }"
            >Sign in</v-btn
          >
          <v-btn
            v-if="!xs"
            color="primary"
            size="large"
            :to="{ name: 'login' }"
          >
            Get started
            <v-icon end :icon="mdiArrowRight" />
          </v-btn>
        </template>
        <v-menu v-if="!wideNav">
          <template #activator="{ props }">
            <v-btn
              v-bind="props"
              icon
              variant="flat"
              color="surface"
              class="sg-site__menu-btn"
              aria-label="Open menu"
            >
              <v-icon :icon="mdiMenu" />
            </v-btn>
          </template>
          <v-list rounded="xl" class="pa-2">
            <v-list-item
              v-for="section in sections"
              :key="section.id"
              rounded="pill"
              :title="section.title"
              @click="goToSection(section.id)"
            />
            <v-list-item rounded="pill" title="Blog" to="/blog" />
            <v-list-item
              v-if="!signedIn && xs"
              rounded="pill"
              title="Get started"
              base-color="primary"
              :to="{ name: 'login' }"
            />
            <v-list-item
              v-if="!signedIn"
              rounded="pill"
              title="Sign in"
              :to="{ name: 'login' }"
            />
          </v-list>
        </v-menu>
      </div>
    </header>

    <v-main>
      <AppToast />
      <slot />
    </v-main>

    <footer class="sg-site__footer">
      <div class="sg-site__footer-top">
        <div class="sg-site__footer-brand">
          <NuxtLink to="/" class="sg-brand">
            <span class="sg-brand__mark">
              <img
                src="/img/saubhagya-mark.png"
                alt=""
                width="30"
                height="30"
              />
            </span>
            <span class="sg-brand__name">Saubhagya SMS</span>
          </NuxtLink>
          <p>
            A Saubhagya Group product. Building enduring businesses for a
            growing Nepal.
          </p>
        </div>
        <div class="sg-site__footer-cols">
          <div>
            <h4>Product</h4>
            <a href="#how-it-works" @click.prevent="goToSection('how-it-works')"
              >How it works</a
            >
            <a href="#security" @click.prevent="goToSection('security')"
              >Security</a
            >
            <a :href="appStore.appData.appDownloadUrl">Download app</a>
            <NuxtLink :to="{ name: 'threads' }">Dashboard</NuxtLink>
          </div>
          <div>
            <h4>Developers</h4>
            <a :href="appStore.appData.documentationUrl" target="_blank"
              >API docs</a
            >
            <a :href="appStore.appData.githubUrl" target="_blank">GitHub</a>
            <NuxtLink to="/blog">Blog</NuxtLink>
          </div>
          <div>
            <h4>Company</h4>
            <a href="https://saubhagyagroup.com" target="_blank"
              >Saubhagya Group</a
            >
            <NuxtLink to="/privacy-policy">Privacy policy</NuxtLink>
            <NuxtLink to="/terms-and-conditions">Terms</NuxtLink>
          </div>
        </div>
      </div>
      <div class="sg-site__footer-bottom">
        <span
          >© {{ new Date().getFullYear() }} Saubhagya Group · Kathmandu,
          Nepal</span
        >
        <span>Based on the open-source httpSMS project</span>
      </div>
    </footer>
  </v-app>
</template>

<style lang="scss" scoped>
.sg-site {
  background: var(--sg-canvas) !important;
}

.sg-site__header {
  position: sticky;
  top: 0;
  z-index: 1004;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px clamp(16px, 5vw, 80px);
  background: rgb(250 246 241 / 88%);
  backdrop-filter: blur(12px);
}

.sg-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.sg-brand__mark {
  width: 44px;
  height: 44px;
  flex: none;
  border-radius: 50%;
  background: var(--sg-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sg-brand__text {
  display: flex;
  flex-direction: column;
}

.sg-brand__name {
  white-space: nowrap;
  font-family: var(--sg-font-display);
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.3px;
  color: var(--sg-ink);
}

.sg-brand__by {
  font-size: 11px;
  font-weight: 700;
  color: var(--sg-muted);

  @media (width <= 600px) {
    display: none;
  }
}

/* Keep in-page anchors clear of the sticky header. */
:deep(section[id]) {
  scroll-margin-top: 96px;
}

.sg-site__nav {
  display: flex;
  gap: 4px;
  padding: 6px;
  border-radius: 999px;
  background: var(--sg-surface);
  box-shadow: var(--sg-shadow-sm);
}

.sg-site__nav-link {
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: none;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #4a4245;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;

  &:hover {
    background: var(--sg-canvas);
  }

  &--active {
    background: var(--sg-primary-soft);
    color: var(--sg-primary);
    font-weight: 800;
  }
}

.sg-site__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sg-site__menu-btn {
  box-shadow: var(--sg-shadow-sm);
}

.sg-site__footer {
  padding: 56px clamp(16px, 5vw, 80px) 40px;
  border-top: 1px solid var(--sg-line);
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.sg-site__footer-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 40px;
}

.sg-site__footer-brand {
  max-width: 360px;

  p {
    margin-top: 14px;
    font-size: 14px;
    line-height: 1.6;
    color: var(--sg-muted);
  }
}

.sg-site__footer-cols {
  display: flex;
  flex-wrap: wrap;
  gap: clamp(32px, 6vw, 72px);

  div {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  h4 {
    margin: 0;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 1.4px;
    text-transform: uppercase;
    color: var(--sg-muted);
  }

  a {
    font-size: 15px;
    font-weight: 600;
    color: #4a4245;
    text-decoration: none;

    &:hover {
      color: var(--sg-primary);
    }
  }
}

.sg-site__footer-bottom {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  color: var(--sg-muted);
}
</style>

<script setup lang="ts">
import {
  mdiArrowRight,
  mdiCheckCircle,
  mdiCheckAll,
  mdiCodeTags,
  mdiDownload,
  mdiQrcode,
  mdiCellphone,
  mdiHeartPulse,
  mdiTrayArrowDown,
  mdiSendOutline,
  mdiClockOutline,
  mdiChartBar,
  mdiWebhook,
  mdiFileDelimitedOutline,
  mdiLockOutline,
  mdiServerSecurity,
  mdiKeyOutline,
  mdiPlus,
  mdiMinus,
} from '@mdi/js'

definePageMeta({
  layout: 'website',
  middleware: ['redirect-to-threads'],
})

useSeoMeta({
  title: 'Saubhagya SMS · Send SMS from your own Android phones with one API',
  description:
    'Saubhagya SMS turns the Android phones and NTC or Ncell SIMs you already own into a programmable SMS gateway with webhooks, bulk sending and end-to-end encryption.',
  ogTitle: 'Saubhagya SMS · Send SMS from your own phones, with one API',
  ogDescription:
    'A programmable SMS gateway for every Saubhagya venture. OTPs, bookings and reminders, delivered from local numbers.',
  twitterCard: 'summary_large_image',
})

const appStore = useAppStore()

const API_URL = 'https://sms-api.saubhagyagroup.com/v1/messages/send'
const KEY_HINT = 'API key from https://sms.saubhagyagroup.com/settings'

interface Sample {
  id: string
  label: string
  lang: string
  code: string
}

const samples: Sample[] = [
  {
    id: 'python',
    label: 'Python',
    lang: 'python',
    code: `import requests

response = requests.post(
    "${API_URL}",
    headers={"x-api-key": "${KEY_HINT}"},
    json={
        "from": "+9779805550101",
        "to": "+9779841102233",
        "content": "Your site visit is confirmed for Friday",
    },
)

print(response.json()["data"]["status"])  # "pending"`,
  },
  {
    id: 'javascript',
    label: 'JavaScript',
    lang: 'javascript',
    code: `const response = await fetch("${API_URL}", {
  method: "POST",
  headers: {
    "x-api-key": "${KEY_HINT}",
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    from: "+9779805550101",
    to: "+9779841102233",
    content: "Your site visit is confirmed for Friday",
  }),
})

console.log((await response.json()).data.id)`,
  },
  {
    id: 'php',
    label: 'PHP',
    lang: 'php',
    code: `<?php
$context = stream_context_create(['http' => [
    'method'  => 'POST',
    'header'  => "Content-Type: application/json\\r\\n" .
                 "x-api-key: ${KEY_HINT}\\r\\n",
    'content' => json_encode([
        'from'    => '+9779805550101',
        'to'      => '+9779841102233',
        'content' => 'Your site visit is confirmed for Friday',
    ]),
]]);

echo file_get_contents('${API_URL}', false, $context);`,
  },
  {
    id: 'go',
    label: 'Go',
    lang: 'go',
    code: `body := strings.NewReader(\`{
  "from": "+9779805550101",
  "to": "+9779841102233",
  "content": "Your site visit is confirmed for Friday"
}\`)

req, _ := http.NewRequest("POST", "${API_URL}", body)
req.Header.Set("x-api-key", "${KEY_HINT}")
req.Header.Set("Content-Type", "application/json")

res, err := http.DefaultClient.Do(req)`,
  },
  {
    id: 'curl',
    label: 'cURL',
    lang: 'bash',
    code: `curl -X POST '${API_URL}' \\
  -H 'x-api-key: ${KEY_HINT}' \\
  -H 'Content-Type: application/json' \\
  -d '{
    "from": "+9779805550101",
    "to": "+9779841102233",
    "content": "Your site visit is confirmed for Friday"
  }'`,
  },
  {
    id: 'csharp',
    label: 'C#',
    lang: 'csharp',
    code: `var client = new HttpClient();
client.DefaultRequestHeaders.Add("x-api-key", "${KEY_HINT}");

var response = await client.PostAsJsonAsync("${API_URL}", new {
    from = "+9779805550101",
    to = "+9779841102233",
    content = "Your site visit is confirmed for Friday",
});

Console.WriteLine(await response.Content.ReadAsStringAsync());`,
  },
  {
    id: 'java',
    label: 'Java',
    lang: 'java',
    code: `var request = HttpRequest.newBuilder()
    .uri(URI.create("${API_URL}"))
    .header("x-api-key", "${KEY_HINT}")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("""
        {"from": "+9779805550101",
         "to": "+9779841102233",
         "content": "Your site visit is confirmed for Friday"}
        """))
    .build();

var response = HttpClient.newHttpClient()
    .send(request, HttpResponse.BodyHandlers.ofString());`,
  },
]
const selectedSample = ref('python')

const ventures = [
  'Saubhagya Ghar',
  'Your Hostel',
  'SSIT Institute',
  'The Hive Fusion',
  'Brand Bird',
  'Nova Production',
]

const steps = [
  {
    title: 'Install the app',
    text: 'Put the Saubhagya SMS app on any Android 9+ phone with a SIM card. An old office phone works fine.',
    icon: mdiDownload,
    tone: 'primary',
  },
  {
    title: 'Pair with a QR code',
    text: 'Scan the code on the dashboard. The phone picks up your API key and server, then starts sending heartbeats.',
    icon: mdiQrcode,
    tone: 'gold',
  },
  {
    title: 'Call the API',
    text: 'POST a message from any language. The phone sends it from your number and reports delivery back to you.',
    icon: mdiCodeTags,
    tone: 'success',
  },
]

const features = [
  {
    icon: mdiCellphone,
    title: 'Many phones, two SIMs each',
    text: 'Spread traffic across office phones and pick SIM 1 or SIM 2 for every message.',
    tone: 'primary',
  },
  {
    icon: mdiHeartPulse,
    title: 'Heartbeat monitoring',
    text: 'Every phone checks in. Get an alert when one goes quiet, runs low or loses data.',
    tone: 'success',
  },
  {
    icon: mdiTrayArrowDown,
    title: 'Receive and reply',
    text: 'Incoming SMS and missed calls are forwarded to your server and webhooks.',
    tone: 'gold',
  },
  {
    icon: mdiSendOutline,
    title: 'Bulk send',
    text: 'Upload a CSV or Excel sheet and send thousands of personalised messages.',
    tone: 'primary',
  },
  {
    icon: mdiClockOutline,
    title: 'Send windows',
    text: 'Only send during business hours, and pace messages so carriers stay happy.',
    tone: 'gold',
  },
  {
    icon: mdiChartBar,
    title: 'Delivery you can see',
    text: 'Sent, delivered and failed states for every message, on the dashboard and via the API.',
    tone: 'success',
  },
]

const securityPoints = [
  {
    icon: mdiLockOutline,
    title: 'End-to-end encryption',
    text: 'Encrypt message content with a key that lives only on your phones.',
  },
  {
    icon: mdiServerSecurity,
    title: 'Self-hosted',
    text: 'Runs on Saubhagya infrastructure, not a third-party SMS provider.',
  },
  {
    icon: mdiKeyOutline,
    title: 'Per-phone API keys',
    text: 'Revoke a lost or retired phone in one click.',
  },
]

const faqs = [
  {
    q: 'Can I use an iPhone?',
    a: "No. iOS doesn't let apps send SMS in the background, so the gateway app runs on Android only.",
  },
  {
    q: 'Which Android version do I need?',
    a: 'Android 9 (Pie) or newer. A spare or older office phone is a perfectly good gateway.',
  },
  {
    q: 'Which number do recipients see?',
    a: 'The number of the SIM that sent the message. Messages go out over your own SIM card, so custom sender names are not supported.',
  },
  {
    q: 'How many messages can I send?',
    a: 'It depends on your SIM plan and carrier limits. Add more phones to spread the load, and use send windows to pace large batches.',
  },
  {
    q: 'Is my message content stored?',
    a: 'Messages are stored on the Saubhagya SMS server so you can see history and delivery status. Turn on end-to-end encryption and the server only keeps ciphertext.',
  },
]
const openFaq = ref<number | null>(0)
</script>

<template>
  <div class="sg-landing">
    <!-- Hero -->
    <section class="sg-hero sg-wrap">
      <div class="sg-hero__copy">
        <div class="sg-pill-note">
          <span class="sg-pill-note__tag">NEW</span>
          Pair a phone in 30 seconds with a QR code
        </div>
        <h1 class="sg-hero__title">
          Send SMS from your own phones, with
          <span class="text-primary">one API.</span>
        </h1>
        <p class="sg-lead">
          Turn the Android phones and NTC or Ncell SIMs you already own into a
          programmable SMS gateway for every Saubhagya venture. OTPs, bookings
          and reminders, delivered from local numbers.
        </p>
        <div class="sg-actions">
          <v-btn color="primary" size="x-large" :to="{ name: 'login' }">
            Get started free
            <v-icon end :icon="mdiArrowRight" />
          </v-btn>
          <v-btn
            size="x-large"
            variant="flat"
            color="surface"
            class="sg-btn-soft"
            :href="appStore.appData.documentationUrl"
            target="_blank"
          >
            <v-icon start :icon="mdiCodeTags" />
            Read the API docs
          </v-btn>
        </div>
        <ul class="sg-checks">
          <li>
            <v-icon color="success" :icon="mdiCheckCircle" /> No SMS provider
          </li>
          <li>
            <v-icon color="success" :icon="mdiCheckCircle" /> End-to-end
            encrypted
          </li>
          <li><v-icon color="success" :icon="mdiCheckCircle" /> Open source</li>
        </ul>
      </div>

      <div class="sg-hero__visual" aria-hidden="true">
        <div class="sg-hero__blob" />
        <div class="sg-hero__sun" />
        <div class="sg-phone">
          <div class="sg-phone__screen">
            <div class="sg-phone__head">
              <span class="sg-brand-dot">
                <img
                  src="/img/saubhagya-mark.png"
                  alt=""
                  width="26"
                  height="26"
                />
              </span>
              <div>
                <small>Namaste</small>
                <strong>Saubhagya SMS</strong>
              </div>
            </div>
            <div class="sg-phone__hero">
              <span class="sg-phone__status"><i />GATEWAY ACTIVE</span>
              <small>Sent today</small>
              <strong>842</strong>
            </div>
            <div class="sg-phone__sim">
              <b>1</b>
              <div>
                <strong>SIM 1 · NTC</strong><small>+977 980-555-0101</small>
              </div>
              <em class="ok">Sending</em>
            </div>
            <div class="sg-phone__sim">
              <b>2</b>
              <div>
                <strong>SIM 2 · Ncell</strong><small>+977 981-555-0199</small>
              </div>
              <em class="gold">Receiving</em>
            </div>
          </div>
        </div>
        <div class="sg-code-float">
          <div class="sg-code-float__bar">
            <i /><i /><i /><span>POST /v1/messages/send</span>
          </div>
          <pre>
curl {{ API_URL }}
  -H "x-api-key: $API_KEY"
  -d '{"to":"+977984…","content":"Your OTP is 482913"}'</pre>
        </div>
        <div class="sg-bubble-float">
          <span>Your SSIT portal OTP is 482913</span>
          <small
            ><v-icon size="14" :icon="mdiCheckAll" /> Delivered · 1.8s</small
          >
        </div>
      </div>
    </section>

    <!-- Ventures -->
    <section class="sg-ventures sg-wrap">
      <p>One gateway, built for every venture in the Saubhagya family</p>
      <div class="sg-ventures__list">
        <span v-for="v in ventures" :key="v">{{ v }}</span>
      </div>
    </section>

    <!-- How it works -->
    <section id="how-it-works" class="sg-section sg-wrap">
      <div class="sg-eyebrow">HOW IT WORKS</div>
      <h2 class="sg-h2">Live in three steps. No contracts, no short codes.</h2>
      <div class="sg-grid sg-grid--3">
        <article v-for="(step, i) in steps" :key="step.title" class="sg-tile">
          <div class="sg-tile__top">
            <span class="sg-step-no">0{{ i + 1 }}</span>
            <span class="sg-icon-tile" :class="`sg-tone-${step.tone}`">
              <v-icon :icon="step.icon" />
            </span>
          </div>
          <h3>{{ step.title }}</h3>
          <p>{{ step.text }}</p>
        </article>
      </div>
    </section>

    <!-- Developers -->
    <section id="developers" class="sg-wrap">
      <div class="sg-dev">
        <div class="sg-dev__copy">
          <div class="sg-eyebrow sg-eyebrow--gold">FOR DEVELOPERS</div>
          <h2 class="sg-h2 text-white">
            A few lines of code. Any language you like.
          </h2>
          <p class="sg-lead sg-lead--dark">
            One REST endpoint to send, webhooks for every delivery and reply,
            and an API key per phone. Bulk-send from a CSV or Excel file when
            you don't want to write code at all.
          </p>
          <ul class="sg-dev__list">
            <li>
              <v-icon :icon="mdiWebhook" /> Webhooks for sent, delivered, failed
              and received
            </li>
            <li>
              <v-icon :icon="mdiFileDelimitedOutline" /> Bulk messages from CSV
              and Excel
            </li>
            <li>
              <v-icon :icon="mdiClockOutline" /> Send windows and scheduled
              messages
            </li>
          </ul>
          <v-btn
            color="primary"
            size="large"
            :href="appStore.appData.documentationUrl"
            target="_blank"
          >
            Read the docs
            <v-icon end :icon="mdiArrowRight" />
          </v-btn>
        </div>
        <div class="sg-dev__code">
          <div class="sg-dev__tabs" role="tablist" aria-label="Code language">
            <button
              v-for="s in samples"
              :key="s.id"
              type="button"
              role="tab"
              :aria-selected="selectedSample === s.id"
              :class="{ active: selectedSample === s.id }"
              @click="selectedSample = s.id"
            >
              {{ s.label }}
            </button>
          </div>
          <div v-highlight>
            <pre
              v-for="s in samples"
              v-show="selectedSample === s.id"
              :key="s.id"
            ><code :class="`language-${s.lang}`">{{ s.code }}</code></pre>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="sg-section sg-wrap">
      <div class="sg-split-head">
        <div>
          <div class="sg-eyebrow">EVERYTHING YOU NEED</div>
          <h2 class="sg-h2">Built for real operations, not just demos.</h2>
        </div>
        <p class="sg-lead">
          From a single OTP to a festival-season campaign, the gateway handles
          routing, retries and reporting for you.
        </p>
      </div>
      <div class="sg-grid sg-grid--3">
        <article v-for="f in features" :key="f.title" class="sg-tile">
          <span class="sg-icon-tile" :class="`sg-tone-${f.tone}`">
            <v-icon :icon="f.icon" />
          </span>
          <h3>{{ f.title }}</h3>
          <p>{{ f.text }}</p>
        </article>
      </div>
    </section>

    <!-- Security -->
    <section id="security" class="sg-wrap">
      <div class="sg-security">
        <div class="sg-security__copy">
          <div class="sg-eyebrow sg-eyebrow--gold">SECURITY &amp; PRIVACY</div>
          <h2 class="sg-h2 text-white">
            Your messages never touch a third-party SMS provider.
          </h2>
          <p class="sg-lead sg-lead--maroon">
            Messages travel from your server to your phone and out over your own
            SIM. Turn on end-to-end encryption and the server only stores
            ciphertext.
          </p>
        </div>
        <div class="sg-security__list">
          <div
            v-for="p in securityPoints"
            :key="p.title"
            class="sg-security__item"
          >
            <v-icon :icon="p.icon" />
            <div>
              <strong>{{ p.title }}</strong>
              <span>{{ p.text }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ -->
    <section id="faq" class="sg-section sg-wrap sg-faq">
      <div class="sg-faq__intro">
        <div class="sg-eyebrow">FAQ</div>
        <h2 class="sg-h2">Questions, answered.</h2>
        <p class="sg-lead">
          Can't find what you need? Write to the Saubhagya tech team and we'll
          help you set up.
        </p>
      </div>
      <div class="sg-faq__list">
        <div
          v-for="(item, i) in faqs"
          :key="item.q"
          class="sg-faq__item"
          :class="{ open: openFaq === i }"
        >
          <button
            type="button"
            :aria-expanded="openFaq === i"
            @click="openFaq = openFaq === i ? null : i"
          >
            <span>{{ item.q }}</span>
            <span class="sg-faq__toggle">
              <v-icon size="18" :icon="openFaq === i ? mdiMinus : mdiPlus" />
            </span>
          </button>
          <p v-show="openFaq === i">{{ item.a }}</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="sg-wrap sg-cta-wrap">
      <div class="sg-cta">
        <div>
          <h2 class="sg-h2">Ready to send your first message?</h2>
          <p class="sg-lead sg-lead--gold">
            Install the app, pair your phone and send a test SMS in under five
            minutes.
          </p>
        </div>
        <div class="sg-actions">
          <v-btn color="primary" size="x-large" :to="{ name: 'login' }">
            Get started free
            <v-icon end :icon="mdiArrowRight" />
          </v-btn>
          <v-btn
            size="x-large"
            variant="flat"
            color="surface"
            class="sg-btn-soft"
            :href="appStore.appData.appDownloadUrl"
          >
            <v-icon start :icon="mdiDownload" />
            Download the app
          </v-btn>
        </div>
      </div>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.sg-landing {
  --gutter: clamp(16px, 5vw, 80px);

  color: var(--sg-ink);
}

.sg-wrap {
  max-width: 1440px;
  margin: 0 auto;
  padding-left: var(--gutter);
  padding-right: var(--gutter);
}

.sg-section {
  padding-top: clamp(56px, 8vw, 96px);
  padding-bottom: clamp(56px, 8vw, 96px);
}

.sg-eyebrow {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1.6px;
  color: var(--sg-primary);

  &--gold {
    color: var(--sg-gold);
  }
}

.sg-h2 {
  margin: 14px 0 0;
  max-width: 720px;
  font-family: var(--sg-font-display);
  font-size: clamp(32px, 4vw, 44px);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.03em;
}

.sg-lead {
  max-width: 600px;
  margin: 0;
  font-size: clamp(16px, 1.4vw, 18px);
  line-height: 1.6;
  color: var(--sg-muted);

  &--dark {
    color: #cdbfc3;
  }

  &--maroon {
    color: #f7d3db;
  }

  &--gold {
    color: var(--sg-gold-ink);
  }
}

.sg-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.sg-btn-soft {
  box-shadow: var(--sg-shadow-sm) !important;
}

/* Hero */
.sg-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
  align-items: center;
  gap: 40px;
  padding-top: clamp(24px, 4vw, 56px);
  padding-bottom: clamp(48px, 7vw, 96px);
}

.sg-hero__copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 26px;
}

.sg-pill-note {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  border-radius: 999px;
  background: var(--sg-surface);
  box-shadow: var(--sg-shadow-sm);
  font-size: 13px;
  font-weight: 700;

  &__tag {
    padding: 4px 10px;
    border-radius: 999px;
    background: var(--sg-gold);
    color: var(--sg-gold-ink);
    font-size: 11px;
    font-weight: 800;
  }
}

.sg-hero__title {
  margin: 0;
  font-family: var(--sg-font-display);
  font-size: clamp(42px, 5.6vw, 72px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.035em;
}

.sg-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 22px;
  padding: 0;
  margin: 0;
  list-style: none;
  font-size: 14px;
  font-weight: 600;
  color: var(--sg-muted);

  li {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.sg-hero__visual {
  position: relative;
  height: 620px;
}

.sg-hero__blob {
  position: absolute;
  left: 18%;
  top: 40px;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: var(--sg-primary-soft);
}

.sg-hero__sun {
  position: absolute;
  right: 4%;
  top: 70px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: var(--sg-gold);
}

.sg-phone {
  position: absolute;
  left: 38%;
  top: 0;
  width: 300px;
  height: 600px;
  padding: 10px;
  border-radius: 44px;
  background: var(--sg-ink);
  box-shadow: 0 30px 80px rgb(31 26 28 / 25%);

  &__screen {
    height: 100%;
    padding: 30px 16px 16px;
    border-radius: 36px;
    background: var(--sg-canvas);
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 10px;

    small {
      display: block;
      font-size: 11px;
      color: var(--sg-muted);
    }

    strong {
      font-family: var(--sg-font-display);
      font-size: 17px;
      font-weight: 800;
    }
  }

  &__hero {
    padding: 16px;
    border-radius: 22px;
    background: var(--sg-primary);
    color: #fff;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;

    small {
      color: #f7d3db;
      font-size: 11px;
    }

    strong {
      font-family: var(--sg-font-display);
      font-size: 36px;
      font-weight: 800;
      line-height: 1;
    }
  }

  &__status {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px;
    border-radius: 999px;
    background: rgb(255 255 255 / 16%);
    font-size: 10px;
    font-weight: 800;

    i {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #7ce0b5;
    }
  }

  &__sim {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border-radius: 18px;
    background: var(--sg-surface);
    box-shadow: var(--sg-shadow-sm);

    b {
      width: 32px;
      height: 32px;
      border-radius: 10px;
      background: var(--sg-primary-soft);
      color: var(--sg-primary);
      font-family: var(--sg-font-display);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    div {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    strong {
      font-size: 12px;
    }

    small {
      font-size: 10px;
      color: var(--sg-muted);
    }

    em {
      padding: 3px 8px;
      border-radius: 999px;
      font-size: 10px;
      font-style: normal;
      font-weight: 800;

      &.ok {
        background: #e5f0ea;
        color: #0a7a4f;
      }

      &.gold {
        background: #f5e7cc;
        color: var(--sg-gold-ink);
      }
    }
  }
}

.sg-brand-dot {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--sg-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sg-code-float {
  position: absolute;
  left: 0;
  top: 250px;
  width: min(430px, 70%);
  padding: 18px 20px;
  border-radius: 24px;
  background: var(--sg-ink);
  box-shadow: 0 24px 60px rgb(31 26 28 / 28%);

  &__bar {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 10px;

    i {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #e2544e;

      &:nth-child(2) {
        background: #e9ad47;
      }

      &:nth-child(3) {
        background: #5bbf7a;
      }
    }

    span {
      margin-left: auto;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 1px;
      color: #a89d99;
    }
  }

  pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 12.5px;
    line-height: 1.7;
    color: var(--sg-gold);
  }
}

.sg-bubble-float {
  position: absolute;
  left: 58%;
  top: 450px;
  width: 230px;
  padding: 14px 16px;
  border-radius: 22px 22px 6px;
  background: var(--sg-surface);
  box-shadow: 0 18px 40px rgb(31 26 28 / 16%);
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;

  small {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    font-weight: 800;
    color: #0a7a4f;
  }
}

/* Ventures */
.sg-ventures {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  text-align: center;

  p {
    margin: 0;
    font-size: 13px;
    font-weight: 700;
    color: var(--sg-muted);
  }

  &__list {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;

    span {
      padding: 12px 20px;
      border-radius: 999px;
      background: var(--sg-surface);
      box-shadow: var(--sg-shadow-sm);
      font-family: var(--sg-font-display);
      font-size: 16px;
      font-weight: 700;
      color: #4a4245;
    }
  }
}

/* Grids and tiles */
.sg-grid {
  display: grid;
  gap: 20px;
  margin-top: 44px;

  &--3 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.sg-tile {
  padding: 26px;
  border-radius: 28px;
  background: var(--sg-surface);
  box-shadow: var(--sg-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 14px;

  h3 {
    margin: 0;
    font-family: var(--sg-font-display);
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.02em;
  }

  p {
    margin: 0;
    font-size: 15px;
    line-height: 1.6;
    color: var(--sg-muted);
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.sg-step-no {
  font-family: var(--sg-font-display);
  font-size: 48px;
  font-weight: 800;
  line-height: 1;
  color: var(--sg-primary-soft);
}

.sg-icon-tile {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sg-tone-primary {
  background: var(--sg-primary-soft);
  color: var(--sg-primary);
}

.sg-tone-gold {
  background: #f5e7cc;
  color: var(--sg-gold-ink);
}

.sg-tone-success {
  background: #e5f0ea;
  color: #0a7a4f;
}

.sg-split-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;

  .sg-lead {
    max-width: 420px;
  }
}

/* Developers */
.sg-dev {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  align-items: center;
  gap: 56px;
  padding: clamp(28px, 5vw, 64px);
  border-radius: 40px;
  background: var(--sg-ink);

  &__copy {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 0;
    margin: 0;
    list-style: none;
    font-size: 15px;
    font-weight: 600;
    color: #f3ecee;

    li {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .v-icon {
      color: var(--sg-gold);
    }
  }

  &__code {
    min-width: 0;
    padding: 22px;
    border-radius: 28px;
    background: #2a2326;

    pre {
      margin: 16px 0 0;
      overflow-x: auto;
      font-size: 13.5px;
      line-height: 1.7;
    }
  }

  &__tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    button {
      padding: 8px 14px;
      border: none;
      border-radius: 999px;
      background: rgb(255 255 255 / 8%);
      color: #e9dee0;
      font: inherit;
      font-size: 13px;
      font-weight: 700;
      cursor: pointer;

      &.active {
        background: #fff;
        color: var(--sg-ink);
      }
    }
  }
}

/* Security */
.sg-security {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  align-items: center;
  gap: 56px;
  margin-top: clamp(56px, 8vw, 96px);
  padding: clamp(28px, 5vw, 56px) clamp(28px, 5vw, 64px);
  border-radius: 40px;
  background: var(--sg-primary);

  &__copy {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    border-radius: 22px;
    background: rgb(255 255 255 / 12%);
    color: #fff;

    .v-icon {
      color: var(--sg-gold);
    }

    div {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    strong {
      font-size: 16px;
    }

    span {
      font-size: 14px;
      color: #f7d3db;
    }
  }
}

/* FAQ */
.sg-faq {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: clamp(32px, 6vw, 80px);

  &__intro {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__item {
    border-radius: 24px;
    background: var(--sg-surface);
    box-shadow: var(--sg-shadow-sm);

    button {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 22px 26px;
      border: none;
      background: none;
      text-align: left;
      font-family: var(--sg-font-display);
      font-size: 19px;
      font-weight: 700;
      color: var(--sg-ink);
      cursor: pointer;
    }

    p {
      margin: -8px 26px 0;
      padding-bottom: 22px;
      font-size: 15px;
      line-height: 1.6;
      color: var(--sg-muted);
    }

    &.open .sg-faq__toggle {
      background: var(--sg-primary-soft);
      color: var(--sg-primary);
    }
  }

  &__toggle {
    flex: none;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: var(--sg-canvas);
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

/* CTA */
.sg-cta-wrap {
  padding-bottom: 80px;
}

.sg-cta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: clamp(28px, 5vw, 64px);
  border-radius: 40px;
  background: #f5e7cc;

  .sg-h2 {
    margin: 0 0 14px;
  }
}

/* Responsive */
@media (width <= 1100px) {
  .sg-hero,
  .sg-dev,
  .sg-security,
  .sg-faq {
    grid-template-columns: minmax(0, 1fr);
  }

  .sg-hero__visual {
    max-width: 640px;
    width: 100%;
    margin: 0 auto;
  }
}

@media (width <= 900px) {
  .sg-grid--3 {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (width <= 600px) {
  .sg-hero__visual {
    height: 460px;
  }

  .sg-hero__blob {
    left: 10%;
    width: 340px;
    height: 340px;
  }

  .sg-hero__sun {
    display: none;
  }

  .sg-phone {
    left: auto;
    right: 0;
    transform: scale(0.72);
    transform-origin: top right;
  }

  .sg-code-float {
    top: 230px;
    width: 88%;
  }

  .sg-bubble-float {
    display: none;
  }

  .sg-actions .v-btn {
    width: 100%;
  }
}
</style>

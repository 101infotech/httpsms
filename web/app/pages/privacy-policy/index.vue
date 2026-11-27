<script setup lang="ts">
definePageMeta({
  layout: 'website',
})

useSeoMeta({
  title: 'Privacy Policy - Saubhagya SMS',
  description:
    'How Saubhagya SMS (operated by Saubhagya Group) collects, uses and protects your data, including SMS and call information handled by the Android app.',
})

const contactEmail = 'support@saubhagyagroup.com'
</script>

<template>
  <VContainer>
    <VRow>
      <VCol cols="12" md="8" offset-md="2">
        <h1 class="text-display-medium mb-2">Privacy Policy</h1>
        <p class="text-body-medium mb-6">Effective date: 26 November 2026</p>

        <p class="text-body-large mb-4">
          Saubhagya SMS ("the Service") is an SMS gateway operated by Saubhagya
          Group ("we", "us"). It is made up of the web dashboard at
          sms.saubhagyagroup.com, the API at sms-api.saubhagyagroup.com and the
          Saubhagya SMS Android app. The Service is based on the open source
          httpSMS project and is hosted on servers we operate. This policy
          explains what information we collect, why, and how it is handled.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">Who the Service is for</h2>
        <p class="text-body-large mb-4">
          The Service is used by Saubhagya Group and its authorised team members
          to send and receive SMS messages through Android phones they own or
          manage, for example to send notifications and reminders to our
          customers. It is not intended for children under 13.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">Information we collect</h2>
        <h3 class="text-title-large mt-6 mb-2">Account information</h3>
        <p class="text-body-large mb-4">
          When you sign in we receive your name, email address and profile photo
          from your sign-in method (Google or email and password, handled by
          Google Firebase Authentication). We create an API key for your account
          so your phone and other systems can use the Service.
        </p>

        <h3 class="text-title-large mt-6 mb-2">
          Information handled by the Android app
        </h3>
        <p class="text-body-large mb-2">
          The Android app turns your phone into an SMS gateway. To do this it
          needs, and uses, the following permissions and data:
        </p>
        <ul class="text-body-large pl-6 mb-4">
          <li>
            <strong>Send SMS</strong> &ndash; to send the messages you or your
            systems request through the Service, from the phone's SIM.
          </li>
          <li>
            <strong>Receive and read SMS / MMS</strong> &ndash; to forward
            messages received on the phone to your account so they appear in
            your dashboard and can trigger your webhooks. Message content,
            sender and recipient numbers, and timestamps are sent to our server.
          </li>
          <li>
            <strong>Read call log and phone state</strong> &ndash; only to
            detect missed calls on the gateway phone and report the caller's
            number and time to your account. We do not record call audio or read
            any other call history.
          </li>
          <li>
            <strong>Phone number and SIM details</strong> &ndash; to identify
            which phone and SIM a message was sent from or received on.
          </li>
          <li>
            <strong>Device status</strong> &ndash; periodic "heartbeat"
            information such as whether the app is online, battery level and
            charging state, so you can see if the gateway phone is working.
          </li>
          <li>
            <strong>Notifications and push token</strong> &ndash; a Firebase
            Cloud Messaging token so our server can tell the phone when there is
            a message to send.
          </li>
        </ul>
        <p class="text-body-large mb-4">
          The app only processes SMS and call information on the phones where
          you install it and log in with your API key. It does not access
          contacts, photos, location or other files.
        </p>

        <h3 class="text-title-large mt-6 mb-2">Technical information</h3>
        <p class="text-body-large mb-4">
          Our servers keep standard logs (such as IP address, request time and
          errors) to keep the Service secure and working.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">How we use information</h2>
        <ul class="text-body-large pl-6 mb-4">
          <li>To send and receive SMS messages on your behalf;</li>
          <li>
            To show message history, phone status and missed calls in your
            dashboard and send them to webhooks you configure;
          </li>
          <li>To authenticate you and secure your account;</li>
          <li>To operate, troubleshoot and improve the Service.</li>
        </ul>
        <p class="text-body-large mb-4">
          We do not sell your information and we do not use message content for
          advertising.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">Service providers</h2>
        <p class="text-body-large mb-2">
          We use a small number of providers to run the Service:
        </p>
        <ul class="text-body-large pl-6 mb-4">
          <li>
            <a
              href="https://firebase.google.com/support/privacy"
              target="_blank"
              rel="noopener"
              >Google Firebase</a
            >
            &ndash; sign-in, push notifications to the Android app, and basic
            app analytics.
          </li>
          <li>
            <a
              href="https://www.cloudflare.com/privacypolicy/"
              target="_blank"
              rel="noopener"
              >Cloudflare</a
            >
            &ndash; secure network delivery for our web addresses.
          </li>
        </ul>
        <p class="text-body-large mb-4">
          SMS content and phone data are stored on servers operated by Saubhagya
          Group. Mobile network operators carry the SMS messages themselves, as
          with any SMS.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">
          Storage, retention and deletion
        </h2>
        <p class="text-body-large mb-4">
          Data is sent over encrypted (HTTPS) connections and kept only as long
          as needed to provide the Service to your account. You can delete
          messages and phones from the dashboard, and you can ask us to delete
          your account and all its data by emailing
          <a
            class="text-decoration-none font-weight-bold"
            :href="`mailto:${contactEmail}`"
            >{{ contactEmail }}</a
          >. Uninstalling the Android app stops all SMS and call processing on
          that phone.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">Security</h2>
        <p class="text-body-large mb-4">
          We use reasonable technical measures to protect your information,
          including encrypted connections, access controls and API keys. No
          method of transmission or storage is completely secure, so we cannot
          guarantee absolute security.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">Changes to this policy</h2>
        <p class="text-body-large mb-4">
          We may update this policy. Changes are posted on this page with a new
          effective date.
        </p>

        <h2 class="text-headline-medium mt-8 mb-4">Contact us</h2>
        <p class="text-body-large mb-10">
          Questions about this policy or your data:
          <a
            class="text-decoration-none font-weight-bold"
            :href="`mailto:${contactEmail}`"
            >{{ contactEmail }}</a
          >
          &middot; Saubhagya Group.
        </p>
        <VDivider class="mx-16" />
        <div class="text-center mt-8 mb-4">
          <BackButton />
        </div>
      </VCol>
    </VRow>
  </VContainer>
</template>

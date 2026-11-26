# Publishing Saubhagya SMS to Google Play (internal testing)

Internal testing lets up to 100 people install the app from the Play Store,
with no "App not installed" / Play Protect block and with automatic updates.

| Item | Value |
| --- | --- |
| Package name | `com.saubhagyagroup.sms` |
| App name | Saubhagya SMS |
| Upload file | `Saubhagya-SMS-release.aab` (Android App Bundle) |
| Signing | Upload key = `~/.android-keys/saubhagya-sms-release.jks` (alias `saubhagya-sms`). Let Google manage the app signing key (Play App Signing). |
| Privacy policy | https://sms.saubhagyagroup.com/privacy-policy |
| Support email | support@saubhagyagroup.com |
| Website | https://sms.saubhagyagroup.com |

## 1. Developer account (one time)

1. Go to https://play.google.com/console/signup and sign in with the Google
   account that should own the app.
2. Choose **Organization** (Saubhagya Group). You will need the organisation's
   D-U-N-S number, a contact phone and email, and the one-time US$25 fee.
3. Complete identity / organisation verification. Google can take a few days.

## 2. Create the app

Play Console → **Create app**:

- App name: **Saubhagya SMS**
- Default language: English
- App or game: **App** · Free or paid: **Free**
- Tick the declarations → **Create app**.

## 3. Internal testing release

Testing → **Internal testing** → **Testers**: create an email list with the
team's Google accounts → **Create new release**:

1. Signing: accept **Google-managed app signing key**.
2. Upload `Saubhagya-SMS-release.aab`.
3. Release name: `1 (initial)` · Release notes: `Initial internal release.`
4. **Save** → **Review release** → **Start rollout to Internal testing**.
5. Copy the **opt-in link** from the Testers tab and send it to testers; they
   accept it on their phone and install from the Play Store.

For each later update, increase `versionCode` in `app/build.gradle.kts`, run
`./gradlew bundleRelease`, and upload the new `.aab`.

## 4. App content (Policy → App content)

**Privacy policy:** https://sms.saubhagyagroup.com/privacy-policy

**App access:** "All or some functionality is restricted" → give reviewers
instructions: _Sign in at https://sms.saubhagyagroup.com, copy the API key
from Settings, paste it into the app. Server URL is pre-filled._ (Create a
reviewer account and give its API key here.)

**Ads:** No ads.

**Content rating:** Category _Utility, Productivity, Communication, or Other_.
Answer "No" to all violence / sexual / gambling questions. Users interact /
exchange content: **No** (messages go to the user's own customers via their own SIM).

**Target audience:** 18 and over. Not designed for children.

**Data safety:**

| Data type | Collected | Shared | Purpose | Optional? |
| --- | --- | --- | --- | --- |
| Personal info → Name, Email address | Yes | No | Account management | Required |
| Personal info → Phone number | Yes | No | App functionality | Required |
| Messages → SMS or MMS | Yes | No | App functionality | Required |
| App activity → Other actions (missed-call events) | Yes | No | App functionality | Required |
| App info and performance → Diagnostics | Yes | No | Analytics | Required |
| Device or other IDs (push token) | Yes | No | App functionality | Required |

- Data is encrypted in transit: **Yes**
- Users can request data deletion: **Yes** (email support@saubhagyagroup.com)

**Sensitive permissions — SMS and Call Log permissions form.**
The app uses `SEND_SMS`, `READ_SMS`, `RECEIVE_SMS`, `RECEIVE_MMS`, `READ_CALL_LOG`.
These are restricted on Play; you must submit the declaration form.

- Core functionality: **SMS-based gateway / device automation**
  (choose the closest listed use, e.g. "Device automation" or
  "Cross-device synchronization or transfer of SMS or calls").
- Description to paste:

  > Saubhagya SMS turns a company-owned Android phone into an SMS gateway for
  > Saubhagya Group. Our web dashboard and API ask the phone to send SMS
  > (SEND_SMS) to our customers, and incoming SMS (RECEIVE_SMS, READ_SMS,
  > RECEIVE_MMS) are forwarded to the same user's dashboard and webhooks so
  > replies can be handled. READ_CALL_LOG is used only to report missed calls
  > on the gateway phone to the user's dashboard. Sending and receiving SMS
  > is the app's core and only purpose; without these permissions it cannot work.

- Video: record a 30–60 s screen recording showing login, a message sent from
  the dashboard arriving via the phone, and a reply appearing in the dashboard.

> Note: Google reviews this form even for internal testing. If it is
> rejected, you can still use internal testing without `READ_CALL_LOG`
> (missed-call reporting) — remove that permission and re-upload.

## 5. Store listing (Grow → Store presence → Main store listing)

- Short description (≤80): `SMS gateway for Saubhagya Group — send and receive SMS from your Android phone.`
- Full description:

  > Saubhagya SMS connects an Android phone to the Saubhagya Group SMS
  > dashboard (sms.saubhagyagroup.com). Messages sent from the dashboard or API
  > are delivered from the phone's SIM, and replies and missed calls are sent
  > back to the dashboard and your webhooks.
  >
  > • Send SMS from your own phone number via a simple API
  > • Receive replies in the dashboard and via webhooks
  > • Phone health (online status, battery) at a glance
  >
  > For use by Saubhagya Group and its authorised team members.

- App icon (512×512 PNG), feature graphic (1024×500), at least 2 phone
  screenshots — take screenshots of the login screen and main screen.

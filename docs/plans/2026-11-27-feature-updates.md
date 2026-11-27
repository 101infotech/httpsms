# Feature updates — 27 Nov 2026

Planned by Claude, implemented by DeepSeek (opencode), verified by Claude.
Stack: Go API in `api/` (Fiber + GORM, optional Mongo for contacts), Nuxt 4 +
Vuetify 4 dashboard in `web/`. Keep the Saubhagya UI style (see
`web/app/assets/css/saubhagya.css`, cream canvas, maroon primary, pill chips,
borderless cards). Every change must keep `go build ./...`, `go test ./...`,
`pnpm lint` and `pnpm test` green.

---

## 1. Name each phone number

Today a phone is shown only as its number (thread picker, settings, API-key
table, heartbeats). Users want a friendly name, e.g. "SSIT front desk".

### API
- `api/pkg/entities/phone.go`: add
  `Name *string \`json:"name" example:"SSIT front desk" validate:"optional"\``
  (nullable, so AutoMigrate adds a column without breaking existing rows).
- `api/pkg/requests/phone_update_request.go`: accept optional `name`
  (trim; empty string clears it). Map to a new `Name *string` field on
  `services.PhoneUpsertParams` (`api/pkg/services/phone_service.go`) and
  persist it in `Upsert` the same way `MissedCallAutoReply` is handled.
- `api/pkg/validators/...` phone update validation: `name` max 50 chars.
- Update `phone_update_request_test.go` for the new field.

### Web
- `web/shared/types/api.ts`: add `name?: string | null` to `EntitiesPhone`
  (the file is generated; edit by hand to match).
- `web/app/stores/phones.ts`: send `name` in the PUT payload; add a helper
  `phoneLabel(phone)` → `name ? \`${name} · ${formatted number}\` : formatted number`.
- Settings page phone editor (`web/app/pages/settings/index.vue`, the dialog
  that edits `activePhone`): add a "Phone name" text field at the top.
- Use the label everywhere a phone is listed: thread owner picker
  (`components/MessageThreadHeader.vue` `owners` items: title = name, subtitle
  = number), API-keys table (`pages/phone-api-keys/index.vue`: show name above
  number), heartbeats page title, new-message/bulk-send "from" pickers.

---

## 2. Missed calls in the dashboard

Already exists: the Android app posts missed calls to
`POST /v1/messages/calls/missed`; they are stored as messages with
`type = "call/missed"`; the `message.call.missed` webhook event and the
per-phone `missed_call_auto_reply` setting exist. Missing: a place to see them.

### Web only (no API change needed)
- New page `web/app/pages/calls/index.vue` ("Missed calls", auth middleware):
  - Data: `GET /v1/messages/search?types=call/missed&owners=<all phone numbers>&sort_by=order_timestamp&sort_descending=true&limit=100&skip=…`
    (check `api/pkg/requests/message_search_request.go` for exact params).
  - Phone filter chips (All + one per phone, using `phoneLabel`), a list of
    cards: caller (contact name if known, else number), which phone/SIM
    received it, relative time, and actions: "Open conversation"
    (`/threads/<thread id>` if available, else `/messages?to=<number>`) and
    "Call back" (`tel:` link).
  - Header summary: count today / last 7 days.
  - Empty state in the Saubhagya style (lotus badge + "No missed calls").
  - "Load more" pagination.
- Live update: subscribe to the existing Pusher `message.call.missed` event
  (see `pages/threads/[id]/index.vue` for the pattern) and prepend.
- Nav: add "Missed calls" (icons `mdiPhoneMissedOutline` / `mdiPhoneMissed`) to
  `components/AppNav.vue` primary items, after Messages. On the phone tab bar
  keep 5 tabs (replace Search with Missed calls).
- Settings: next to each phone's missed-call auto reply field, add a one-line
  hint that missed calls also appear on the Missed calls page and fire the
  `message.call.missed` webhook.

---

## 3. Contact groups + bulk send to groups / typed numbers

Today bulk send only accepts a CSV/Excel upload (`POST /v1/bulk-messages`).
The API already has `POST /v1/messages/bulk-send` (`from`, `to[]` max 1000,
`content`).

### Groups as contact labels (works for both GORM and Mongo backends)
- `api/pkg/entities/contact.go`: add
  `Groups pq.StringArray \`json:"groups" gorm:"type:text[]" bson:"groups" swaggertype:"array,string" example:"Students,Staff"\``.
- Contact store/update requests + service + both repositories: accept and save
  `groups` (trimmed, de-duplicated, max 20 per contact, each max 50 chars).
- `GET /v1/contacts` (`requests/contact_index.go`): optional `group` filter
  (exact match). GORM: `? = ANY(groups)`; Mongo: `{"groups": group}`.
- New `GET /v1/contacts/groups` → `[{ "name": "Students", "count": 42 }]`
  for the user (GORM: `unnest(groups)` + group by; Mongo: aggregate).
- New `PUT /v1/contacts/groups/:name` body `{ "name": "New name" }` renames the
  label on all the user's contacts; `DELETE /v1/contacts/groups/:name` removes
  the label from them (contacts are kept). Register routes in
  `handlers/contact_handler.go` before `/v1/contacts/:contactID`.
- Tests for the new repository/service functions next to the existing ones.

### Web: contacts
- `stores/contacts.ts`: `loadGroups()`, `renameGroup()`, `deleteGroup()`,
  `group` filter on `loadContacts`.
- `pages/contacts/index.vue` + `components/ContactDialog.vue`: groups combobox
  (multiple, chips, can create new) on the contact form; a group chip filter row
  above the contact list with counts; rename/delete actions in a group menu.

### Web: bulk send
- `pages/bulk-messages/index.vue`: two tabs — **Compose** (new, default) and
  **Upload file** (existing CSV/Excel flow, unchanged).
- Compose tab:
  - "From" phone select (`phoneLabel`).
  - Recipients: multi-select of contact groups (shows count), plus a textarea
    "Or type numbers" accepting commas, spaces or new lines.
  - Resolve: fetch all contacts of the chosen groups (page through
    `GET /v1/contacts?group=…`), take each contact's phone numbers, merge with
    typed numbers, normalise to E.164 with `libphonenumber-js` (default region
    from the selected phone), drop invalid ones and de-duplicate.
  - Show a summary pill: "128 recipients · 3 invalid skipped" with the invalid
    list expandable.
  - Message textarea with character + SMS-part counter.
  - Send: `POST /v1/messages/bulk-send` in chunks of 1000 with the same
    `request_id` prefix (`bulk-compose-<timestamp>`), show progress, then a
    success toast and link to Search filtered by that request.
  - Confirm dialog before sending ("Send to 128 recipients from SSIT front desk?").

---

## Out of scope
- Android app changes (it already reports missed calls).
- Changing message billing/limits.

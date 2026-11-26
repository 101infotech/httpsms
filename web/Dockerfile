# build stage
FROM node:24-alpine AS build

WORKDIR /app

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

# Install pnpm
RUN npm install -g pnpm

RUN pnpm install

COPY . .

# Optional build-time config (used by Dokploy). Only non-empty values are exported,
# so a local web/.env still applies when these args are not passed.
ARG API_BASE_URL APP_URL APP_NAME CLOUDFLARE_TURNSTILE_SITE_KEY PUSHER_KEY PUSHER_CLUSTER \
    FIREBASE_API_KEY FIREBASE_AUTH_DOMAIN FIREBASE_PROJECT_ID FIREBASE_STORAGE_BUCKET \
    FIREBASE_MESSAGING_SENDER_ID FIREBASE_APP_ID FIREBASE_MEASUREMENT_ID
RUN for v in API_BASE_URL APP_URL APP_NAME CLOUDFLARE_TURNSTILE_SITE_KEY PUSHER_KEY PUSHER_CLUSTER \
      FIREBASE_API_KEY FIREBASE_AUTH_DOMAIN FIREBASE_PROJECT_ID FIREBASE_STORAGE_BUCKET \
      FIREBASE_MESSAGING_SENDER_ID FIREBASE_APP_ID FIREBASE_MEASUREMENT_ID; do \
      eval "val=\${$v}"; [ -z "$val" ] && unset "$v"; \
    done; \
    pnpm run generate

# production stage
FROM nginx:stable-alpine AS production
COPY --from=build /app/.output/public /usr/share/nginx/html

# Copy the nginx configuration file
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]

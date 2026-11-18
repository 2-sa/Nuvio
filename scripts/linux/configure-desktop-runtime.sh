#!/usr/bin/env bash

set -euo pipefail

optional_values=(
    LOCAL_PROPERTIES_BASE64
    SENTRY_AUTH_TOKEN
    SENTRY_DESKTOP_DSN
)
missing=()
for value_name in "${optional_values[@]}"; do
    if [[ -z "${!value_name:-}" ]]; then
        missing+=("${value_name}")
    fi
done
if (( ${#missing[@]} > 0 )); then
    printf 'Notice: optional desktop release values not set: %s\n' "${missing[*]}"
fi

if [[ -n "${LOCAL_PROPERTIES_BASE64:-}" ]]; then
    printf '%s' "${LOCAL_PROPERTIES_BASE64}" | base64 --decode > local.properties
    sed -i -E '/^[[:space:]]*(sdk\.dir|NUVIO_RELEASE_(STORE_FILE|STORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD)|NUVIO_MACOS_(SIGNING_IDENTITY|NOTARY_APPLE_ID|NOTARY_TEAM_ID|NOTARY_PASSWORD|NOTARY_KEYCHAIN_PROFILE|NOTARY_KEYCHAIN_PATH))[[:space:]]*=/d' local.properties
else
    echo "Notice: LOCAL_PROPERTIES_BASE64 not set; using empty local.properties."
    : > local.properties
fi

optional_properties=(
    NUVIO_SUPABASE_URL
    NUVIO_SUPABASE_ANON_KEY
    TRAKT_CLIENT_ID
    TRAKT_CLIENT_SECRET
)
for property_name in "${optional_properties[@]}"; do
    if ! grep -Eq "^${property_name}=.+" local.properties; then
        echo "Warning: optional desktop property not set in local.properties: ${property_name}"
    fi
done

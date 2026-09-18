#!/bin/sh
# Certbot initialization script - generates initial certificates if they don't exist,
# then runs the renewal loop.

set -e

CERTBOT_DIR="/etc/letsencrypt/live"
WEBROOT="/var/www/certbot"

# Define domains to manage
DOMAINS=("api.kuchimittai.com" "api.trynat.com")

echo "Starting Certbot initialization..."

# Generate initial certificates for each domain if they don't exist
for domain in "${DOMAINS[@]}"; do
    CERT_PATH="$CERTBOT_DIR/$domain/fullchain.pem"

    if [ ! -f "$CERT_PATH" ]; then
        echo "Certificate not found for $domain. Generating..."
        certbot certonly \
            --webroot \
            -w "$WEBROOT" \
            -d "$domain" \
            --non-interactive \
            --agree-tos \
            -m operations@trynat.com \
            || echo "Warning: Failed to generate certificate for $domain (might already exist or domain DNS not configured)"
    else
        echo "Certificate already exists for $domain"
    fi
done

# Run the renewal loop
echo "Starting certificate renewal loop..."
trap exit TERM
while :; do
    echo "Running certbot renew..."
    certbot renew --webroot -w "$WEBROOT" --quiet
    sleep 12h
done


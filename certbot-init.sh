#!/bin/sh
# Certbot initialization script - generates initial certificates if they don't exist,
# then runs the renewal loop.

CERTBOT_DIR="/etc/letsencrypt/live"
WEBROOT="/var/www/certbot"

echo "Starting Certbot initialization..."

# Generate initial certificates for each domain if they don't exist
# Using space-separated string instead of array (POSIX-compatible)
DOMAINS="api.kuchimittai.com api.trynat.com"

for domain in $DOMAINS; do
    CERT_PATH="$CERTBOT_DIR/$domain/fullchain.pem"

    if [ ! -f "$CERT_PATH" ]; then
        echo "Certificate not found for $domain. Generating..."
        certbot certonly \
            --webroot \
            -w "$WEBROOT" \
            -d "$domain" \
            --non-interactive \
            --agree-tos \
            -m operations@trynat.com || true
        echo "Certificate generation attempted for $domain"
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


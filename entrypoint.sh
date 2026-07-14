#!/bin/sh
# Fix ownership of the volume-mounted logs directory at container startup.
# This runs as root so it can correct host-created directory permissions,
# then drops to the non-root appuser before launching the JVM.
chown -R appuser:appgroup /app/logs 2>/dev/null || true

exec su-exec appuser java $JAVA_OPTS -jar /app/app.jar


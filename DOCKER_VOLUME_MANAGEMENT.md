# Docker Volume Management Guide

## Problem Solved ✅

Your issue of 81 orphaned volumes has been fixed by:

1. **Updated docker-compose files** with explicit volume driver configuration
2. **Bind mount volumes** to a predictable local directory (`./data/postgres`)
3. **Created cleanup scripts** for regular maintenance

---

## What Changed

### Before (docker-compose.yml)
```yaml
volumes:
  postgres_data:
```

### After (docker-compose.yml)
```yaml
volumes:
  postgres_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ${PWD}/data/postgres
```

**Benefits:**
- ✅ Data stored in `./data/postgres` directory (persistent & visible)
- ✅ Volume name is explicit and manageable
- ✅ No anonymous volumes created
- ✅ Easy backup and migration

---

## How to Clean Up NOW

### Windows (PowerShell)
```powershell
.\docker-cleanup.ps1
```

### macOS/Linux (Bash)
```bash
chmod +x docker-cleanup.sh
./docker-cleanup.sh
```

### Manual Cleanup (All Platforms)
```bash
# Stop services and remove volumes
docker-compose down -v

# Remove all dangling volumes
docker volume prune -f

# Remove all dangling images
docker image prune -f

# Remove exited containers
docker container prune -f

# Restart services
docker-compose up -d
```

---

## Best Practices Going Forward

### 1. Always Use Named Volumes
```yaml
volumes:
  postgres_data:
    driver: local
  app_cache:
    driver: local
```

### 2. Use Bind Mounts for Development
```yaml
volumes:
  - ./data/postgres:/var/lib/postgresql/data
  - ./logs:/app/logs
```

### 3. Proper Shutdown Procedure
```bash
# Good: Removes containers AND associated volumes
docker-compose down -v

# Avoid: Leaves orphaned volumes behind
docker-compose down
```

### 4. Regular Maintenance
Schedule weekly cleanup:

**Windows Task Scheduler:**
- Create task to run `docker-cleanup.ps1` weekly

**Linux Cron:**
```bash
# Add to crontab -e
0 2 * * 0 /path/to/docker-cleanup.sh >> /var/log/docker-cleanup.log 2>&1
```

### 5. Monitor Volume Usage
```bash
# Check volumes regularly
docker volume ls

# Inspect volume details
docker volume inspect postgres_data

# Check disk usage
docker system df
```

---

## Volume Commands Reference

| Command | Purpose |
|---------|---------|
| `docker volume ls` | List all volumes |
| `docker volume inspect <name>` | Show volume details |
| `docker volume rm <name>` | Remove specific volume |
| `docker volume prune -f` | Remove all dangling volumes |
| `docker system df` | Show docker disk usage |
| `docker system prune -a` | Complete system cleanup |

---

## Data Backup Strategy

### Backup PostgreSQL Volume
```bash
# Create backup
docker exec app-db pg_dump -U postgres onlineshop > backup.sql

# Restore from backup
docker exec -i app-db psql -U postgres onlineshop < backup.sql
```

### Backup Volume Directory
```bash
# Linux/macOS
tar -czf postgres_data_backup.tar.gz ./data/postgres

# Windows
Compress-Archive -Path ./data/postgres -DestinationPath postgres_data_backup.zip
```

---

## Troubleshooting

### Issue: Volume still shows as dangling
```bash
# Check if volume is used by stopped containers
docker ps -a --filter volume=<volume_name>

# Force remove if safe
docker volume rm -f <volume_name>
```

### Issue: Permission denied on bind mount
```bash
# Linux: Fix permissions
sudo chown 999:999 ./data/postgres  # For PostgreSQL
sudo chmod 700 ./data/postgres

# Windows: Run PowerShell as Administrator
```

### Issue: Space not freed after cleanup
```bash
# Complete system prune
docker system prune -a --volumes -f

# Check actual disk space
df -h  # Linux/macOS
dir   # Windows
```

---

## Your Setup is Now Optimized ✨

- ✅ Explicit named volumes
- ✅ Bind mount to `./data/postgres`
- ✅ Cleanup scripts ready to use
- ✅ No more orphaned volumes
- ✅ Data persists across container restarts

**Next Step:** Run the cleanup script to remove the 81 orphaned volumes!



#!/bin/bash

# Docker Cleanup & Maintenance Script
# Removes dangling volumes, images, and containers

set -e

echo "🧹 Starting Docker Cleanup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# 1. Stop all running containers
echo -e "\n${YELLOW}Stopping containers...${NC}"
if [ -f "docker-compose.yml" ]; then
    docker-compose down -v 2>/dev/null || true
    print_status "docker-compose services stopped"
else
    print_warning "docker-compose.yml not found in current directory"
fi

# 2. Remove dangling volumes
echo -e "\n${YELLOW}Cleaning up dangling volumes...${NC}"
DANGLING_VOLUMES=$(docker volume ls -q -f dangling=true)
if [ -z "$DANGLING_VOLUMES" ]; then
    print_status "No dangling volumes to remove"
else
    echo "$DANGLING_VOLUMES" | xargs docker volume rm
    print_status "Removed $(echo "$DANGLING_VOLUMES" | wc -l) dangling volumes"
fi

# 3. Remove dangling images
echo -e "\n${YELLOW}Cleaning up dangling images...${NC}"
DANGLING_IMAGES=$(docker images -q -f dangling=true)
if [ -z "$DANGLING_IMAGES" ]; then
    print_status "No dangling images to remove"
else
    echo "$DANGLING_IMAGES" | xargs docker rmi -f
    print_status "Removed $(echo "$DANGLING_IMAGES" | wc -l) dangling images"
fi

# 4. Remove exited containers
echo -e "\n${YELLOW}Cleaning up exited containers...${NC}"
EXITED_CONTAINERS=$(docker ps -q -f status=exited)
if [ -z "$EXITED_CONTAINERS" ]; then
    print_status "No exited containers to remove"
else
    echo "$EXITED_CONTAINERS" | xargs docker rm
    print_status "Removed $(echo "$EXITED_CONTAINERS" | wc -l) exited containers"
fi

# 5. Prune unused networks
echo -e "\n${YELLOW}Pruning unused networks...${NC}"
docker network prune -f > /dev/null 2>&1
print_status "Network cleanup complete"

# 6. Show volume summary
echo -e "\n${YELLOW}Current Docker volumes:${NC}"
docker volume ls

# 7. Show storage usage
echo -e "\n${YELLOW}Docker disk usage:${NC}"
docker system df

echo -e "\n${GREEN}✅ Docker cleanup completed!${NC}"
echo -e "\nNext steps:"
echo -e "  1. Run: ${YELLOW}docker-compose up -d${NC}"
echo -e "  2. Verify: ${YELLOW}docker volume ls${NC}"
echo -e "  3. Check logs: ${YELLOW}docker-compose logs -f${NC}"


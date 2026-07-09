version: '3.9'

services:
  db:
    image: postgres:15
    restart: unless-stopped
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: onlineshopping
      POSTGRES_DB: onlineshop
      TZ: Asia/Kolkata
      PGTZ: Asia/Kolkata
    volumes:
      - postgres_data:/var/lib/postgresql/data   # <== FIXED
    healthcheck:
          test: ["CMD-SHELL", "pg_isready -U postgres"]
          interval: 10s
          timeout: 5s
          retries: 5
    networks:
      - mynetwork


  app:
    build: .
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/onlineshop
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: onlineshopping
    ports:
      - "8080:8080"

    volumes:
      - ./logs:/app/logs
    networks:
      - mynetwork

volumes:
  postgres_data:


networks:
  mynetwork:


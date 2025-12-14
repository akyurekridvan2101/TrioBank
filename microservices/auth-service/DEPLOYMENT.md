# Auth Service Deployment Guide

## 🚀 Quick Start

### Development (Mevcut Yapı)
1. **Start infrastructure:**
   ```bash
   docker-compose up -d
   ```

2. **Run auth service locally:**
   ```bash
   go run cmd/main.go
   ```

### Full Docker Deployment
1. **Update config/.env** with your credentials
2. **Build and run everything:**
   ```bash
   docker-compose --profile full-stack up --build -d
   ```

## 📋 Environment Variables

Copy `config/.env.example` to `config/.env` and update:

```bash
cp config/.env.example config/.env
```

**Required variables:**
- `MONGO_USERNAME` - MongoDB admin username
- `MONGO_PASSWORD` - MongoDB admin password  
- `REDIS_PASSWORD` - Redis password
- `SECRET_KEY` - Internal service secret
- `TOKEN_SIGNATURE` - JWT signing key

## 🔒 Database Credentials

Default credentials (CHANGE IN PRODUCTION):
- **MongoDB:** `admin` / `changeme123`
- **Redis:** `changeme456`

## 🏗️ Deployment Options

### Option 1: VPS (DigitalOcean, AWS EC2, Hetzner)
```bash
# On your VPS
git clone <your-repo>
cd triobank/microservices/auth-service

# Set environment variables
nano config/.env

# Start with Docker Compose
docker-compose --profile full-stack up -d

# Setup Nginx reverse proxy for HTTPS
# Point your Cloudflare domain to VPS IP
```

### Option 2: Cloud Platforms

**Railway.app:**
1. Connect GitHub repo
2. Add environment variables in dashboard
3. Deploy automatically

**Render.com:**
1. Create new Web Service
2. Connect repo
3. Use Dockerfile
4. Add env vars

**Fly.io:**
```bash
flyctl launch
flyctl secrets set MONGO_PASSWORD=xxx REDIS_PASSWORD=yyy
flyctl deploy
```

## 🌐 Cloudflare Setup

1. **DNS Settings:**
   - A record: `api.yourdomain.com` → VPS IP
   - Proxy: ON (orange cloud)

2. **SSL/TLS:**
   - Mode: Full
   - Edge certificates: Auto

3. **Firewall Rules:** (optional)
   - Allow only your IP for admin endpoints

## ✅ Health Check

```bash
curl http://localhost:8080/health
```

## 📦 Build Docker Image

```bash
docker build -t auth-service:latest .
docker run -p 8080:8080 --env-file config/.env auth-service:latest
```

## 🔄 Updates

```bash
git pull
docker-compose --profile full-stack up --build -d
```

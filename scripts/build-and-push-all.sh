#!/bin/bash

# TrioBank - Tüm Servisleri Build ve Push Script
# Kullanım: ./scripts/build-and-push-all.sh [service-name]
# Service name belirtilmezse tüm servisleri build eder ve push eder

set -e

# Renkler
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Registry bilgileri
REGISTRY="docker.io"
DOCKER_USERNAME="akyurekridvan2101"
TAG="${TAG:-latest}"

# Docker login kontrolü
check_docker_login() {
    echo -e "${BLUE}Checking Docker login status...${NC}"
    if ! docker info | grep -q "Username: $DOCKER_USERNAME"; then
        echo -e "${YELLOW}Docker login required. Please login to Docker Hub:${NC}"
        docker login -u "$DOCKER_USERNAME"
    else
        echo -e "${GREEN}Already logged in to Docker Hub${NC}"
    fi
}

# Tek bir servisi build ve push et
build_and_push_service() {
    local service_name=$1
    local service_dir="services/$service_name"
    local image_name="$DOCKER_USERNAME/$service_name:$TAG"
    
    if [ ! -d "$service_dir" ]; then
        echo -e "${RED}Error: Service directory not found: $service_dir${NC}"
        return 1
    fi
    
    if [ ! -f "$service_dir/Dockerfile" ]; then
        echo -e "${RED}Error: Dockerfile not found in $service_dir${NC}"
        return 1
    fi
    
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}Building $service_name...${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    cd "$service_dir"
    
    # Build
    echo -e "${YELLOW}Building image: $image_name${NC}"
    docker build -t "$image_name" -f Dockerfile .
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed for $service_name${NC}"
        cd - > /dev/null
        return 1
    fi
    
    # Push
    echo -e "${YELLOW}Pushing image: $image_name${NC}"
    docker push "$image_name"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Push failed for $service_name${NC}"
        cd - > /dev/null
        return 1
    fi
    
    echo -e "${GREEN}✓ Successfully built and pushed $image_name${NC}"
    cd - > /dev/null
}

# Main script
main() {
    local target_service=$1
    local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    cd "$project_root"
    
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}TrioBank Docker Build & Push${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "Registry: ${GREEN}$REGISTRY/$DOCKER_USERNAME${NC}"
    echo -e "Tag: ${GREEN}$TAG${NC}"
    echo ""
    
    # Docker login kontrolü
    check_docker_login
    
    # Eğer spesifik bir servis belirtilmişse sadece onu build et
    if [ -n "$target_service" ]; then
        echo -e "${BLUE}Building single service: $target_service${NC}"
        build_and_push_service "$target_service"
        exit $?
    fi
    
    # Tüm servisleri build et
    echo -e "${BLUE}Building all services...${NC}"
    echo ""
    
    # Servis listesi (build sırası önemli değil, paralel olabilir)
    services=(
        "auth-service"
        "account-service"
        "card-service"
        "client-service"
        "ledger-service"
        "mail-service"
        "transaction-service"
        "api-gateway"
    )
    
    failed_services=()
    
    for service in "${services[@]}"; do
        if ! build_and_push_service "$service"; then
            failed_services+=("$service")
        fi
        echo ""
    done
    
    # Sonuç özeti
    echo -e "${BLUE}========================================${NC}"
    if [ ${#failed_services[@]} -eq 0 ]; then
        echo -e "${GREEN}✓ All services built and pushed successfully!${NC}"
    else
        echo -e "${RED}✗ Some services failed:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "  ${RED}- $service${NC}"
        done
        exit 1
    fi
    echo -e "${BLUE}========================================${NC}"
}

# Script'i çalıştır
main "$@"


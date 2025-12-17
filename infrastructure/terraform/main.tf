# ==============================================================================
# Terraform Configuration - TrioBank Infrastructure
# AWS Provider ve temel yapılandırma
# ==============================================================================

# Terraform versiyonu
terraform {
  required_version = ">= 1.0"

  # Gerekli provider'lar
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"  # AWS provider 5.x versiyonu
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"  # TLS provider (Key Pair için)
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.0"  # Local provider (Private key dosyası için)
    }
  }

  # Backend yapılandırması (opsiyonel - state dosyası için)
  # Şimdilik lokal state kullanacağız, sonra S3'e taşıyabiliriz
  # backend "s3" {
  #   bucket = "triobank-terraform-state"
  #   key    = "terraform.tfstate"
  #   region = "eu-north-1"
  # }
}

# AWS Provider yapılandırması
provider "aws" {
  region = var.aws_region

  # Default tags - tüm kaynaklara otomatik eklenir
  default_tags {
    tags = {
      Project     = "TrioBank"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

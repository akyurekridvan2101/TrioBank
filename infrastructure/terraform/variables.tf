# ==============================================================================
# Terraform Variables - TrioBank Infrastructure
# Tüm değişkenler burada tanımlanır
# ==============================================================================

# AWS Region
variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "eu-north-1"
}

# Environment (dev, staging, prod)
variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"
}

# ==============================================================================
# EKS Variables
# ==============================================================================

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "triobank-cluster"
}

variable "kubernetes_version" {
  description = "Kubernetes version for EKS cluster"
  type        = string
  default     = "1.29"
}

variable "node_group_instance_type" {
  description = "EC2 instance type for EKS node group"
  type        = string
  default     = "t3.micro"  # Free tier eligible
}

variable "node_group_desired_size" {
  description = "Desired number of nodes in EKS node group"
  type        = number
  default     = 2
}

variable "node_group_min_size" {
  description = "Minimum number of nodes in EKS node group"
  type        = number
  default     = 1
}

variable "node_group_max_size" {
  description = "Maximum number of nodes in EKS node group"
  type        = number
  default     = 3
}

# ==============================================================================
# EC2 Variables
# ==============================================================================

variable "ec2_instance_type" {
  description = "EC2 instance type for databases"
  type        = string
  default     = "t3.small"  # 2GB RAM - MSSQL minimum gereksinim (Free Tier kısıtlaması nedeniyle)
  # NOT: Free Tier kısıtlaması nedeniyle t3.medium/t3.large Terraform ile oluşturulamıyor
  # AWS Console'dan manuel olarak instance type değiştirilebilir:
  # 1. EC2 Console → Instance → Stop
  # 2. Actions → Instance Settings → Change Instance Type → t3.medium/t3.large
  # 3. Start
}

variable "ec2_key_pair_name" {
  description = "Name of the EC2 Key Pair (will be created if not exists)"
  type        = string
  default     = "triobank-ec2-key"
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to SSH to EC2 (0.0.0.0/0 = everyone - test için)"
  type        = string
  default     = "0.0.0.0/0"  # Test için her yerden
}

# ==============================================================================
# VPC Variables (Opsiyonel - EKS otomatik oluşturur)
# ==============================================================================

variable "vpc_cidr" {
  description = "CIDR block for VPC (EKS otomatik oluşturur, bu opsiyonel)"
  type        = string
  default     = "10.0.0.0/16"
}

# ==============================================================================
# EC2 Instance - TrioBank Databases
# MSSQL, MongoDB, Redis ve Vault için EC2 instance
# ==============================================================================

# ==============================================================================
# 1. Security Group - EC2 için güvenlik duvarı
# ==============================================================================

resource "aws_security_group" "ec2_databases" {
  name        = "${var.cluster_name}-ec2-databases-sg"
  description = "Security group for TrioBank databases EC2 instance"
  vpc_id      = aws_vpc.main.id

  # SSH access (from anywhere for testing)
  ingress {
    description = "SSH from anywhere for testing"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  # MSSQL port (from VPC - EKS nodes)
  ingress {
    description = "MSSQL from VPC"
    from_port   = 1433
    to_port     = 1433
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # MongoDB port (from VPC - EKS nodes)
  ingress {
    description = "MongoDB from VPC"
    from_port   = 27017
    to_port     = 27017
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Redis port (from VPC - EKS nodes)
  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Vault port (from VPC - EKS nodes)
  ingress {
    description = "Vault from VPC"
    from_port   = 8200
    to_port     = 8200
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Tüm giden trafiğe izin ver
  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.cluster_name}-ec2-databases-sg"
  }
}

# ==============================================================================
# 2. Key Pair - SSH için anahtar çifti
# ==============================================================================

resource "aws_key_pair" "ec2_key" {
  key_name   = var.ec2_key_pair_name
  public_key = tls_private_key.ec2_key_pair.public_key_openssh

  tags = {
    Name = "${var.cluster_name}-ec2-key"
  }
}

# Key Pair için private key oluştur
resource "tls_private_key" "ec2_key_pair" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# Private key'i lokal dosyaya kaydet
resource "local_file" "ec2_private_key" {
  content         = tls_private_key.ec2_key_pair.private_key_pem
  filename        = "${var.ec2_key_pair_name}.pem"
  file_permission = "0400"  # Sadece okuma izni (güvenlik)
}

# ==============================================================================
# 3. EC2 Instance - Veritabanları için sunucu
# ==============================================================================

# En son Amazon Linux 2 AMI'yi bul
data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# EC2 Instance oluştur
resource "aws_instance" "databases" {
  ami           = data.aws_ami.amazon_linux_2.id
  instance_type = var.ec2_instance_type

  # Key Pair
  key_name = aws_key_pair.ec2_key.key_name

  # VPC ve Subnet (Public subnet'te - test için)
  subnet_id = aws_subnet.public_1.id

  # Security Group
  vpc_security_group_ids = [aws_security_group.ec2_databases.id]

  # Root volume (20 GB)
  root_block_device {
    volume_type = "gp3"
    volume_size = 20
    encrypted   = true
  }

  # User data - Instance başladığında çalışacak script
  # Docker ve Docker Compose'u otomatik kurar
  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              yum install docker -y
              systemctl start docker
              systemctl enable docker
              usermod -aG docker ec2-user
              
              # Docker Compose kurulumu
              curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              chmod +x /usr/local/bin/docker-compose
              EOF

  tags = {
    Name = "${var.cluster_name}-databases"
  }
}

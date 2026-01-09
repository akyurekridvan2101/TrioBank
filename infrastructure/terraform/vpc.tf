# ==============================================================================
# VPC - TrioBank Infrastructure
# EC2 ve EKS için ortak VPC
# ==============================================================================

# VPC oluştur
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.cluster_name}-vpc"
  }
}

# Internet Gateway (Public subnet için)
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.cluster_name}-igw"
  }
}

# Availability Zones (2 AZ kullanacağız)
data "aws_availability_zones" "available" {
  state = "available"
}

# Public Subnet 1 (AZ-a)
resource "aws_subnet" "public_1" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, 0)  # 10.0.0.0/24
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.cluster_name}-public-subnet-1"
    "kubernetes.io/role/elb" = "1"  # EKS için gerekli
  }
}

# Public Subnet 2 (AZ-b)
resource "aws_subnet" "public_2" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, 1)  # 10.0.1.0/24
  availability_zone       = data.aws_availability_zones.available.names[1]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.cluster_name}-public-subnet-2"
    "kubernetes.io/role/elb" = "1"  # EKS için gerekli
  }
}

# Private Subnet 1 (AZ-a) - EKS node'ları için
resource "aws_subnet" "private_1" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 10)  # 10.0.10.0/24
  availability_zone = data.aws_availability_zones.available.names[0]

  tags = {
    Name = "${var.cluster_name}-private-subnet-1"
    "kubernetes.io/role/internal-elb" = "1"  # EKS için gerekli
  }
}

# Private Subnet 2 (AZ-b) - EKS node'ları için
resource "aws_subnet" "private_2" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 11)  # 10.0.11.0/24
  availability_zone = data.aws_availability_zones.available.names[1]

  tags = {
    Name = "${var.cluster_name}-private-subnet-2"
    "kubernetes.io/role/internal-elb" = "1"  # EKS için gerekli
  }
}

# Route Table - Public Subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.cluster_name}-public-rt"
  }
}

# Route Table Association - Public Subnet 1
resource "aws_route_table_association" "public_1" {
  subnet_id      = aws_subnet.public_1.id
  route_table_id = aws_route_table.public.id
}

# Route Table Association - Public Subnet 2
resource "aws_route_table_association" "public_2" {
  subnet_id      = aws_subnet.public_2.id
  route_table_id = aws_route_table.public.id
}

# NAT Gateway (Private subnet'ler için internet erişimi)
# Test için basit tutuyoruz, public subnet kullanabiliriz
# Ama EKS node'ları private subnet'te olmalı (best practice)

# Elastic IP for NAT Gateway
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.cluster_name}-nat-eip"
  }

  depends_on = [aws_internet_gateway.main]
}

# NAT Gateway
resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public_1.id

  tags = {
    Name = "${var.cluster_name}-nat"
  }

  depends_on = [aws_internet_gateway.main]
}

# Route Table - Private Subnets
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "${var.cluster_name}-private-rt"
  }
}

# Route Table Association - Private Subnet 1
resource "aws_route_table_association" "private_1" {
  subnet_id      = aws_subnet.private_1.id
  route_table_id = aws_route_table.private.id
}

# Route Table Association - Private Subnet 2
resource "aws_route_table_association" "private_2" {
  subnet_id      = aws_subnet.private_2.id
  route_table_id = aws_route_table.private.id
}


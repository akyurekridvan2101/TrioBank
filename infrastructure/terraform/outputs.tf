# ==============================================================================
# Terraform Outputs - TrioBank Infrastructure
# Oluşturulan kaynakların bilgileri
# ==============================================================================

# ==============================================================================
# EC2 Outputs
# ==============================================================================

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.databases.id
}

output "ec2_public_ip" {
  description = "EC2 instance public IP address"
  value       = aws_instance.databases.public_ip
}

output "ec2_private_ip" {
  description = "EC2 instance private IP address (EKS'ten bağlanmak için)"
  value       = aws_instance.databases.private_ip
}

output "ec2_ssh_command" {
  description = "SSH command to connect to EC2 instance"
  value       = "ssh -i ${var.ec2_key_pair_name}.pem ec2-user@${aws_instance.databases.public_ip}"
}

output "ec2_key_pair_name" {
  description = "EC2 Key Pair name"
  value       = aws_key_pair.ec2_key.key_name
}

output "ec2_private_key_path" {
  description = "Path to private key file"
  value       = "${var.ec2_key_pair_name}.pem"
  sensitive   = false
}

# ==============================================================================
# EKS Outputs
# ==============================================================================

output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_version" {
  description = "EKS cluster Kubernetes version"
  value       = aws_eks_cluster.main.version
}

output "kubectl_config_command" {
  description = "Command to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"
}

output "eks_node_group_arn" {
  description = "EKS node group ARN"
  value       = aws_eks_node_group.main.arn
}

# ==============================================================================
# VPC Outputs
# ==============================================================================

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "VPC CIDR block"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = [aws_subnet.public_1.id, aws_subnet.public_2.id]
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = [aws_subnet.private_1.id, aws_subnet.private_2.id]
}

# ==============================================================================
# Load Balancer Controller Outputs
# ==============================================================================

output "alb_controller_role_arn" {
  description = "AWS Load Balancer Controller IAM Role ARN (IRSA)"
  value       = aws_iam_role.alb_controller.arn
}

# ==============================================================================
# ArgoCD ve Deployment Hazırlık Bilgileri
# ==============================================================================

output "deployment_info" {
  description = "Deployment için gerekli bilgiler"
  value = {
    cluster_name     = aws_eks_cluster.main.name
    cluster_endpoint = aws_eks_cluster.main.endpoint
    region           = var.aws_region
    ec2_private_ip   = aws_instance.databases.private_ip
    vpc_id           = aws_vpc.main.id
  }
}

output "next_steps" {
  description = "Sonraki adımlar"
  value = <<-EOT
    1. kubectl config:
       ${aws_eks_cluster.main.name == "" ? "" : "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"}
    
    2. EC2'ye bağlan ve Docker Compose başlat:
       ${aws_instance.databases.public_ip == "" ? "" : "ssh -i ${var.ec2_key_pair_name}.pem ec2-user@${aws_instance.databases.public_ip}"}
    
    3. EC2 Private IP (EKS'ten bağlanmak için):
       ${aws_instance.databases.private_ip == "" ? "" : aws_instance.databases.private_ip}
    
    4. Load Balancer Controller kurulumu:
       - IAM Role ARN: ${aws_iam_role.alb_controller.arn}
       - Service Account: kube-system/aws-load-balancer-controller
    
    5. ArgoCD kurulumu:
       - kubectl create namespace argocd
       - kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
  EOT
}

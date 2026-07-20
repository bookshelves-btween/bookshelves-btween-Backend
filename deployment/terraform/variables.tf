variable "aws_region" {
  description = "AWS region in which the development server is created."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Short lowercase project name used in AWS resource names."
  type        = string
  default     = "bookshelf"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.project_name))
    error_message = "project_name must be 3-21 lowercase letters, digits, or hyphens and start with a letter."
  }
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod."
  }
}

variable "instance_type" {
  description = "EC2 instance type. t3.medium is the recommended development size for Spring Boot, MySQL, Redis, and Nginx together."
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size" {
  description = "Encrypted gp3 root volume size in GiB."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size >= 20 && var.root_volume_size <= 100
    error_message = "root_volume_size must be between 20 and 100 GiB."
  }
}

variable "admin_cidr" {
  description = "Optional public CIDR allowed to use SSH, such as 203.0.113.10/32. Leave empty to keep port 22 closed and use AWS Systems Manager Session Manager."
  type        = string
  default     = ""

  validation {
    condition     = var.admin_cidr == "" || can(cidrnetmask(var.admin_cidr))
    error_message = "admin_cidr must be empty or a valid IPv4 CIDR such as 203.0.113.10/32."
  }
}

variable "ssh_public_key" {
  description = "Optional OpenSSH public key. Both this and admin_cidr must be set to enable SSH. Never put a private key here."
  type        = string
  default     = ""
}

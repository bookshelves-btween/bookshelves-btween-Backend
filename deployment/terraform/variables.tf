variable "aws_region" {
  description = "AWS region in which the development server is created."
  type        = string
  default     = "ap-northeast-2"
}

variable "aws_profile" {
  description = "Local AWS CLI profile included in generated Session Manager commands. It does not configure the Terraform provider."
  type        = string
  default     = "bookshelf"
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
  description = "EC2 instance type. The development default runs Spring Boot, Redis, and Nginx; MySQL runs on RDS."
  type        = string
  default     = "t3.small"
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

variable "db_instance_class" {
  description = "Cost-optimized RDS MySQL instance class for the initial service workload."
  type        = string
  default     = "db.t4g.small"
}

variable "db_engine_version" {
  description = "RDS for MySQL major engine version. RDS selects a supported minor release."
  type        = string
  default     = "8.4"
}

variable "db_name" {
  description = "Initial application database name."
  type        = string
  default     = "bookshelf"

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]{0,63}$", var.db_name))
    error_message = "db_name must start with a letter and contain only letters, digits, or underscores."
  }
}

variable "db_master_username" {
  description = "RDS master username. Terraform generates the password and stores it only in state."
  type        = string
  default     = "bookshelf_admin"

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]{0,15}$", var.db_master_username))
    error_message = "db_master_username must start with a letter and be at most 16 letters, digits, or underscores."
  }
}

variable "db_allocated_storage" {
  description = "RDS gp3 storage size in GiB."
  type        = number
  default     = 20

  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 100
    error_message = "db_allocated_storage must be between 20 and 100 GiB."
  }
}

variable "db_backup_retention_days" {
  description = "Number of days that RDS automated backups are retained."
  type        = number
  default     = 7

  validation {
    condition     = var.db_backup_retention_days >= 1 && var.db_backup_retention_days <= 35
    error_message = "db_backup_retention_days must be between 1 and 35."
  }
}

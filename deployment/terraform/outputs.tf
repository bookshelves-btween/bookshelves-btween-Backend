output "instance_id" {
  description = "EC2 instance ID."
  value       = aws_instance.server.id
}

output "public_ip" {
  description = "Static Elastic IP address. Public IPv4 charges apply."
  value       = aws_eip.server.public_ip
}

output "bootstrap_url" {
  description = "Open this URL after cloud-init completes."
  value       = "http://${aws_eip.server.public_ip}"
}

output "ssm_session_command" {
  description = "Preferred shell access command. Requires the AWS CLI and Session Manager plugin."
  value       = "aws ssm start-session --target ${aws_instance.server.id} --region ${var.aws_region} --profile ${var.aws_profile}"
}

output "ssh_command" {
  description = "SSH command when admin_cidr and ssh_public_key are configured."
  value       = local.enable_ssh ? "ssh ubuntu@${aws_eip.server.public_ip}" : "SSH disabled; use Session Manager"
}

output "database_endpoint" {
  description = "Private RDS MySQL hostname. It is reachable only from resources allowed by the database security group."
  value       = aws_db_instance.database.address
}

output "database_port" {
  description = "RDS MySQL port."
  value       = aws_db_instance.database.port
}

output "database_name" {
  description = "Initial application database name."
  value       = aws_db_instance.database.db_name
}

output "database_username" {
  description = "RDS master username used by the development application."
  value       = aws_db_instance.database.username
}

output "database_password" {
  description = "Generated RDS password. Store it only in the server .env file and never commit it."
  value       = random_password.database.result
  sensitive   = true
}

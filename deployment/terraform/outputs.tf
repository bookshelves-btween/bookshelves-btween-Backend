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
  value       = "aws ssm start-session --target ${aws_instance.server.id} --region ${var.aws_region}"
}

output "ssh_command" {
  description = "SSH command when admin_cidr and ssh_public_key are configured."
  value       = local.enable_ssh ? "ssh ubuntu@${aws_eip.server.public_ip}" : "SSH disabled; use Session Manager"
}

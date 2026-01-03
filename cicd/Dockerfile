# Start from a Maven image which includes Java
FROM maven:3.9-eclipse-temurin-21

# Switch to root to install all necessary tools
USER root

# Install prerequisites, including curl
RUN apt-get update && apt-get install -y ca-certificates curl

# Install Docker CLI
RUN install -m 0755 -d /etc/apt/keyrings
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
RUN chmod a+r /etc/apt/keyrings/docker.gpg
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN apt-get update && apt-get install -y docker-ce-cli

# Install standalone docker-compose binary
RUN curl -L "https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
RUN chmod +x /usr/local/bin/docker-compose

# Drop root privileges after setup — least privilege principle
USER 1000
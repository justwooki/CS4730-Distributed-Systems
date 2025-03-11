# Variables
APP_NAME=prj3
DOCKER_IMAGE=$(APP_NAME):latest
DOCKER_COMPOSE_FILE=prj3-docker-compose-and-hostsfile/docker-compose-testcase-3.yml

# Default target
all: build docker-build docker-compose-up

# Compile the Java application
build:
	find src -name "*.java" > sources.txt
	javac -d out @sources.txt
	rm sources.txt


# Build the Docker image
docker-build:
	docker buildx build -t $(DOCKER_IMAGE) .

# Start the containers using Docker Compose
docker-compose-up:
	docker compose -f $(DOCKER_COMPOSE_FILE) up --build

# Stop and remove containers
docker-compose-down:
	docker compose -f $(DOCKER_COMPOSE_FILE) down

# Clean compiled Java files
clean:
	rm -rf out

# Clean everything (compiled files, containers, images)
clean-all: clean docker-compose-down
	docker rmi $(DOCKER_IMAGE)

.PHONY: all build docker-build docker-compose-up docker-compose-down clean clean-all

APP_NAME = prj4
DOCKER_IMAGE = $(APP_NAME):latest

all: build docker-build

build:
	find src -name "*.java" > sources.txt
	javac -d out @sources.txt
	rm sources.txt

docker-build:
	docker buildx build -t $(DOCKER_IMAGE) .

# Testcase 1
test1: all
	docker compose -f docker-compose-testcase-1.yml up --build

test1-down:
	docker compose -f docker-compose-testcase-1.yml down

# Testcase 2
test2: all
	docker compose -f docker-compose-testcase-2.yml up --build

test2-down:
	docker compose -f docker-compose-testcase-2.yml down

clean:
	rm -rf out

clean-all: clean
	docker rmi $(DOCKER_IMAGE)

.PHONY: all build docker-build test1 test1-down test2 test2-down clean clean-all

IMAGE_BOOTSTRAP = prj5-bootstrap
IMAGE_PEER = prj5-peer

.PHONY: all bootstrap peer clean test1, test1-down, test2, test2-down

all: bootstrap peer

bootstrap:
	docker build -f dockerfile-things/Dockerfile.bootstrap -t $(IMAGE_BOOTSTRAP) .

peer:
	docker build -f dockerfile-things/Dockerfile.peer -t $(IMAGE_PEER) .

clean:
	docker rmi $(IMAGE_BOOTSTRAP) $(IMAGE_PEER)

# Testcase 1
test1: all
	docker compose -f dockerfile-things/docker-compose-testcase-1.yml up --build

test1-down:
	docker compose -f dockerfile-things/docker-compose-testcase-1.yml down

# Testcase 2
test2: all
	docker compose -f dockerfile-things/docker-compose-testcase-2.yml up --build

test2-down:
	docker compose -f dockerfile-things/docker-compose-testcase-2.yml down
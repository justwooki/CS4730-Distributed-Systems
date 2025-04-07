IMAGE_BOOTSTRAP = prj5-bootstrap
IMAGE_PEER = prj5-peer
IMAGE_CLIENT = prj5-client

.PHONY: all bootstrap peer client clean test1, test1-down, test2, test2-down, test3, test3-down, test4, test4-down, test5, test5-down

all: bootstrap peer client

bootstrap:
	docker build -f dockerfile-things/Dockerfile.bootstrap -t $(IMAGE_BOOTSTRAP) .

peer:
	docker build -f dockerfile-things/Dockerfile.peer -t $(IMAGE_PEER) .

client:
	docker build -f dockerfile-things/Dockerfile.client -t $(IMAGE_CLIENT) .

clean:
	docker rmi $(IMAGE_BOOTSTRAP) $(IMAGE_PEER) $(IMAGE_CLIENT)

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

# Testcase 3
test3: all
	docker compose -f dockerfile-things/docker-compose-testcase-3.yml up --build

test3-down:
	docker compose -f dockerfile-things/docker-compose-testcase-3.yml down

# Testcase 4
test4: all
	docker compose -f dockerfile-things/docker-compose-testcase-4.yml up --build

test4-down:
	docker compose -f dockerfile-things/docker-compose-testcase-4.yml down

# Testcase 5
test5: all
	docker compose -f dockerfile-things/docker-compose-testcase-5.yml up --build

test5-down:
	docker compose -f dockerfile-things/docker-compose-testcase-5.yml down
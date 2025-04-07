# Project 5: DHT
Justin Kim

## Instructions

1. Open terminal.

2. Ensure the directory looks like this:
   ```
   .
   └── dockerfile-things
       ├── docker-compose-testcase-1.yml
       ├── docker-compose-testcase-2.yml
       ├── docker-compose-testcase-3.yml
       ├── docker-compose-testcase-4.yml
       ├── docker-compose-testcase-5.yml
       ├── objects1.txt
       ├── objects5.txt
       ├── objects10.txt
       ├── objects50.txt
       ├── objects66.txt
       ├── objects100.txt
       ├── objects126.txt
       ├── Dockerfile.bootstrap
       ├── Dockerfile.client
       └── Dockerfile.peer
   └── src
       ├── BootstrapServer
       ├── Client
       ├── Peer
       └── Utils
   ├── Makefile
   └── README.md
   ```

3. Ensure you are located in the directory with the Makefile. 

4. Run each test on the Makefile. See example for testcase 1 below:
   ```
   make test1
   ```
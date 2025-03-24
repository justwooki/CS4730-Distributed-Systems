# Project 4: Paxos
Justin Kim

## Instructions
1. Open terminal.
2. Ensure you are located in the directory with the
   Dockerfile.
3. Build the docker image
   ```
   docker build . -t prj4
   ```
4. Run Docker Compose, which should automatically run the container
   ```
   docker compose -f [insert docker compose file name] up
   ```
5. You can also run the program with the Makefile:
   
   Run the following to run test1:
   ```
   make test1
   ```
   Run the following to run test2:
   ```
   make test2
   ```
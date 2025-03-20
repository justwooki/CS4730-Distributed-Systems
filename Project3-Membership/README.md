# Project 3: Membership
Justin Kim

## Instructions
1. Open terminal.
2. Ensure you are located in the directory with the
   Dockerfile.
3. Build the docker image
```
docker build . -t prj3
```
4. Run Docker Compose, which should automatically run the container
```
docker compose -f docker-compose-and-hostsfile/[insert docker compose file name] up
```
*Note: Sometimes, when running the program, an error may occur where something
fails to connect. Although this happens very rarely, if it does occur, simply
exit the program and rerun it.
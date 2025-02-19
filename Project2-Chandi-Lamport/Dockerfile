FROM ubuntu:22.04

RUN apt-get update
RUN apt-get install -y gcc

ADD program.c /app/
ADD docker-compose/hostsfile.txt /app/
WORKDIR /app
RUN gcc program.c -o program

ENTRYPOINT ["/app/program"]
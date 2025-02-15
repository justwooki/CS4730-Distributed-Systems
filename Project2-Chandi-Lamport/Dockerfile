FROM ubuntu:22.04

RUN apt-get update
RUN apt-get install -y gcc

ADD program.c /app/
WORKDIR /app
RUN gcc program.c data_array.c -o program

ENTRYPOINT ["/app/program"]
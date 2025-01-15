FROM ubuntu:22.04

RUN apt-get update
RUN apt-get install -y gcc

ADD program1.c /app/
ADD data_array.h /app/
ADD data_array.c /app/
ADD constants.h /app/
ADD hostsfile.txt /app/
WORKDIR /app
RUN gcc program1.c data_array.c -o program1

ENTRYPOINT ["/app/program1"]
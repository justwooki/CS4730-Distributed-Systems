# Makefile

# Compiler
CC = gcc

# Compiler flags
CFLAGS = -Wall -g -pthread

# Create executable
$(EXEC): $(OBJS)
	$(CC) $(CFLAGS) -o $(EXEC) $(OBJS) -pthread

# Compile source files to object files
%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

# Clean object files and executable
clean:
	rm -f $(OBJS) $(EXEC)

.PHONY: all clean

prog1: clean program
	docker build . -t prj2
	docker compose -f docker-compose/docker-compose-testcase-1.yml up


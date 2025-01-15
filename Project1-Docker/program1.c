/*
 * This program implements a peer-to-peer network using UDP sockets. Each peer runs both a server
 * and client thread. The server thread listens for messages from other peers and the client thread
 * sends messages to other peers. When the server thread has confirmed that it has received a
 * message from all other peers, it prints "READY" to stderr.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <pthread.h>
#include <netdb.h>
#include "constants.h"
#include "data_array.h"

// Thread dealing with UDP server socket
void *server(void *arg) {
  data_array_t *prog_names = data_arr_copy(arg);
  char hostname[MAX_CHAR];
  gethostname(hostname, sizeof(hostname)); // Get hostname of the machine
  data_arr_remove(prog_names, hostname); // Remove hostname from list of programs

  int sock_fd;
  char port_num[MAX_CHAR];
  sprintf(port_num, "%d", PORT); // Convert port number to string
  struct addrinfo hints, *res;
  struct sockaddr_storage client_addr;
  data_array_t *received = data_arr_init();

  // Get address info
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_DGRAM;
  hints.ai_flags = AI_PASSIVE;

  if (getaddrinfo(hostname, port_num, &hints, &res) != 0) {
    perror("Server side: Error getting address info");
    perror(hostname);
    exit(1);
  }

  // Create socket file descriptor
  if ((sock_fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
    perror("Server side: Error opening socket");
    exit(1);
  }

  // Bind socket with server address
  if (bind(sock_fd, res->ai_addr, res->ai_addrlen) < 0) {
    perror("Server side: Error binding socket");
    exit(1);
  }

  // Receive messages from all programs
  while (data_arr_equals(prog_names, received) == 0) {
    char buf[MAX_CHAR];

    socklen_t addr_len = sizeof(client_addr);
    if (recvfrom(sock_fd, buf, MAX_CHAR, 0, (struct sockaddr *)&client_addr, &addr_len) < 0) {
      perror("Server side: Error receiving message");
      exit(1);
    }

    if (data_arr_contains(received, buf) == 0) {
      data_arr_add(received, buf);
    }
  }

  // Create socket file descriptor
  if ((sock_fd = socket(hints.ai_family, hints.ai_socktype, 0)) < 0) {
    perror("Server side: Error opening socket");
    exit(1);
  }

  // Print "READY" to stderr when message is received from all programs
  fprintf(stderr, "READY\n");

  // Free memory and close socket before exiting
  data_arr_obliterate(prog_names);
  data_arr_obliterate(received);
  freeaddrinfo(res);
  close(sock_fd);
  return NULL;
}

// Thread dealing with UDP client socket
void *client(void *arg) {
  data_array_t *prog_names = arg;
  int sock_fd;
  char hostname[MAX_CHAR];
  gethostname(hostname, sizeof(hostname)); // Get hostname of the machine
  char port_num[MAX_CHAR];
  sprintf(port_num, "%d", PORT); // Convert port number to string
  struct addrinfo hints, *res;

  // Set hints to get address info
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_DGRAM;

  for (int i = 0; i < data_arr_size(prog_names); i++) {
    const char *serv_name = data_arr_get(prog_names, i);

    if (strcmp(serv_name, hostname) == 0) {
      continue;
    }

    // Get address info
    if (getaddrinfo(serv_name, port_num, &hints, &res) != 0) {
      fprintf(stderr, "Client side: Error getting address info for %s:", serv_name);
      exit(1);
    }

    // Create socket file descriptor
    if ((sock_fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
      fprintf(stderr, "Client side: Error opening socket for %s:", serv_name);
      exit(1);
    }

    sleep(1); // Sleep for 1 second

    // Send message to server
    if (sendto(sock_fd, hostname, strlen(hostname), 0, res->ai_addr, res->ai_addrlen) < 0) {
      fprintf(stderr, "Client side: Error sending message for %s:", serv_name);
      exit(1);
    }

    // Free memory and close socket
    freeaddrinfo(res);
    close(sock_fd);
  }

  return NULL;
}

int main(int argc, char *argv[]) {
  if (argc != 3) {
    exit(1);
  }

  // Open file
  FILE *file = fopen(argv[2], "r");
  if (file == NULL) {
    perror("Error opening file");
    exit(1);
  }

  // Store all hostnames found in data array struct
  data_array_t *arr = data_arr_init();
  char buffer[MAX_CHAR_PER_LINE];

  while (fgets(buffer, MAX_CHAR_PER_LINE, file) != NULL) {
    buffer[strcspn(buffer, "\n")] = 0; // Remove newline character
    data_arr_add(arr, buffer);
  }

  if (data_arr_size(arr) == 0) {
    perror("No programs found in file");
    exit(1);
  }

  // Create server and client threads
  pthread_t server_thread;
  pthread_t client_thread;

  // Create server thread
  if (pthread_create(&server_thread, NULL, server, arr) != 0) {
    perror("Error creating server thread");
    exit(1);
  }

  // Create client thread
  if (pthread_create(&client_thread, NULL, client, arr) != 0) {
    perror("Error creating client thread");
    exit(1);
  }

  // Join server thread
  if (pthread_join(server_thread, NULL) != 0) {
    perror("Error joining server thread");
    exit(1);
  }

  // Join client thread
  if (pthread_join(client_thread, NULL) != 0) {
    perror("Error joining client thread");
    exit(1);
  }

  // Free memory and close file before exiting
  data_arr_obliterate(arr);
  fclose(file);
  return 0;
}
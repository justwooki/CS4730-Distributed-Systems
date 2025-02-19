#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <stdbool.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <pthread.h>

#define MAX_HOSTNAME_LENGTH 256 // Maximum length of a hostname string
#define MAX_PROCESSES 5 // Maximum number of processes in this system
#define PORT 7000 // the port users will be connecting to
#define PORT_NUM_STR_LEN 6 // Length of the port number string
#define BACKLOG 10 // how many pending connections queue will hold
#define MAX_RETRIES 10 // Maximum number of connection retries
#define RETRY_DELAY_SECONDS 1 // Delay between retries in seconds
#define STRING_LENGTH 1024

// Structure to hold process information
typedef struct {
  int proc_id; // UID of the process
  int state; // Number of tokens received
  int predecessor; // UID of the predecessor process
  int successor; // UID of the successor process
  char hostname[MAX_HOSTNAME_LENGTH]; // Hostname of this process
  char all_hostnames[MAX_PROCESSES][MAX_HOSTNAME_LENGTH]; // Hostnames of all processes
  float tok_delay; // Delay between token transmissions in microseconds
  float mark_delay; // Delay between mark transmissions in microseconds
  pthread_mutex_t mutex;
  pthread_cond_t cond;
  char strbuf[STRING_LENGTH];
  int ready;
} ProcessInfo;

// Thread dealing with TCP server socket
void *server(void *arg) {
  ProcessInfo *process = (ProcessInfo *)arg;
  int sock_fd;
  char port_num[PORT_NUM_STR_LEN];
  sprintf(port_num, "%d", PORT); // Convert port number to string
  struct addrinfo hints, *res;

  // Get address info
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_STREAM;
  hints.ai_flags = AI_PASSIVE;

  if (getaddrinfo(NULL, port_num, &hints, &res) != 0) {
    fprintf(stderr, "Server Side Error: Could not get address info for %s\n", process->hostname);
    exit(1);
  }

  // Create socket file descriptor
  if ((sock_fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
    fprintf(stderr, "Server Side Error: Could not open socket for %s\n", process->hostname);
    exit(1);
  }

  // Set socket options
  int opt = 1;
  if (setsockopt(sock_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
      perror("Server side error setting socket options");
      exit(1);
  }

  // Bind socket with server address
  if (bind(sock_fd, res->ai_addr, res->ai_addrlen) < 0) {
    perror("Server side error binding socket");
    exit(1);
  }

  // Listen for incoming connections
  if (listen(sock_fd, BACKLOG) < 0) {
    perror("Server side error listening on socket");
    exit(1);
  }

  // Accept incoming connections
  struct sockaddr_storage client_addr;
  int addr_size = sizeof(client_addr);
  int new_fd;
  char clients[MAX_PROCESSES][MAX_HOSTNAME_LENGTH];
  strncpy(clients[process->proc_id - 1], process->hostname, MAX_HOSTNAME_LENGTH - 1);
  bool all_clients_connected = false;

  // Wait for all clients to connect
//  while (all_clients_connected == false) {
    // Accept connection
    if ((new_fd = accept(sock_fd, (struct sockaddr *)&client_addr, (socklen_t *)&addr_size)) < 0) {
      perror("Server side error accepting connection");
      exit(1);
    }

    while (1) {
      // Receive message from client
      char msg[MAX_HOSTNAME_LENGTH];
      char rec_msg[MAX_HOSTNAME_LENGTH];
      char new_msg[MAX_HOSTNAME_LENGTH];

      if (recv(new_fd, msg, MAX_HOSTNAME_LENGTH, 0) < 0) {
        perror("Server side error receiving message");
        exit(1);
      }

      // Process msg
      // {proc_id: ID, sender: SENDER_ID, receiver: RECEIVER_ID, message:"token"}
      char *result = strstr(msg, "\"token\"");
      if (result != NULL) {
        process->state++; // update state

        // Print proccess id and state
        fprintf(stderr, "proc_id: %d, state: %d\n", process->proc_id, process->state);

        // Print message received
        sprintf(rec_msg, "{\"proc_id\": %d, \"sender\": %d, \"receiver\": %d, \"message\":\"token\"}\n",
                process->proc_id, process->predecessor, process->proc_id);
        fprintf(stderr, "%s", msg);

        sprintf(new_msg, "{\"proc_id\": %d, \"sender\": %d, \"receiver\": %d, \"message\":\"token\"}\n",
                process->proc_id, process->proc_id, process->successor);

        usleep(process->tok_delay); // sleep for tok_delay seconds

        // Send message to successor
        pthread_mutex_lock(&process->mutex);
        strcpy(process->strbuf, new_msg);
        process->ready = 1;
        pthread_cond_signal(&process->cond);
        pthread_mutex_unlock(&process->mutex);
      }
    }
    // Check if the client is already connected
//    for (int i = 0; i < MAX_PROCESSES; i++) {
//      if (clients[i] == NULL && strcmp(buf, process.all_hostnames[i]) == 0) {
//        strncpy(clients[i], buf, MAX_HOSTNAME_LENGTH - 1);
//        break;
//      }
//    }

    // Check if all clients are connected
    for (int i = 0; i < MAX_PROCESSES; i++) {
      // If a client is not connected, break
      if (clients[i] == NULL || strcmp(clients[i], process->all_hostnames[i]) != 0) {
        break;
      }

      // If all clients are connected, set flag to true
      if (i == MAX_PROCESSES - 1) {
        all_clients_connected = true;
      }
    }
//  }

  // Print "READY" to stderr when message is received from all programs
  fprintf(stderr, "READY\n");

  // Free memory and close socket before exiting
  freeaddrinfo(res);
  close(sock_fd);
  return NULL;
}

// Thread dealing with TCP client socket
void *client(void *arg) {
  ProcessInfo *process = (ProcessInfo *)arg;
  int sock_fd;
  char port_num[PORT_NUM_STR_LEN];
  sprintf(port_num, "%d", PORT); // Convert port number to string
  struct addrinfo hints, *res;

  sleep(1); // wait for servers to come up

  // Get address info
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_STREAM;

  // Get address info
  const char *successor_name = process->all_hostnames[process->successor - 1];
  if (getaddrinfo(successor_name, port_num, &hints, &res) != 0) {
    fprintf(stderr, "Client side error: Could not get address info for %s\n", successor_name);
    exit(1);
  }

  // Create socket file descriptor
  if ((sock_fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
    fprintf(stderr, "Client side error: Could not open socket for %s\n", successor_name);
    exit(1);
  }

  // Connect to server
  if (connect(sock_fd, res->ai_addr, res->ai_addrlen) < 0) {
    fprintf(stderr, "Client side error: Could not connect to %s\n", successor_name);
    exit(1);
  }

  // If state is 1, send token to successor to start the ring
  if (process->state == 1) {
      char msg[STRING_LENGTH];
      sprintf(msg, "{\"proc_id\": %d, \"sender\": %d, \"receiver\": %d, \"message\":\"token\"}\n",
              process->proc_id, process->proc_id, process->successor);

      fprintf(stderr, "%s", msg);

      // Send message to server
      if (send(sock_fd, msg, strlen(msg), 0) < 0) {
        fprintf(stderr, "Client side error: Could not send message for %s\n", successor_name);
        exit(1);
      }
  }

  while (1) {
    // Wait for data from reader
    pthread_mutex_lock(&process->mutex);
    while (!process->ready) {
      pthread_cond_wait(&process->cond, &process->mutex); // Wait for signal
    }
    process->ready = 0; // Reset the flag for future use
    pthread_mutex_unlock(&process->mutex);

    // Send message to server
    fprintf(stderr, "%s", process->strbuf);
    if (send(sock_fd, process->strbuf, strlen(process->strbuf), 0) < 0) {
      fprintf(stderr, "Client side error: Could not send message for %s\n", successor_name);
      exit(1);
    }
  }

  printf("Client sent message to %s \n", successor_name);
  freeaddrinfo(res);
  close(sock_fd);

//  for (int i = 0; i < MAX_PROCESSES; i++) {
//    const char *serv_name = process.all_hostnames[i];
//
//    if (strcmp(serv_name, process.hostname) == 0) {
//      continue;
//    }
//
//    int retries = 0;
//    while (retries < MAX_RETRIES) {
//      // Get address info
//      if (getaddrinfo(serv_name, port_num, &hints, &res) != 0) {
//        fprintf(stderr, "Client side error: Could not get address info for %s\n", serv_name);
//        retries++;
//        sleep(RETRY_DELAY_SECONDS);
//        continue;
//      }
//
//      // Create socket file descriptor
//      if ((sock_fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
//        fprintf(stderr, "Client side error: Could not open socket for %s\n", serv_name);
//        retries++;
//        sleep(RETRY_DELAY_SECONDS);
//        freeaddrinfo(res);
//        continue;
//      }
//
//      // Connect to server
//      if (connect(sock_fd, res->ai_addr, res->ai_addrlen) < 0) {
//        fprintf(stderr, "Client side error: Could not connect to %s\n", serv_name);
//        retries++;
//        sleep(RETRY_DELAY_SECONDS);
//        freeaddrinfo(res);
//        close(sock_fd);
//        continue;
//      }
//
//      // Send message to server
//      if (send(sock_fd, process.hostname, strlen(process.hostname), 0) < 0) {
//        fprintf(stderr, "Client side error: Could not send message for %s\n", serv_name);
//        retries++;
//        sleep(RETRY_DELAY_SECONDS);
//        freeaddrinfo(res);
//        close(sock_fd);
//        continue;
//      }
//
//      printf("Client sent message to %s \n", serv_name);
//      freeaddrinfo(res);
//      close(sock_fd);
//      break;
//    }
//
//    if (retries == MAX_RETRIES) {
//      fprintf(stderr, "Client side error: Max retries reached for %s. Exiting.\n", serv_name);
//    }
//  }

  return NULL;
}

int main(int argc, char *argv[]) {
  // Initialize variables
  char *hostfile_path = NULL;
  float tok_delay = 0.0f;
  float mark_delay = 0.0f;
  int snapshot_state = -1;
  int snapshot_id = -1;
  bool starts_with_tok = false;
  ProcessInfo process;

  // Parse command line arguments
  int opt;
  while ((opt = getopt(argc, argv, "h:xt:m:s:p:")) != -1) {
    switch (opt) {
      case 'h':
        hostfile_path = optarg;
        break;
      case 'x':
        starts_with_tok = true;
        break;
      case 't':
        tok_delay = atof(optarg);
        break;
      case 'm':
        mark_delay = atof(optarg);
        break;
      case 's':
        snapshot_state = atoi(optarg);
        break;
      case 'p':
        snapshot_id = atoi(optarg);
        break;
      default:
        fprintf(stderr, "Usage: %s -h <hostfile> [-x] [-t <tok_delay>] [-m <mark_delay>] [-s <snapshot_state> -p <snapshot_id>]\n", argv[0]);
        exit(1);
    }
  }

  // Check if hostfile path is provided
  if (hostfile_path == NULL) {
    fprintf(stderr, "Error: Hostfile path is missing.\n");
    exit(1);
  }

  // Check if both snapshot state and snapshot id are provided or not
  if ((snapshot_state < 0 && snapshot_id >= 0) || (snapshot_state >= 0 && snapshot_id < 0)) {
    fprintf(stderr, "Error: Both snapshot state and snapshot id must be provided.\n");
    exit(1);
  }

  process.state = starts_with_tok ? 1 : 0;
  process.tok_delay = tok_delay * 1000000; // Convert seconds to microseconds
  process.mark_delay = mark_delay * 1000000; // Convert seconds to microseconds
  if (gethostname(process.hostname, sizeof(process.hostname)) != 0) {
    perror("Error getting hostname");
    exit(1);
  }

  // Open hostfile for reading
  FILE *file = fopen("hostsfile.txt", "r");
  char line[MAX_HOSTNAME_LENGTH];
  int line_num = 0;
  int num_processes = 0;
  if (file == NULL) {
    fprintf(stderr, "Error opening file at %s\n", hostfile_path);
    exit(1);
  }

  // Read the hostfile line by line
  while (fgets(line, sizeof(line), file) != NULL) {
    line[strcspn(line, "\n")] = 0; // Remove trailing newline character

    // Check for empty lines or lines that are too long
    if (strlen(line) == 0 || strlen(line) >= MAX_HOSTNAME_LENGTH) {
      fprintf(stderr, "Error: Invalid line in hostfile: %s\n", line);
      exit(1);
    }

    // Store the hostname
    strcpy(process.all_hostnames[line_num], line);

    // Check if this is the current process's hostname
    if (strcmp(line, process.hostname) == 0) {
      process.proc_id = line_num + 1;
    }

    line_num++;
    num_processes++;
  }

  // Check if the number of processes is valid
  if (num_processes != MAX_PROCESSES) {
    fprintf(stderr, "Error: Invalid number of processes in hostfile. Expected %d, got %d.\n",
            MAX_PROCESSES, num_processes);
    exit(1);
  }

  // Check if the process ID was found
  if (process.proc_id == 0) {
    fprintf(stderr, "Error: Could not find hostname '%s' in hostfile\n", process.hostname);
    exit(1);
  }

  // Calculate predecessor and successor IDs
  process.predecessor = (process.proc_id == 1) ? num_processes : process.proc_id - 1;
  process.successor = (process.proc_id == num_processes) ? 1 : process.proc_id + 1;

  // Print process information
  fprintf(stderr, "{proc_id: %d, state: %d, predecessor: %d, successor: %d}\n",
          process.proc_id, process.state, process.predecessor, process.successor);

  // Create server and client threads
  pthread_t server_thread;
  pthread_t client_thread;

  // Initialize mutex and condition variable
  pthread_mutex_init(&process.mutex, NULL);
  pthread_cond_init(&process.cond, NULL);
  process.ready = 0;

  // Create server thread
  if (pthread_create(&server_thread, NULL, server, &process) != 0) {
    perror("Error creating server thread");
    exit(1);
  }

  // Create client thread
  if (pthread_create(&client_thread, NULL, client, &process) != 0) {
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

  fclose(file);
  return 0;
}
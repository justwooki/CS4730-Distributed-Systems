# Report
My program implements a peer-to-peer network using UDP sockets.
Because each peer needs to send and receive messages to and from
all other peers, I decided to use multithreading to divide the
server (receiving messages) from the client actions (sending
messages).

After reading the ```hostsfile.txt``` file, I needed to pass all the
program names in it to both the client and the server, so I defined
a ```data_array``` struct (it's basically a dynamic array) to store
all the program names. This data array was then passed to both
client and server threads.

In the server thread, I created a socket and bound it to the
hostname address. Then I ran a loop to continuously receive messages
until the server has received a message from all other peers.
Afterwards, the server thread will print out a "READY" message on
stderr to indicate that it has received all messages.

In the client thread, I created a socket and sent a message to each
peer. This was done in a big loop for every peer given in the data
array. The message contained the hostname of the client, which
allowed the server receiving the message to keep track which peer
was sending the message.

And that's it! That's the algorithm I used to implement the
peer-to-peer network using UDP sockets!
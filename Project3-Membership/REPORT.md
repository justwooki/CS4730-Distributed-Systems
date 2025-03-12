# Report
Justin Kim

## State Diagram
My state diagram has a main state centered around "static state".
In this state, the process simply exists.

If the leader sees a new process has joined, it will check how
many processes are currently present and alive in the system. If
there are at least two present, then the leader will send a REQ
message to all other processes and wait for all of them to
respond with an OK message. Upon receiving these messages, the
leader will then proceed to add the process to the membership and
send a NEWVIEW message to all processes. However, if there are
less than two processes, the leader will simply add the process
to the membership and send the NEWVIEW message to all processes.
It will then return to its static state.

If the process receives a REQ message, it will save the
operation, send an OK message back to its sender, and resume its
static state.

If the process receives a NEWVIEW message, it will update the
membership accordingly and resume its static state.

If the process finds a dead process, it will check if it is the
leader. If not, it will resume its static state, but if it is, it
will send a REQ message to the other processes and wait for an OK
message from all processes before removing the process from the
membership and sending a NEWVIEW message to everyone. The process
will then resume its static state.

## Language
This assignment was programmed in Java. Why? Because dealing with
locks in a multithreaded C program is more painful than hitting
my funny bone on my laptop.

## Design Decisions
For each process that runs, its operations are divided between
server and client side, each running in their own thread.

The client thread will have a heartbeat sender running in its own
subthread. This heartbeat sender will broadcast its heartbeat to
all processes to let everyone know it's alive. The client will
connect to the leader process to join as an alive peer. If the
client belongs to a leader process, it will additionally wait
for its server counterpart to tell it when to broadcast messages
to all other processes in the system.

The server thread will have a heartbeat listener. Its main job is
to listen for heartbeats and determine when a process has
crashed. The heartbeat listener is split into three subthreads.
The first is in charge of constantly updating who are the alive
processes and who are the dead ones that haven't been removed
from membership yet (these are kept an eye on). The second
subthread constantly listens for heartbeats from each alive
process and updates the time at which they were last spotted
alive. The third subthread looks at the last time each process
was seen alive and labels it as dead if a heartbeat hasn't been
received for some time. If the heartbeat listener belongs to a
leader process, it will report that its dead to the main server
in order for it to be removed from membership. The server thread
is in charge of accepting connections from clients. For every
server that connects, it will start a new client handler
subthread that will deal with the client.
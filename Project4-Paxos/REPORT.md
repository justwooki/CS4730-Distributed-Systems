# Report
Justin Kim

## Language
This assignment was programmed in Java...bc it's easier that way.

## Design Decisions
My code structure is described as below:

### Process
This is an abstract class describing a process object. Both Proposer
and Acceptor extend this class. It exists mainly to branch proposer
and acceptor together since they are both processes and that share
some similar properties.

### Proposer
The proposer is an extension of the Process class that represents a
proposer in the Paxos algorithm. The proposer carries a list of its
acceptors which it uses to broadcast messages to them. Since the
proposer is its own acceptor, it is also on the acceptor list, and
even has an Acceptor running on a separate thread. The start()
method runs the entire proposer program and is really made of three
components.

The first component is handling the Prepare broadcast. This is split
between the actual broadcast and the waiting for acknowledgements,
which are both run concurrently. Broadcasting will eventually split
into multiple sub-threads each dealing with sending a message to an
acceptor. The acknowledgement part simply waits for messages to be
received on a queue and read them. Once the majority acknowledgement
is achieved, the algorithm will replace its value if needed.

The second component is handling the Accept broadcast. This works
very similarly with the way Prepare broadcast is handled. The biggest
difference is that a boolean value is returned that determines if the
proposal has been rejected or not. This sets up the third component.

The third component is the loop. If the proposal is not accepted, the
algorithm will loop around and perform the entire process again with
a newly assigned proposal number.

### Acceptor
The acceptor is an extension of the Process class that represents an
acceptor in the Paxos algorithm. The acceptor simply waits until
an acceptor sends a message, to which it will respond accordingly.
For each proposer that connects to it, it will start a new thread
to handle it.

### ProcessInfo & ProposalValuePair
Both of these classes are just simple data classes that made my life
easier.

### Util
A utility class that made my life easier.

### Main
The main class that runs the whole program.
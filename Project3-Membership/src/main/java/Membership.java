import java.util.ArrayList;
import java.util.List;

/**
 * A membership consists of a unique id, referred to as view id and the list of the alive peers.
 * The view id should be monotonically increasing and create a total order, even when the leader
 * changes. The list of peers should represent peers by their peer number. It should also be
 * updated when a peer joins or leaves the group. Additionally, this class is thread-safe.
 */
public final class Membership {
  private int viewId;
  private final List<Integer> peers;

  /**
   * Constructor for Membership that creates a new membership with a view id of 0 and an empty list
   * of peers.
   */
  public Membership() {
    this.viewId = 0;
    this.peers = new ArrayList<>();
  }

  /**
   * Gets the view id of the membership.
   *
   * @return the view id
   */
  public synchronized int getViewId() {
    return this.viewId;
  }

  /**
   * Gets the list of peers in the membership.
   *
   * @return the list of peers
   */
  public synchronized List<Integer> getPeers() {
    return new ArrayList<>(this.peers);
  }

  /**
   * Sets the view id of the membership.
   *
   * @param viewId the view id to set
   */
  public synchronized void setViewId(int viewId) {
    this.viewId = viewId;
  }

  /**
   * Sets the list of peers in the membership.
   *
   * @param peers the list of peers to set
   */
  public synchronized void setPeers(List<Integer> peers) {
    this.peers.clear();
    this.peers.addAll(peers);
  }

  /**
   * Adds a peer to the membership and updates the view id.
   *
   * @param peer the peer to add
   * @return true if the peer was added, false if the peer was already in the membership
   */
  public synchronized boolean addPeer(int peer) {
    if (!peers.contains(peer)) {
      this.peers.add(peer);
      this.viewId++;
      return true;
    }
    return false;
  }

  /**
   * Removes a peer from the membership and updates the view id.
   *
   * @param peer the peer to remove
   * @throws IllegalArgumentException if the peer is not in the membership
   */
  public synchronized void removePeer(int peer) throws IllegalArgumentException {
    if (!this.peers.contains(peer)) {
      throw new IllegalArgumentException("Peer not found");
    }
    this.peers.remove(Integer.valueOf(peer));
    this.viewId++;
  }

  @Override
  public synchronized boolean equals(Object obj) {
    if (!(obj instanceof Membership other)) {
      return false;
    }
    return this.viewId == other.viewId && this.peers.equals(other.peers);
  }
}

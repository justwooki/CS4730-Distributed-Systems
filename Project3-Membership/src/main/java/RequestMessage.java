/**
 * Represents a request message sent by the leader to other peers in the system. This message
 * contains a unique request ID, the current view ID, and the type of operation to be performed.
 * The operation type can indicate actions such as adding or removing a peer.
 */
public final class RequestMessage {
  private int requestId;
  private int viewId;
  private String operationType;

  /**
   * Constructor for RequestMessage that creates a new request message with the given request ID,
   * view ID, and operation type.
   *
   * @param requestId the request ID
   * @param viewId the view ID
   * @param operationType the operation type
   */
  public RequestMessage(int requestId, int viewId, String operationType) {
    this.requestId = requestId;
    this.viewId = viewId;
    this.operationType = operationType;
  }

  /**
   * Gets the request ID of the request message.
   *
   * @return the request ID
   */
  public int getRequestId() {
    return requestId;
  }

  /**
   * Gets the view ID of the request message.
   *
   * @return the view ID
   */
  public int getViewId() {
    return viewId;
  }

  /**
   * Gets the operation type of the request message.
   *
   * @return the operation type
   */
  public String getOperationType() {
    return operationType;
  }

  /**
   * Sets the request ID of the request message.
   *
   * @param requestId the request ID to set
   */
  public void setRequestId(int requestId) {
    this.requestId = requestId;
  }

  /**
   * Sets the view ID of the request message.
   *
   * @param viewId the view ID to set
   */
  public void setViewId(int viewId) {
    this.viewId = viewId;
  }

  /**
   * Sets the operation type of the request message.
   *
   * @param operationType the operation type to set
   */
  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }
}

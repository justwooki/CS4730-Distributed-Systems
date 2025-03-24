package main.java;

/**
 * Abstract class representing a process. A process has an ID and hostname to identify itself.
 */
public abstract class Process {
  protected final ProcessInfo info;

  /**
   * Constructs a new Process object.
   *
   * @param id the unique ID of the process
   * @param name the hostname of the process
   */
  public Process(int id, String name) {
    this.info = new ProcessInfo(id, name);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Process other)) {
      return false;
    }
    return this.info.getId() == other.info.getId()
            && this.info.getName().equals(other.info.getName());
  }

  /**
   * Starts running the process.
   */
  public abstract void start();
}

package main.java;

/**
 * Represents the information by which to identify a process by. Includes specifically the unique
 * id and host name of the process.
 */
public final class ProcessInfo {
  private final int id;
  private final String name;

  /**
   * Constructs a new ProcessInfo object belonging to a process.
   * @param id the id of the process
   * @param name the name of the process
   */
  public ProcessInfo(int id, String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Get the id of the process.
   * @return the id of the process
   */
  public int getId() {
    return this.id;
  }

  /**
   * Get the name of the process.
   * @return the name of the process
   */
  public String getName() {
    return this.name;
  }
}

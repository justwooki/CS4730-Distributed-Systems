package main.java;

public abstract class Process {
  protected final ProcessInfo info;

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

  public abstract void start();
}

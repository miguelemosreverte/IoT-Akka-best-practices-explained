package monitoring.algebra

trait Counter {
  def increment(): Unit

  def add(num: Int): Unit
}

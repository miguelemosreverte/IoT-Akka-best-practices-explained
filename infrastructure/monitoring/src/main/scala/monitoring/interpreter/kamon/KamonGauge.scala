package monitoring.interpreter.kamon

import kamon.Kamon
import monitoring.algebra.Gauge

class KamonGauge(preffix: String)(name: String) extends Gauge {

  private val gauge = Kamon
    .gauge(s"$preffix-gauges")
    .withTag("entity", name)

  override def increment(): Unit = gauge.increment()
  override def decrement(): Unit = gauge.decrement()

  override def add(num: Int): Unit = gauge.increment(num)
  override def subtract(num: Int): Unit = gauge.decrement(num)

  override def set(num: Int): Unit = gauge.update(num)
}

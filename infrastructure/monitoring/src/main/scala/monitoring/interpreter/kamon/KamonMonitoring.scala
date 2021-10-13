package monitoring.interpreter.kamon

import kamon.Kamon
import monitoring.Monitoring
import monitoring.algebra.{Counter, Gauge, Histogram}

class KamonMonitoring(preffix: String) extends Monitoring {

  Kamon.init()

  override def counter(name: String): Counter =
    new KamonCounter(preffix)(name)

  override def histogram(name: String): Histogram = new KamonHistogram(preffix)(name)

  override def gauge(name: String): Gauge = new KamonGauge(preffix)(name)
}

package monitoring

import monitoring.algebra.{Counter, Gauge, Histogram}

trait Monitoring {
  def counter(name: String): Counter

  def histogram(name: String): Histogram

  def gauge(name: String): Gauge
}

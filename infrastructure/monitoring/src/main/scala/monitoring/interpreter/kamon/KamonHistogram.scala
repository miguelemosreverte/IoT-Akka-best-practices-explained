package monitoring.interpreter.kamon

import kamon.Kamon
import monitoring.algebra.Histogram

class KamonHistogram(preffix: String)(name: String) extends Histogram {

  private val histogram = Kamon
    .histogram(s"${preffix}-histograms")
    .withTag("entity", name)

  override def record(value: Long): Unit = histogram.record(value)
}

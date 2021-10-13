package monitoring.interpreter.kamon

import kamon.Kamon
import kamon.tag.TagSet
import monitoring.algebra.Counter

class KamonCounter(preffix: String)(name: String) extends Counter {
  private val tags = TagSet.from(Map("entity" -> name))

  private val counter = Kamon
    .counter(s"$preffix-counter")
    .withTags(tags)

  override def increment(): Unit = counter.increment()
  override def add(num: Int): Unit = counter.increment(num)
}

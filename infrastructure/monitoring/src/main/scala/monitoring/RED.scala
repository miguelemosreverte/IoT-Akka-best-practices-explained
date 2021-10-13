package monitoring

import scala.concurrent.{ExecutionContext, Future}

object RED {
  final def recordRequests(name: String)(
      implicit
      monitoring: Monitoring
  ): Unit =
    monitoring.counter(s"$name-request").increment()

  final def recordErrors(name: String)(
      implicit
      monitoring: Monitoring
  ): Unit =
    monitoring.counter(s"$name-error").increment()

  final def recordDuration(name: String, duration: Long)(
      implicit
      monitoring: Monitoring
  ): Unit =
    monitoring.histogram(s"$name-duration").record(duration)

}

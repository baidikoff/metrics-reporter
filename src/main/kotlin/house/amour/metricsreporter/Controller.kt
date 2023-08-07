package house.amour.metricsreporter

import io.micrometer.core.instrument.MeterRegistry
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("api/v1")
class MetricsController(
    private val registry: MeterRegistry
) {

    @PostMapping("/metrics")
    @Operation(summary = "Report metrics")
    fun submitMetrics(@RequestBody body: MetricsBody) {
        body.metrics.forEach { metric ->
            when (metric.type) {
                MetricType.COUNTER -> reportCounter(metric, body.sender)
                MetricType.TIMER -> reportTimer(metric, body.sender)
            }
        }
    }

    private fun reportCounter(metric: Metric, sender: MetricsSender) =
        registry.counter("${sender.name}.${metric.name}").increment(metric.value)

    private fun reportTimer(metric: Metric, sender: MetricsSender) =
        registry.timer("${sender.name}.${metric.name}").record(metric.value.toLong(), TimeUnit.MILLISECONDS)
}

data class MetricsBody(val sender: MetricsSender, val metrics: List<Metric>)
data class MetricsSender(val name: String)
data class Metric(val name: String, val value: Double, val type: MetricType)
enum class MetricType {
    COUNTER, TIMER;
}

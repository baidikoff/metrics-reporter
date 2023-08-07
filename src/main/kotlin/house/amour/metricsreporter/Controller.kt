package house.amour.metricsreporter

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Statistic
import io.micrometer.statsd.StatsdConfig
import io.micrometer.statsd.StatsdLineBuilder
import io.micrometer.statsd.StatsdMeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

class CustomStatsdLineBuilder(private val meterId: Meter.Id): StatsdLineBuilder {
    override fun count(amount: Long, stat: Statistic): String = "${metric("counter")}:$amount|c"
    override fun gauge(amount: Double, stat: Statistic): String = "${metric("gauge")}:$amount|g"
    override fun histogram(amount: Double): String = "${metric("histogram")}:$amount|h"
    override fun timing(timeMs: Double): String = "${metric("timer")}:$timeMs|ms"

    private fun metric(kind: String): String =
        joined("application", "metrics_reporter", "kind", kind, meterId.name, meterId.getTag(meterId.name))

    private fun joined(vararg metrics: String?) = metrics.filterNotNull().joinToString(separator = ".")
}

@Configuration
class StatsDMeterConfiguration {
    @Bean
    fun customRegistry(config: StatsdConfig, clock: Clock): StatsdMeterRegistry =
        StatsdMeterRegistry.builder(config).clock(clock).lineBuilder { id, _ ->
            CustomStatsdLineBuilder(id)
        }.build()
}

@RestController
@RequestMapping("api/v1")
class MetricsController {

    @Autowired
    private lateinit var registry: MeterRegistry

    @PostMapping("/metrics")
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

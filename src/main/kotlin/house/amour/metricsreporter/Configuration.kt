package house.amour.metricsreporter

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Statistic
import io.micrometer.statsd.StatsdConfig
import io.micrometer.statsd.StatsdLineBuilder
import io.micrometer.statsd.StatsdMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class CustomStatsdLineBuilder(private val meterId: Meter.Id) : StatsdLineBuilder {
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

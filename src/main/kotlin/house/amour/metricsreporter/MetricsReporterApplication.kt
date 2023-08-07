package house.amour.metricsreporter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MetricsReporterApplication

fun main(args: Array<String>) {
    runApplication<MetricsReporterApplication>(*args)
}

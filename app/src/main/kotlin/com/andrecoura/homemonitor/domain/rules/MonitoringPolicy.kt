package com.andrecoura.homemonitor.domain.rules

import java.time.Duration

/** How aggressively the monitor looks at the house. */
data class MonitoringPolicy(
    val gateOpenLimit: Duration = AlertRules.DEFAULT_GATE_OPEN_LIMIT,
    val probeInterval: Duration = Duration.ofSeconds(30),
    val gateCheckInterval: Duration = Duration.ofSeconds(15),
)

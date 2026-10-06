package com.andrecoura.homemonitor.data.simulation

/**
 * Knobs of the fake home. [Default] feels alive on a phone; [Instant] has no latency, failures or
 * background activity, so tests are deterministic.
 */
data class SimulationConfig(
    val latencyMillis: LongRange,
    val commandFailureRate: Double,
    val gateTravelMillis: Long,
    /** null disables autonomous gate activity (visitors, outages). */
    val gateActivityEveryMillis: LongRange?,
    val gateOpenForMillis: Long,
    val gateOutageMillis: Long,
    /** null disables incoming intercom calls. */
    val callEveryMillis: LongRange?,
    val ringTimeoutMillis: Long,
    val deviceOutageChance: Double,
    val deviceOutageMillis: Long,
    val frameIntervalMillis: Long,
) {
    companion object {
        val Default =
            SimulationConfig(
                latencyMillis = 150L..700L,
                commandFailureRate = 0.15,
                gateTravelMillis = 6_000,
                gateActivityEveryMillis = 60_000L..150_000L,
                gateOpenForMillis = 45_000,
                gateOutageMillis = 40_000,
                callEveryMillis = 25_000L..120_000L,
                ringTimeoutMillis = 25_000,
                deviceOutageChance = 0.04,
                deviceOutageMillis = 90_000,
                frameIntervalMillis = 400,
            )

        val Instant =
            SimulationConfig(
                latencyMillis = 0L..0L,
                commandFailureRate = 0.0,
                gateTravelMillis = 100,
                gateActivityEveryMillis = null,
                gateOpenForMillis = 0,
                gateOutageMillis = 0,
                callEveryMillis = null,
                ringTimeoutMillis = 1_000,
                deviceOutageChance = 0.0,
                deviceOutageMillis = 0,
                frameIntervalMillis = 100,
            )
    }
}

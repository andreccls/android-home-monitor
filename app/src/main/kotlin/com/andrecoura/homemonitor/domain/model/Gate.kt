package com.andrecoura.homemonitor.domain.model

import java.time.Instant

enum class GateState { CLOSED, OPENING, OPEN, CLOSING, OFFLINE }

enum class GateCommand { OPEN, CLOSE }

data class Gate(
    val id: String,
    val name: String,
    val state: GateState,
    val stateSince: Instant,
) {
    /**
     * Gate state machine: the one command that makes sense right now, or null when none does.
     * A moving gate can be reversed; an offline gate accepts nothing.
     */
    val suggestedCommand: GateCommand?
        get() =
            when (state) {
                GateState.CLOSED, GateState.CLOSING -> GateCommand.OPEN
                GateState.OPEN, GateState.OPENING -> GateCommand.CLOSE
                GateState.OFFLINE -> null
            }

    fun allows(command: GateCommand): Boolean = command == suggestedCommand
}

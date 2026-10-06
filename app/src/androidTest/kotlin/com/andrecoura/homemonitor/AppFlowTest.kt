package com.andrecoura.homemonitor

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.andrecoura.homemonitor.data.local.AlertEntity
import com.andrecoura.homemonitor.data.local.AppDatabase
import com.andrecoura.homemonitor.data.monitoring.MonitoringService
import com.andrecoura.homemonitor.di.ApplicationScope
import com.andrecoura.homemonitor.domain.model.AlertType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * End-to-end UI flows on the real app graph: real Compose UI, view models, repositories, Room (in memory)
 * and the simulators, configured to be instant. Only the Application differs (HiltTestApplication).
 */
@HiltAndroidTest
class AppFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var monitoring: MonitoringService

    @Inject lateinit var db: AppDatabase

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        hilt.inject()
        monitoring.start(scope)
    }

    private fun text(
        id: Int,
        vararg args: Any,
    ) = compose.activity.getString(id, *args)

    private fun waitForText(value: String) =
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }

    private fun hasNodeWithDescription(value: String) = compose.onAllNodes(hasContentDescription(value)).fetchSemanticsNodes().isNotEmpty()

    // The section titles on Home reuse the tab labels, so pick the clickable one.
    private fun tab(id: Int) = compose.onNode(hasText(text(id)) and hasClickAction())

    private fun openTab(id: Int) = tab(id).performClick()

    @Test
    fun registerCamera() {
        openTab(R.string.tab_devices)
        compose.onNodeWithContentDescription(text(R.string.devices_add)).performClick()
        compose.onNodeWithText(text(R.string.form_name)).performTextInput("Câmera do portão")
        compose.onNodeWithText(text(R.string.form_room)).performTextInput("Entrada")
        compose.onNodeWithText(text(R.string.form_address_camera)).performTextInput("rtsp://192.168.0.50/stream")
        compose.onNodeWithText(text(R.string.save)).performClick()
        waitForText("Câmera do portão")
        compose.onNodeWithText("Câmera do portão").assertIsDisplayed()
    }

    @Test
    fun registerAlexa() {
        openTab(R.string.tab_devices)
        compose.onNodeWithContentDescription(text(R.string.devices_add)).performClick()
        compose.onNodeWithText(text(R.string.form_name)).performTextInput("Echo da cozinha")
        compose.onNodeWithText(text(R.string.form_room)).performTextInput("Cozinha")
        compose.onNodeWithText(text(R.string.device_kind_alexa)).performClick()
        compose.onNodeWithText(text(R.string.form_address_alexa)).performTextInput("G090LF1111222233")
        compose.onNodeWithText(text(R.string.save)).performClick()
        waitForText("Echo da cozinha")
        compose.onNodeWithText("Echo da cozinha").assertIsDisplayed()
    }

    @Test
    fun invalidFormShowsErrorsAndDoesNotLeaveTheScreen() {
        openTab(R.string.tab_devices)
        compose.onNodeWithContentDescription(text(R.string.devices_add)).performClick()
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.onNodeWithText(text(R.string.form_error_name_required)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.form_error_room_required)).assertIsDisplayed()
    }

    @Test
    fun gateNeedsConfirmationThenOpens() {
        openTab(R.string.tab_gates)
        val open = text(R.string.gate_action_open_named, "Portão da garagem")
        compose.waitUntil(TIMEOUT) { hasNodeWithDescription(open) }
        // Cancel first: the gate must stay closed.
        compose.onNodeWithContentDescription(open).performClick()
        compose.onNodeWithText(text(R.string.gate_confirm_open_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithContentDescription(open).assertIsEnabled()
        // Now confirm.
        compose.onNodeWithContentDescription(open).performClick()
        compose.onNodeWithText(text(R.string.confirm)).performClick()
        compose.waitUntil(TIMEOUT) {
            hasNodeWithDescription(text(R.string.gate_action_close_named, "Portão da garagem"))
        }
    }

    @Test
    fun answerIntercomCall() {
        openTab(R.string.tab_intercom)
        waitForText(text(R.string.intercom_active_ringing))
        compose.onNodeWithText(text(R.string.intercom_answer)).performClick()
        waitForText(text(R.string.intercom_active_answered))
        compose.onNodeWithText(text(R.string.intercom_open_gate)).performClick()
        compose.onNodeWithText(text(R.string.intercom_confirm_title)).assertIsDisplayed()
    }

    @Test
    fun alertsAreListed() {
        runBlocking {
            db.alertDao().insert(
                AlertEntity(type = AlertType.GATE_LEFT_OPEN, subject = "Portão social", createdAtMillis = System.currentTimeMillis()),
            )
        }
        openTab(R.string.tab_alerts)
        waitForText(text(R.string.alert_gate_left_open, "Portão social"))
    }

    @Test
    fun mainControlsMeetTheMinimumTouchTarget() {
        tab(R.string.tab_devices).assertHeightIsAtLeast(48.dp)
        openTab(R.string.tab_devices)
        compose.onNodeWithContentDescription(text(R.string.devices_add)).assertHeightIsAtLeast(48.dp)
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}

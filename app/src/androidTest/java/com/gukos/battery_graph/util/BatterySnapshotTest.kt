package com.gukos.battery_graph.util

import android.content.Intent
import android.os.BatteryManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatterySnapshotTest {

    private fun batteryIntent(
        level: Int? = 80,
        scale: Int? = 100,
        plugged: Int? = 0,
        status: Int? = null,
        temperature: Int? = 250,
        voltage: Int? = 4000
    ) = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
        level?.let { putExtra(BatteryManager.EXTRA_LEVEL, it) }
        scale?.let { putExtra(BatteryManager.EXTRA_SCALE, it) }
        plugged?.let { putExtra(BatteryManager.EXTRA_PLUGGED, it) }
        status?.let { putExtra(BatteryManager.EXTRA_STATUS, it) }
        temperature?.let { putExtra(BatteryManager.EXTRA_TEMPERATURE, it) }
        voltage?.let { putExtra(BatteryManager.EXTRA_VOLTAGE, it) }
    }

    @Test
    fun fromIntent_readsAllValues() {
        val snapshot = BatterySnapshot.fromIntent(
            batteryIntent(plugged = BatteryManager.BATTERY_PLUGGED_USB)
        )

        assertEquals(BatterySnapshot(80, true, 250, 4000), snapshot)
    }

    @Test
    fun fromIntent_convertsToPercentUsingScale() {
        val snapshot = BatterySnapshot.fromIntent(batteryIntent(level = 150, scale = 200))

        assertEquals(75, snapshot?.level)
    }

    @Test
    fun fromIntent_notChargingWhenUnplugged() {
        val snapshot = BatterySnapshot.fromIntent(batteryIntent(plugged = 0))

        assertEquals(false, snapshot?.isCharging)
    }

    @Test
    fun fromIntent_chargeStatusFromStatusExtra() {
        fun statusOf(plugged: Int, status: Int?) =
            BatterySnapshot.fromIntent(batteryIntent(plugged = plugged, status = status))?.chargeStatus

        val ac = BatteryManager.BATTERY_PLUGGED_AC
        assertEquals(ChargeStatus.DISCHARGING, statusOf(0, BatteryManager.BATTERY_STATUS_DISCHARGING))
        assertEquals(ChargeStatus.CHARGING, statusOf(ac, BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals(ChargeStatus.FULL, statusOf(ac, BatteryManager.BATTERY_STATUS_FULL))
        // 充電上限機能などで、接続したまま充電を止めている
        assertEquals(ChargeStatus.PLUGGED_NOT_CHARGING, statusOf(ac, BatteryManager.BATTERY_STATUS_NOT_CHARGING))
        // 給電が弱く、接続中でも減っている
        assertEquals(ChargeStatus.PLUGGED_NOT_CHARGING, statusOf(ac, BatteryManager.BATTERY_STATUS_DISCHARGING))
        // 状態が取れない端末では接続中を充電中とみなす
        assertEquals(ChargeStatus.CHARGING, statusOf(ac, null))
    }

    @Test
    fun fromIntent_returnsNullWithoutLevel() {
        assertNull(BatterySnapshot.fromIntent(batteryIntent(level = null)))
        assertNull(BatterySnapshot.fromIntent(batteryIntent(scale = 0)))
    }

    @Test
    fun fromIntent_optionalValuesAreNullWhenMissing() {
        val snapshot = BatterySnapshot.fromIntent(
            batteryIntent(temperature = null, voltage = null)
        )!!

        assertNull(snapshot.temperature)
        assertNull(snapshot.voltage)
    }
}

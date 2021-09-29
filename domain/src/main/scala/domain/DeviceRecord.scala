package domain

import java.util.UUID

case class DeviceRecord(
    deviceId: UUID,
    currentValue: Float,
    unit: String,
    timestamp: Long,
    version: Float
)

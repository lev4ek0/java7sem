package ru.quipy.shopService2.delivery.logic

import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.shopService2.delivery.api.DeliveryAggregate
import ru.quipy.shopService2.delivery.api.DeliveryCreatedEvent
import java.time.LocalDateTime
import java.util.*


class Delivery : AggregateState<UUID, DeliveryAggregate> {
    private lateinit var deliveryId: UUID
    private lateinit var orderId: UUID

    override fun getId() = deliveryId

    fun createNewDelivery(id: UUID = UUID.randomUUID(), orderId: UUID, warehouseId: UUID): DeliveryCreatedEvent {
        val time = LocalDateTime.now()
        return DeliveryCreatedEvent(deliveryId = id, orderId = orderId, warehouseId = warehouseId, time = time)
    }

    @StateTransitionFunc
    fun createNewDelivery(event: DeliveryCreatedEvent) {
        deliveryId = event.deliveryId
        orderId = event.orderId
    }
}

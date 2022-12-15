package ru.quipy.shopService2.warehouse.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import ru.quipy.core.EventSourcingService
import ru.quipy.shopService2.delivery.api.DeliveryAggregate
import ru.quipy.shopService2.delivery.api.DeliveryCreatedEvent
import ru.quipy.shopService2.warehouse.api.OrderStatus
import ru.quipy.shopService2.warehouse.api.WarehouseAggregate
import ru.quipy.shopService2.warehouse.logic.Warehouse
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct


@Component
class DeliverySubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val warehouseEsService: EventSourcingService<UUID, WarehouseAggregate, Warehouse>
) {
    private val logger: Logger = LoggerFactory.getLogger(DeliverySubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            DeliveryAggregate::class,
            "delivery::create-delivery"
        ) {
            `when`(DeliveryCreatedEvent::class) { event ->
                logger.info("Got delivery to process: $event")

                val delivery =
                    warehouseEsService.update(event.warehouseId) {
                        it.changeOrderStatus(
                            event.orderId,
                            OrderStatus.WAITING_FOR_DELIVERY
                        )
                    }

                warehouseEsService.update(event.warehouseId) {
                    it.setDeliveryTime(orderId = event.orderId, time = event.time)
                }

                logger.info("Delivery: ${event.deliveryId}. Outcomes: $delivery")
            }
        }
    }
}
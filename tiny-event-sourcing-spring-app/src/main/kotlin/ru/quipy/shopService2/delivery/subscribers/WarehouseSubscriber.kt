package ru.quipy.shopService2.delivery.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import ru.quipy.core.EventSourcingService
import ru.quipy.shopService2.delivery.api.DeliveryAggregate
import ru.quipy.shopService2.delivery.logic.Delivery
import ru.quipy.shopService2.warehouse.api.OrderStatusPayedEvent
import ru.quipy.shopService2.warehouse.api.WarehouseAggregate
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct


@Component
class WarehouseSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val deliveryEsService: EventSourcingService<UUID, DeliveryAggregate, Delivery>
) {
    private val logger: Logger = LoggerFactory.getLogger(WarehouseSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            WarehouseAggregate::class,
            "warehouse::book-c"
        ) {
            `when`(OrderStatusPayedEvent::class) { event ->
                logger.info("Got delivery to process: $event")

                val delivery =
                    deliveryEsService.create {
                        it.createNewDelivery(
                            orderId = event.orderId,
                            warehouseId = event.warehouseId
                        )
                    }

                logger.info("Warehouse: ${event.warehouseId}. Outcomes: $delivery")
            }
        }
    }
}
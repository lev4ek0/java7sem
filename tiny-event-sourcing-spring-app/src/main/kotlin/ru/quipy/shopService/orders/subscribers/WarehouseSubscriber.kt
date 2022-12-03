package ru.quipy.shopService.warehouse.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import ru.quipy.core.EventSourcingService
import ru.quipy.shopService.orders.api.OrderAggregate
import ru.quipy.shopService.orders.api.ProductAddedToOrderEvent
import ru.quipy.shopService.orders.api.ProductRemovedFromOrder
import ru.quipy.shopService.orders.logic.Order
import ru.quipy.shopService.warehouse.api.ProductBookAmount
import ru.quipy.shopService.warehouse.api.ProductUnBookAmount
import ru.quipy.shopService.warehouse.api.WarehouseAggregate
import ru.quipy.shopService.warehouse.logic.Warehouse
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct


@Component
class WarehouseSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val orderEsService: EventSourcingService<UUID, OrderAggregate, Order>
) {
    private val logger: Logger = LoggerFactory.getLogger(WarehouseSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            WarehouseAggregate::class,
            "warehouse::book-confirm"
        ) {
            `when`(ProductBookAmount::class) { event ->
                logger.info("Got book to process: $event")

                val book =
                    orderEsService.update(event.orderId) { // todo sukhoa idempotence!
                        it.confirmAddProduct(
                            event.warehouseId,
                            event.productId,
                            event.amount
                        )
                    }

                logger.info("Product: ${event.productId}. Outcomes: $book")
            }
            `when`(ProductUnBookAmount::class) { event ->
                logger.info("Got unbook to process: $event")
                val unbook =
                    orderEsService.update(event.orderId) { // todo sukhoa idempotence!
                        it.confirmRemoveProduct(
                            event.warehouseId,
                            event.productId,
                            event.amount
                        )
                    }

                logger.info("Product: ${event.productId}. Outcomes: $unbook")
            }
        }
    }
}
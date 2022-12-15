package ru.quipy.shopService2.warehouse.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

const val WAREHOUSE_CREATED = "WAREHOUSE_CREATED"
const val ORDER_CREATED = "ORDER_CREATED"
const val DELIVERY_TIME = "DELIVERY_TIME"
const val PRODUCT_ADDED_TO_WAREHOUSE = "PRODUCT_ADDED_TO_WAREHOUSE"
const val PRODUCT_REMOVED_FROM_WAREHOUSE = "PRODUCT_REMOVED_FROM_WAREHOUSE"
const val PRODUCT_BOOKED = "PRODUCT_BOOKED"
const val PRODUCT_UNBOOKED = "PRODUCT_UNBOOKED"
const val ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED"
const val ORDER_STATUS_PAYED = "ORDER_STATUS_PAYED"
const val ORDER_STATUS_DELIVERED = "ORDER_STATUS_DELIVERED"

enum class OrderStatus {
    STARTED, WAITING_FOR_PAYMENT, PAYED, WAITING_FOR_DELIVERY, DELIVERED, CANCELLED
}


@DomainEvent(name = WAREHOUSE_CREATED)
data class WarehouseCreatedEvent(
    val warehouseId: UUID,
) : Event<WarehouseAggregate>(
    name = WAREHOUSE_CREATED,
)


@DomainEvent(name = ORDER_CREATED)
data class OrderCreatedEvent(
    val warehouseId: UUID,
    val orderId: UUID,
) : Event<WarehouseAggregate>(
    name = ORDER_CREATED,
)


@DomainEvent(name = DELIVERY_TIME)
data class DeliveryTimeEvent(
    val warehouseId: UUID,
    val orderId: UUID,
    val time: LocalDateTime,
) : Event<WarehouseAggregate>(
    name = DELIVERY_TIME,
)


@DomainEvent(name = PRODUCT_ADDED_TO_WAREHOUSE)
data class ProductAddedToWarehouseEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val title: String,
    val price: BigDecimal,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_ADDED_TO_WAREHOUSE,
)


@DomainEvent(name = PRODUCT_REMOVED_FROM_WAREHOUSE)
data class ProductRemovedFromWarehouseEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_REMOVED_FROM_WAREHOUSE,
)


@DomainEvent(name = PRODUCT_BOOKED)
data class ProductBookedEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val orderId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_BOOKED,
)


@DomainEvent(name = PRODUCT_UNBOOKED)
data class ProductUnBookedEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val orderId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_UNBOOKED,
)


@DomainEvent(name = ORDER_STATUS_CHANGED)
data class OrderStatusChangedEvent(
    val warehouseId: UUID,
    val orderId: UUID,
    val status: OrderStatus,
) : Event<WarehouseAggregate>(
    name = ORDER_STATUS_CHANGED,
)


@DomainEvent(name = ORDER_STATUS_DELIVERED)
data class OrderStatusDeliveredEvent(
    val warehouseId: UUID,
    val orderId: UUID,
    val status: OrderStatus,
) : Event<WarehouseAggregate>(
    name = ORDER_STATUS_DELIVERED,
)


@DomainEvent(name = ORDER_STATUS_PAYED)
data class OrderStatusPayedEvent(
    val warehouseId: UUID,
    val orderId: UUID,
    val status: OrderStatus,
) : Event<WarehouseAggregate>(
    name = ORDER_STATUS_PAYED,
)

package ru.quipy.shopService.orders.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import ru.quipy.shopService.warehouse.logic.Warehouse
import java.math.BigDecimal
import java.util.*

const val ORDER_CREATED = "ORDER_CREATED"
const val PRODUCT_ADDED_TO_ORDER = "PRODUCT_ADDED_TO_ORDER"
const val CONFIRM_PRODUCT_ADDED_TO_ORDER = "CONFIRM_PRODUCT_ADDED_TO_ORDER"
const val PRODUCT_REMOVED_FROM_ORDER = "PRODUCT_REMOVED_FROM_ORDER"
const val CONFIRM_PRODUCT_REMOVED_FROM_ORDER = "CONFIRM_PRODUCT_REMOVED_FROM_ORDER"


@DomainEvent(name = ORDER_CREATED)
data class OrderCreatedEvent(
    val orderId: UUID,
) : Event<OrderAggregate>(
    name = ORDER_CREATED,
)


@DomainEvent(name = PRODUCT_ADDED_TO_ORDER)
data class ProductAddedToOrderEvent(
    val warehouseId: UUID,
    val orderId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<OrderAggregate>(
    name = PRODUCT_ADDED_TO_ORDER,
)

@DomainEvent(name = CONFIRM_PRODUCT_ADDED_TO_ORDER)
data class ConfirmProductAddedToOrderEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<OrderAggregate>(
    name = CONFIRM_PRODUCT_ADDED_TO_ORDER,
)

@DomainEvent(name = PRODUCT_REMOVED_FROM_ORDER)
data class ProductRemovedFromOrder(
    val warehouseId: UUID,
    val orderId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<OrderAggregate>(
    name = PRODUCT_REMOVED_FROM_ORDER,
)

@DomainEvent(name = CONFIRM_PRODUCT_REMOVED_FROM_ORDER)
data class ConfirmProductRemovedFromOrderEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<OrderAggregate>(
    name = CONFIRM_PRODUCT_REMOVED_FROM_ORDER,
)

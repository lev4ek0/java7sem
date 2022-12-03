package ru.quipy.shopService.warehouse.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.*

const val PRODUCT_DECREASE_AMOUNT = "PRODUCT_DECREASE_AMOUNT"
const val PRODUCT_INCREASE_AMOUNT = "PRODUCT_INCREASE_AMOUNT"
const val PRODUCT_BOOK_AMOUNT = "PRODUCT_BOOK_AMOUNT"
const val PRODUCT_UNBOOK_AMOUNT = "PRODUCT_UNBOOK_AMOUNT"
const val WAREHOUSE_CREATED = "WAREHOUSE_CREATED"
const val PRODUCT_CREATED = "PRODUCT_CREATED"
const val PRODUCT_DELETED = "PRODUCT_DELETED"
const val PRODUCT_BOOK_REJECT = "PRODUCT_BOOK_REJECT"


@DomainEvent(name = WAREHOUSE_CREATED)
data class WarehouseCreatedEvent(
    val warehouseId: UUID
) : Event<WarehouseAggregate>(
    name = WAREHOUSE_CREATED,
)


@DomainEvent(name = PRODUCT_DECREASE_AMOUNT)
data class ProductDecreaseAmount(
    val warehouseId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_DECREASE_AMOUNT,
)

@DomainEvent(name = PRODUCT_INCREASE_AMOUNT)
data class ProductIncreaseAmount(
    val warehouseId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_INCREASE_AMOUNT,
)

@DomainEvent(name = PRODUCT_BOOK_AMOUNT)
data class ProductBookAmount(
    val warehouseId: UUID,
    val orderId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_BOOK_AMOUNT,
)

@DomainEvent(name = PRODUCT_UNBOOK_AMOUNT)
data class ProductUnBookAmount(
    val warehouseId: UUID,
    val orderId: UUID,
    val productId: UUID,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_UNBOOK_AMOUNT,
)

@DomainEvent(name = PRODUCT_CREATED)
data class ProductCreatedEvent(
    val warehouseId: UUID,
    val productId: UUID,
    val title: String,
    val price: BigDecimal,
    val amount: BigDecimal,
) : Event<WarehouseAggregate>(
    name = PRODUCT_CREATED,
)

@DomainEvent(name = PRODUCT_DELETED)
data class ProductDeletedEvent(
    val warehouseId: UUID,
    val productId: UUID,
) : Event<WarehouseAggregate>(
    name = PRODUCT_DELETED,
)

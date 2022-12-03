package ru.quipy.shopService.orders.logic

import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.shopService.orders.api.*
import ru.quipy.shopService.warehouse.logic.Warehouse
import java.math.BigDecimal
import java.util.*


class Order : AggregateState<UUID, OrderAggregate> {
    private lateinit var orderId: UUID
    var products: MutableMap<UUID, BigDecimal> = mutableMapOf()

    override fun getId() = orderId

    fun createNewOrder(id: UUID = UUID.randomUUID()): OrderCreatedEvent {
        return OrderCreatedEvent(id)
    }

    fun addProductToOrder(warehouseId: UUID, productId: UUID, amount: BigDecimal): ProductAddedToOrderEvent {
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductAddedToOrderEvent(warehouseId, orderId, productId, amount)
    }

    fun removeProductFromOrder(warehouseId: UUID, productId: UUID, amount: BigDecimal): ProductRemovedFromOrder {
        val product = (products[productId]
            ?: throw IllegalArgumentException("No such product to remove from cart: $productId"))
        if (product - amount < BigDecimal.ZERO) {
            throw IllegalStateException("Product amount can't be negative")
        }
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductRemovedFromOrder(warehouseId, orderId, productId, amount)
    }

    fun confirmAddProduct(warehouseId: UUID, productId: UUID, amount: BigDecimal): ConfirmProductAddedToOrderEvent {
        return ConfirmProductAddedToOrderEvent(warehouseId, productId, amount)
    }

    fun confirmRemoveProduct(warehouseId: UUID, productId: UUID, amount: BigDecimal): ConfirmProductRemovedFromOrderEvent {
        return ConfirmProductRemovedFromOrderEvent(warehouseId, productId, amount)
    }

    @StateTransitionFunc
    fun createNewOrder(event: OrderCreatedEvent) {
        orderId = event.orderId
    }

    @StateTransitionFunc
    fun addProductToOrder(event: ConfirmProductAddedToOrderEvent) {
        val nowAmount = products[event.productId]
        if (nowAmount != null) {
            products[event.productId] = nowAmount + event.amount
        } else {
            products[event.productId] = event.amount
        }
    }

    @StateTransitionFunc
    fun removeProductFromOrder(event: ConfirmProductRemovedFromOrderEvent) {
        val willAmount = products[event.productId]!! - event.amount
        if (willAmount == BigDecimal.ZERO) {
            products.remove(event.productId)
        } else {
            products[event.productId] = willAmount
        }
    }


    @StateTransitionFunc
    fun removeProductFromOrder(event: ProductRemovedFromOrder) {

    }

    @StateTransitionFunc
    fun addProductToOrder(event: ProductAddedToOrderEvent) {

    }
}


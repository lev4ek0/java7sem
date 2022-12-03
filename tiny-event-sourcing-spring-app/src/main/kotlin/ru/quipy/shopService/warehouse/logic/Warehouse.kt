package ru.quipy.shopService.warehouse.logic

import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.shopService.warehouse.api.*
import java.math.BigDecimal
import java.util.*


class Warehouse : AggregateState<UUID, WarehouseAggregate> {
    private lateinit var warehouseId: UUID

    var products: MutableMap<UUID, Product> = mutableMapOf()

    override fun getId() = warehouseId

    fun createNewWarehouse(id: UUID = UUID.randomUUID()): WarehouseCreatedEvent {
        return WarehouseCreatedEvent(id)
    }

    fun createNewProduct(
        id: UUID = UUID.randomUUID(),
        title: String,
        amount: BigDecimal,
        price: BigDecimal
    ): ProductCreatedEvent {
        if (amount <= BigDecimal.ZERO)
            throw IllegalStateException("Warehouse $warehouseId can't have negative amount: $amount")

        return ProductCreatedEvent(warehouseId, id, title, price, amount)
    }

    fun deleteProduct(productId: UUID): ProductDeletedEvent {
        return ProductDeletedEvent(warehouseId, productId)
    }

    fun decreaseAmount(productId: UUID, amount: BigDecimal): ProductDecreaseAmount {
        val product = (products[productId]
            ?: throw IllegalArgumentException("No such product to decrease amount: $productId"))
        if (product.amount - amount < BigDecimal.ZERO) {
            throw IllegalStateException("Product amount can't be negative")
        }
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductDecreaseAmount(warehouseId, productId, amount)
    }

    fun increaseAmount(productId: UUID, amount: BigDecimal): ProductIncreaseAmount {
        products[productId] ?: throw IllegalArgumentException("No such product to increase amount: $productId")
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductIncreaseAmount(warehouseId, productId, amount)
    }

    fun bookAmount(orderId: UUID, productId: UUID, amount: BigDecimal): ProductBookAmount {
        val product = (products[productId]
            ?: throw IllegalArgumentException("No such product to book amount: $productId"))
        if (product.amount - amount < BigDecimal.ZERO) {
            throw IllegalStateException("Product amount can't be negative")
        }
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductBookAmount(warehouseId, orderId, productId, amount)
    }

    fun unBookAmount(orderId: UUID, productId: UUID, amount: BigDecimal): ProductUnBookAmount {
        products[productId] ?: throw IllegalArgumentException("No such product to unbook amount: $productId")
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductUnBookAmount(warehouseId, orderId, productId, amount)
    }

    @StateTransitionFunc
    fun createNewWarehouse(event: WarehouseCreatedEvent) {
        warehouseId = event.warehouseId
    }

    @StateTransitionFunc
    fun createNewProduct(event: ProductCreatedEvent) {
        products[event.productId] = Product(event.productId, event.title, event.price, event.amount)
    }

    @StateTransitionFunc
    fun deleteProduct(event: ProductDeletedEvent) {
        products.remove(event.productId)
    }

    @StateTransitionFunc
    fun decreaseAmount(event: ProductDecreaseAmount) {
        products[event.productId]!!.decreaseAmount(event.amount)
    }

    @StateTransitionFunc
    fun increaseAmount(event: ProductIncreaseAmount) {
        products[event.productId]!!.increaseAmount(event.amount)
    }

    @StateTransitionFunc
    fun bookAmount(event: ProductBookAmount) {
        products[event.productId]!!.bookAmount(event.amount)
    }

    @StateTransitionFunc
    fun unBookAmount(event: ProductUnBookAmount) {
        products[event.productId]!!.unBookAmount(event.amount)
    }
}

data class Product(
    val id: UUID,
    var title: String,
    var price: BigDecimal,
    var amount: BigDecimal,
    internal var booked_amount: BigDecimal = BigDecimal.ZERO
) {
    fun decreaseAmount(amount: BigDecimal) {
        this.amount -= amount
    }

    fun increaseAmount(amount: BigDecimal) {
        this.amount += amount
    }

    fun bookAmount(amount: BigDecimal) {
        this.amount -= amount
        this.booked_amount += amount
    }

    fun unBookAmount(amount: BigDecimal) {
        this.amount += amount
        this.booked_amount -= amount
    }
}

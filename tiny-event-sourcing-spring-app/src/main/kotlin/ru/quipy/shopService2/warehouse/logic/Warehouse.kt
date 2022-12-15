package ru.quipy.shopService2.warehouse.logic

import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.domain.Event
import ru.quipy.shopService2.warehouse.api.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


class Warehouse : AggregateState<UUID, WarehouseAggregate> {
    private lateinit var warehouseId: UUID
    var orders: MutableMap<UUID, Order> = mutableMapOf()
    var products: MutableMap<UUID, Product> = mutableMapOf()

    override fun getId() = warehouseId

    fun createNewWarehouse(id: UUID = UUID.randomUUID()): WarehouseCreatedEvent {
        return WarehouseCreatedEvent(id)
    }

    fun createNewOrder(id: UUID = UUID.randomUUID()): OrderCreatedEvent {
        return OrderCreatedEvent(warehouseId, id)
    }

    fun setDeliveryTime(orderId: UUID, time: LocalDateTime): DeliveryTimeEvent {
        return DeliveryTimeEvent(warehouseId, orderId, time)
    }

    fun bookProduct(productId: UUID, orderId: UUID, amount: BigDecimal): ProductBookedEvent {
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        if (products[productId]!!.amount - amount < BigDecimal.ZERO) {
            throw IllegalStateException("Can't be negative amount of products at warehouse")
        }
        return ProductBookedEvent(warehouseId, productId, orderId, amount)
    }

    fun unBookProduct(productId: UUID, orderId: UUID, amount: BigDecimal): ProductUnBookedEvent {
        val product = (products[productId]
            ?: throw IllegalArgumentException("No such product to remove from cart: $productId"))
        if (product.amount - amount < BigDecimal.ZERO) {
            throw IllegalStateException("Product amount can't be negative")
        }
        if (amount <= BigDecimal.ZERO) {
            throw IllegalStateException("Amount must be positive")
        }
        return ProductUnBookedEvent(warehouseId, productId, orderId, amount)
    }

    fun addProductToWarehouse(id: UUID = UUID.randomUUID(), title: String, price: BigDecimal, amount: BigDecimal): ProductAddedToWarehouseEvent {
        return ProductAddedToWarehouseEvent(warehouseId, id, title, price, amount)
    }

    fun removeProductFromWarehouse(id: UUID, amount: BigDecimal): ProductRemovedFromWarehouseEvent {
        return ProductRemovedFromWarehouseEvent(warehouseId, id, amount)
    }

    fun changeOrderStatus(orderId: UUID, status: OrderStatus): Event<WarehouseAggregate> {
        if (status == OrderStatus.DELIVERED) {
            return OrderStatusDeliveredEvent(warehouseId, orderId, status)
        } else if (status == OrderStatus.PAYED) {
            return OrderStatusPayedEvent(warehouseId, orderId, status)
        } else {
            return OrderStatusChangedEvent(warehouseId, orderId, status)
        }
    }

    @StateTransitionFunc
    fun createNewWarehouse(event: WarehouseCreatedEvent) {
        warehouseId = event.warehouseId
    }

    @StateTransitionFunc
    fun createNewOrder(event: OrderCreatedEvent) {
        orders[event.orderId] = Order()
    }

    @StateTransitionFunc
    fun bookProduct(event: ProductBookedEvent) {
        val product = products[event.productId]!!
        product.bookAmount(event.amount)
        val order = orders[event.orderId]!!
        order.increaseAmount(event.productId, event.amount)
    }

    @StateTransitionFunc
    fun unBookProduct(event: ProductUnBookedEvent) {
        val product = products[event.productId]!!
        product.unBookAmount(event.amount)
        val order = orders[event.orderId]!!
        order.decreaseAmount(event.productId, event.amount)
    }


    @StateTransitionFunc
    fun addProductToWarehouse(event: ProductAddedToWarehouseEvent) {
        val product = products[event.productId]
        if (product != null) {
            product.increaseAmount(event.amount)
        } else {
            products[event.productId] = Product(event.title, event.price, event.amount)
        }
    }

    @StateTransitionFunc
    fun removeProductFromWarehouse(event: ProductRemovedFromWarehouseEvent) {
        val product = products[event.productId]!!
        product.decreaseAmount(event.amount)
        if (product.amount == BigDecimal.ZERO) {
            products.remove(event.productId)
        }
    }

    @StateTransitionFunc
    fun setDeliveryTime(event: DeliveryTimeEvent) {
        orders[event.orderId]!!.setTime(event.time)
    }

    @StateTransitionFunc
    fun changeOrderStatus(event: OrderStatusDeliveredEvent) {
        val order = orders[event.orderId]!!
        order.changeStatus(event.status)
    }

    @StateTransitionFunc
    fun change1OrderStatus(event: OrderStatusPayedEvent) {
        val order = orders[event.orderId]!!
        order.changeStatus(event.status)
    }

    @StateTransitionFunc
    fun change2OrderStatus(event: OrderStatusChangedEvent) {
        val order = orders[event.orderId]!!
        order.changeStatus(event.status)
    }
}

data class Product(
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


data class Order(
    var deliveryTime: LocalDateTime? = null,
    internal var products: MutableMap<UUID, BigDecimal> = mutableMapOf(),
    internal var status: OrderStatus = OrderStatus.STARTED,
    internal var price: BigDecimal = BigDecimal.ZERO
) {
    fun decreaseAmount(productId: UUID, amount: BigDecimal) {
        var productAmount = this.products[productId]
        if (productAmount != null) {
            productAmount -= amount
            if (productAmount == BigDecimal.ZERO) {
                this.products.remove(productId)
            }
        }
    }

    fun increaseAmount(productId: UUID, amount: BigDecimal) {
        val productAmount = this.products[productId]
        if (productAmount != null) {
            this.products[productId] = this.products[productId]!! + amount
        } else {
            this.products[productId] = amount
        }
    }

    fun setTime(time: LocalDateTime) {
        this.deliveryTime = time
    }

    fun changeStatus(status: OrderStatus) {
        this.status = status
    }
}
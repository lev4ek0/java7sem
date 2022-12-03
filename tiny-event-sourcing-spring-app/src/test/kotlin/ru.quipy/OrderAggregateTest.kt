package ru.quipy

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import ru.quipy.core.EventSourcingService
import ru.quipy.shopService.orders.api.OrderAggregate
import ru.quipy.shopService.orders.logic.Order
import ru.quipy.shopService.warehouse.api.WarehouseAggregate
import ru.quipy.shopService.warehouse.logic.Product
import ru.quipy.shopService.warehouse.logic.Warehouse
import java.math.BigDecimal
import java.util.*

@SpringBootTest
class OrderAggregateTest {
    companion object {
        private val orderId = UUID.randomUUID()
        private val productId = UUID.randomUUID()
        private val warehouseId = UUID.randomUUID()
        private val amount = BigDecimal(9)
    }

    @Autowired
    private lateinit var orderEsService: EventSourcingService<UUID, OrderAggregate, Order>

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun init() {
        cleanDatabase()
    }

    fun cleanDatabase() {
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(orderId)), "orders")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(orderId)), "snapshots")
    }

    @Test
    fun createOrder() {
        orderEsService.create {
            it.createNewOrder(id = orderId)
        }

        val state = orderEsService.getState(orderId)!!

        Assertions.assertEquals(orderId, state.getId())
    }

    @Test
    fun confirmProductAdding() {
        orderEsService.create {
            it.createNewOrder(id = orderId)
        }

        orderEsService.update(orderId) {
            it.confirmAddProduct(warehouseId = warehouseId, productId = productId, amount = amount)
        }

        val state = orderEsService.getState(orderId)!!

        Assertions.assertEquals(1, state.products.size)
        Assertions.assertEquals(state.products[productId], amount)
    }

    @Test
    fun confirmProductRemoving() {
        orderEsService.create {
            it.createNewOrder(id = orderId)
        }

        orderEsService.update(orderId) {
            it.confirmAddProduct(warehouseId = warehouseId, productId = productId, amount = amount)
        }

        orderEsService.update(orderId) {
            it.confirmRemoveProduct(warehouseId = warehouseId, productId = productId, amount = amount)
        }

        val state = orderEsService.getState(orderId)!!

        Assertions.assertEquals(0, state.products.size)
    }
}
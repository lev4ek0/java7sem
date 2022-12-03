package ru.quipy

import org.awaitility.kotlin.await
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
import ru.quipy.shopService.warehouse.logic.Warehouse
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class BookProductTest {
    companion object {
        private val orderId = UUID.randomUUID()
        private val productId = UUID.randomUUID()
        private val warehouseId = UUID.randomUUID()
        private val amount = BigDecimal(9)
        private val to_book = BigDecimal.ONE
        private val price = BigDecimal(100)
        private val title = "Iphone 14 pro"
    }

    @Autowired
    private lateinit var orderEsService: EventSourcingService<UUID, OrderAggregate, Order>

    @Autowired
    private lateinit var warehouseEsService: EventSourcingService<UUID, WarehouseAggregate, Warehouse>

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun init() {
        cleanDatabase()
    }

    fun cleanDatabase() {
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(orderId)), "orders")
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(warehouseId)), "warehouse")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(orderId)), "snapshots")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(warehouseId)), "snapshots")
    }


    @Test
    fun addProductToOrder() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.createNewProduct(id = productId, title = title, price = price, amount = amount)
        }


        orderEsService.create {
            it.createNewOrder(id = orderId)
        }

        val numberOfThreads = 50
        val service = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(numberOfThreads)
        for (i in 0 until numberOfThreads) {
            service.submit {
                orderEsService.update(orderId) {
                    it.addProductToOrder(warehouseId = warehouseId, productId = productId, amount = to_book)
                }
                latch.countDown()
            }
        }
        latch.await()


        await.atMost(10, TimeUnit.MINUTES).until {
            warehouseEsService.getState(warehouseId)!!.products[productId]!!.amount == BigDecimal.ZERO
        }

        val state = warehouseEsService.getState(warehouseId)!!

        Assertions.assertEquals(state.products[productId]!!.booked_amount, amount)

        await.atMost(10, TimeUnit.MINUTES).until {
            orderEsService.getState(orderId)!!.products[productId] == amount
        }
    }
}
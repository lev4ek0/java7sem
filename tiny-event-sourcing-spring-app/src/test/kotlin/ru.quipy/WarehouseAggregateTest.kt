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
import ru.quipy.shopService.warehouse.api.WarehouseAggregate
import ru.quipy.shopService.warehouse.logic.Warehouse
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors


@SpringBootTest
class WarehouseAggregateTest {
    companion object {
        private val warehouseId = UUID.randomUUID()
        private const val title = "Iphone 14 pro"
        private val price = BigDecimal(100)
        private val amount = BigDecimal.TEN
    }

    @Autowired
    private lateinit var warehouseEsService: EventSourcingService<UUID, WarehouseAggregate, Warehouse>

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun init() {
        cleanDatabase()
    }

    fun cleanDatabase() {
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(warehouseId)), "warehouse")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(warehouseId)), "snapshots")
    }

    @Test
    fun createWarehouse() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val state = warehouseEsService.getState(warehouseId)!!

        Assertions.assertEquals(warehouseId, state.getId())
    }

    @Test
    fun createProduct() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        val state = warehouseEsService.getState(warehouseId)!!

        Assertions.assertEquals(warehouseId, state.getId())
        Assertions.assertEquals(1, state.products.size)
        Assertions.assertNotNull(state.products[createdEvent.productId])
        Assertions.assertEquals(createdEvent.productId, state.products[createdEvent.productId]!!.id)
        Assertions.assertEquals(BigDecimal.ZERO, state.products[createdEvent.productId]!!.booked_amount)
    }

    @Test
    fun deleteProduct() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEventNew = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        val createdEventDelete = warehouseEsService.update(warehouseId) {
            it.deleteProduct(createdEventNew.productId)
        }

        val state = warehouseEsService.getState(warehouseId)!!

        Assertions.assertEquals(0, state.products.size)
    }

    @Test
    fun increaseProductAmount() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        val someEvent = warehouseEsService.update(warehouseId) {
            it.increaseAmount(createdEvent.productId, BigDecimal.TEN)
        }
        val state = warehouseEsService.getState(warehouseId)!!

        Assertions.assertEquals(BigDecimal(20), state.products[createdEvent.productId]!!.amount)
    }

    @Test
    fun decreaseProductAmount() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        val someEvent = warehouseEsService.update(warehouseId) {
            it.decreaseAmount(createdEvent.productId, BigDecimal.TEN)
        }
        val state = warehouseEsService.getState(warehouseId)!!

        Assertions.assertEquals(BigDecimal.ZERO, state.products[createdEvent.productId]!!.amount)
    }

    @Test
    fun increaseProductAmountException() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            warehouseEsService.update(warehouseId) {
                it.increaseAmount(createdEvent.productId, BigDecimal(-1))
            }
        }
    }

    @Test
    fun decreaseProductAmountException() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            warehouseEsService.update(warehouseId) {
                it.decreaseAmount(createdEvent.productId, BigDecimal(-1))
            }
        }
    }

    @Test
    fun decreaseProductNegativeAmountException() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            warehouseEsService.update(warehouseId) {
                it.decreaseAmount(createdEvent.productId, BigDecimal(11))
            }
        }
    }

    @Test
    fun increaseAmountMultiThreading() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val createdEvent = warehouseEsService.update(warehouseId) {
            it.createNewProduct(title = title, price = price, amount = amount)
        }

        val numberOfThreads = 50
        val service = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(numberOfThreads)
        for (i in 0 until numberOfThreads) {
            service.submit {
                warehouseEsService.update(warehouseId) {
                    it.increaseAmount(createdEvent.productId, BigDecimal.TEN)
                }
                latch.countDown()
            }
        }
        latch.await()

        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertEquals(BigDecimal(510), state.products[createdEvent.productId]!!.amount)
    }
}
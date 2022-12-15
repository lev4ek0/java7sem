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
import ru.quipy.shopService2.warehouse.api.WarehouseAggregate
import ru.quipy.shopService2.warehouse.logic.Warehouse
import java.lang.Exception
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class BookProductTest {
    companion object {
        private val orderId = UUID.randomUUID()
        private val orderId2 = UUID.randomUUID()
        private val orderId3 = UUID.randomUUID()
        private val orderId4 = UUID.randomUUID()
        private val productId = UUID.randomUUID()
        private val warehouseId = UUID.randomUUID()
        private val amount = BigDecimal(9)
        private val to_book = BigDecimal.ONE
        private val price = BigDecimal(100)
        private val title = "Iphone 14 pro"
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
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(orderId)), "orders")
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(warehouseId)), "warehouse")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(orderId)), "snapshots")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(warehouseId)), "snapshots")
    }


    @Test
    fun createWarehouse() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertEquals(state.getId(), warehouseId)
    }

    @Test
    fun addProduct() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.addProductToWarehouse(id = productId, title = title, price = price, amount = amount)
        }

        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertNotNull(state.products[productId])
        Assertions.assertEquals(state.products[productId]!!.amount, amount)
    }

    @Test
    fun createOrder() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.createNewOrder(id = orderId)
        }

        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertNotNull(state.orders[orderId])
    }

    @Test
    fun addProductToOrder() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.addProductToWarehouse(id = productId, title = title, price = price, amount = amount)
        }

        warehouseEsService.update(warehouseId) {
            it.createNewOrder(id = orderId)
        }


        warehouseEsService.update(warehouseId) {
            it.bookProduct(productId = productId, orderId = orderId, amount = BigDecimal(3))
        }


        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertEquals(state.orders[orderId]!!.products[productId], BigDecimal(3))
        Assertions.assertEquals(state.products[productId]!!.amount, BigDecimal(6))
        Assertions.assertEquals(state.products[productId]!!.booked_amount, BigDecimal(3))
    }

    @Test
    fun addProductToOrderThreading() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.addProductToWarehouse(id = productId, title = title, price = price, amount = amount)
        }

        warehouseEsService.update(warehouseId) {
            it.createNewOrder(id = orderId)
        }

        val numberOfThreads = 50
        val service = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(numberOfThreads)
        for (i in 0 until numberOfThreads) {
            service.submit {
                try {
                    warehouseEsService.update(warehouseId) {
                        it.bookProduct(productId = productId, orderId = orderId, amount = BigDecimal(3))
                    }
                } catch (_: Exception) {

                }
                latch.countDown()
            }
        }
        latch.await()

        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertEquals(state.orders[orderId]!!.products[productId], BigDecimal(9))
        Assertions.assertEquals(state.products[productId]!!.amount, BigDecimal.ZERO)
        Assertions.assertEquals(state.products[productId]!!.booked_amount, BigDecimal(9))
    }

    @Test
    fun addProductToOrders() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.addProductToWarehouse(id = productId, title = title, price = price, amount = BigDecimal(2698))
        }


//        val orderIds: Array<UUID> = arrayOf(orderId, orderId2, orderId3, orderId4)
        val orderIds: ArrayList<UUID> = arrayListOf()

        for(n in 1..900){
            val uuid = UUID.randomUUID()
            orderIds.add(uuid)
            warehouseEsService.update(warehouseId) {
                it.createNewOrder(id = uuid)
            }

        }

        val factIds: ArrayList<UUID> = arrayListOf()
        val numberOfThreads = 30
        val service = Executors.newFixedThreadPool(30)
        val latch = CountDownLatch(numberOfThreads)
        for (i in 0 until numberOfThreads) {
            service.submit {
                for (n in 0..29) {
                    try {
                        warehouseEsService.update(warehouseId) {
                            it.bookProduct(productId = productId, orderId = orderIds[i * 30 + n], amount = BigDecimal(3))
                        }
                    } catch (_: Exception) {

                    } finally {
                        factIds.add(orderIds[i * 30 + n])
                    }

                }

                latch.countDown()
            }
        }
        latch.await()
        println(factIds.size)
        val state = warehouseEsService.getState(warehouseId)!!
        Assertions.assertEquals(state.orders[factIds[0]]!!.products[productId], BigDecimal(3))
        Assertions.assertEquals(state.orders[factIds[1]]!!.products[productId], BigDecimal(3))
        Assertions.assertEquals(state.orders[factIds[2]]!!.products[productId], BigDecimal(3))
//        Assertions.assertEquals(state.products[productId]!!.booked_amount, BigDecimal(2697))
        val bookedAmount = state.products[productId]!!.booked_amount
        val nullAmount = (BigDecimal(2700) - bookedAmount) / BigDecimal(3)
        println(bookedAmount)
        var inc = 0
        for (i in 0..899) {
            if (state.orders[factIds[i]]!!.products[productId] == null) {
                inc++
            }
        }
        Assertions.assertEquals(nullAmount, BigDecimal(inc))
        Assertions.assertNull(state.orders[factIds[899]]!!.products[productId])
    }

    @Test
    fun addRandomAmountOfProductsToOrderThreading() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.addProductToWarehouse(id = productId, title = title, price = price, amount = BigDecimal(250))
        }

        warehouseEsService.update(warehouseId) {
            it.createNewOrder(id = orderId)
        }

        val numberOfThreads = 100
        val service = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(numberOfThreads)
        var total = 0
        for (i in 0 until numberOfThreads) {
            service.submit {
                val rand = (1..10).random()
                try {
                    warehouseEsService.update(warehouseId) {
                        it.bookProduct(productId = productId, orderId = orderId, amount = BigDecimal(rand))
                    }
                } catch (_: Exception) {
                    total -= rand
                } finally {
                    total += rand
                }
                latch.countDown()
            }
        }
        latch.await()

        val state = warehouseEsService.getState(warehouseId)!!
        println(total)
        Assertions.assertEquals(state.orders[orderId]!!.products[productId], BigDecimal(total))
        Assertions.assertEquals(state.products[productId]!!.amount, BigDecimal(250 - total))
        Assertions.assertEquals(state.products[productId]!!.booked_amount, BigDecimal(total))
    }
}
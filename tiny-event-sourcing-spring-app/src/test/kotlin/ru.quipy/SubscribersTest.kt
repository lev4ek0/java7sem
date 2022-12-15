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
import ru.quipy.shopService2.warehouse.api.OrderStatus
import ru.quipy.shopService2.warehouse.api.WarehouseAggregate
import ru.quipy.shopService2.warehouse.logic.Warehouse
import java.lang.Exception
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

@SpringBootTest
class SubscribersTest {
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
    fun deliverySubscriber() {
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

        warehouseEsService.update(warehouseId) {
            it.changeOrderStatus(orderId, OrderStatus.PAYED)
        }

        await.atMost(10, TimeUnit.MINUTES).until {
            warehouseEsService.getState(warehouseId)!!.orders[orderId]!!.status == OrderStatus.WAITING_FOR_DELIVERY
        }

        Assertions.assertNotNull(warehouseEsService.getState(warehouseId)!!.orders[orderId]!!.deliveryTime)
    }

    @Test
    fun deliverySubscriberMany() {
        warehouseEsService.create {
            it.createNewWarehouse(id = warehouseId)
        }

        warehouseEsService.update(warehouseId) {
            it.addProductToWarehouse(id = productId, title = title, price = price, amount = amount)
        }

        val orderIds: ArrayList<UUID> = arrayListOf()

        for(n in 0..899){
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
                            it.changeOrderStatus(orderIds[i * 30 + n], OrderStatus.PAYED)
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

        await.atMost(10, TimeUnit.MINUTES).until {
            warehouseEsService.getState(warehouseId)!!.orders[factIds[899]]!!.status == OrderStatus.WAITING_FOR_DELIVERY
        }

        val warehouseState = warehouseEsService.getState(warehouseId)!!

        var counter: Int = 0;

        for (i in 0..898) {
            if (warehouseState.orders[factIds[i]]!!.deliveryTime!! < warehouseState.orders[factIds[i + 1]]!!.deliveryTime!!) {
                counter += 1
            }
        }

        Assertions.assertEquals(counter, 898)
    }
}
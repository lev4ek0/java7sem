package ru.quipy.shopService.orders.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import ru.quipy.shopService.orders.api.OrderAggregate
import ru.quipy.shopService.orders.logic.Order
import java.util.*

@Configuration
class OrderBoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun orderEsService(): EventSourcingService<UUID, OrderAggregate, Order> =
        eventSourcingServiceFactory.create()
}
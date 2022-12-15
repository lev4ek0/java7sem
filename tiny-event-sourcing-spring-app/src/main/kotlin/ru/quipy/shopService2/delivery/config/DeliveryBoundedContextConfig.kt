package ru.quipy.shopService2.delivery.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import ru.quipy.shopService2.delivery.api.DeliveryAggregate
import ru.quipy.shopService2.delivery.logic.Delivery

import java.util.*

@Configuration
class DeliveryBoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun deliveryEsService(): EventSourcingService<UUID, DeliveryAggregate, Delivery> =
        eventSourcingServiceFactory.create()
}
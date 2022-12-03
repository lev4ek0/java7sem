package ru.quipy.shopService.warehouse.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import ru.quipy.shopService.warehouse.api.WarehouseAggregate
import ru.quipy.shopService.warehouse.logic.Warehouse
import java.util.*

@Configuration
class WarehouseBoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun warehouseEsService(): EventSourcingService<UUID, WarehouseAggregate, Warehouse> =
        eventSourcingServiceFactory.create()
}
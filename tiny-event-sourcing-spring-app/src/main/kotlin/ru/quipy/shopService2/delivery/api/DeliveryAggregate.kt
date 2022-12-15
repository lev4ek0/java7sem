package ru.quipy.shopService2.delivery.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "delivery")
class DeliveryAggregate: Aggregate

package ru.quipy.shopService.orders.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "orders")
class OrderAggregate: Aggregate
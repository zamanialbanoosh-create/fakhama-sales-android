package com.fakhama.sales

import java.util.UUID

enum class AppLanguage { FA, AR, EN }
enum class OrderStatus { DRAFT, NEW, SENT, DELIVERED, CANCELLED }

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val area: String = "",
    val marketer: String = "",
    val openingBalance: Double = 0.0
)

data class Fabric(
    val id: String = UUID.randomUUID().toString(),
    val catalog: String,
    val code: String,
    val color: String,
    val unit: String,
    val stock: Double,
    val salePrice: Double,
    val lowStockAt: Double = 10.0
)

data class OrderLine(
    val fabricId: String,
    val catalog: String,
    val code: String,
    val color: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double
) {
    val total: Double get() = quantity * unitPrice
}

data class SalesOrder(
    val id: String = UUID.randomUUID().toString(),
    val number: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val marketer: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lines: List<OrderLine>,
    val discount: Double = 0.0,
    val paid: Double = 0.0,
    val note: String = "",
    val status: OrderStatus = OrderStatus.NEW
) {
    val subtotal: Double get() = lines.sumOf { it.total }
    val total: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val balance: Double get() = total - paid
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

package com.fakhama.sales

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("fakhama_sales_store", Context.MODE_PRIVATE)

    fun loadCustomers(): MutableList<Customer> {
        val raw = prefs.getString("customers", null) ?: return sampleCustomers().toMutableList()
        return runCatching {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                Customer(o.getString("id"), o.getString("name"), o.getString("phone"), o.optString("area"), o.optString("marketer"), o.optDouble("openingBalance"))
            }
        }.getOrElse { sampleCustomers().toMutableList() }
    }

    fun saveCustomers(items: List<Customer>) {
        val a = JSONArray()
        items.forEach { c -> a.put(JSONObject().apply {
            put("id", c.id); put("name", c.name); put("phone", c.phone); put("area", c.area); put("marketer", c.marketer); put("openingBalance", c.openingBalance)
        }) }
        prefs.edit().putString("customers", a.toString()).apply()
    }

    fun loadFabrics(): MutableList<Fabric> {
        val raw = prefs.getString("fabrics", null) ?: return sampleFabrics().toMutableList()
        return runCatching {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                Fabric(o.getString("id"), o.getString("catalog"), o.getString("code"), o.optString("color"), o.optString("unit", "m"), o.optDouble("stock"), o.optDouble("salePrice"), o.optDouble("lowStockAt", 10.0))
            }
        }.getOrElse { sampleFabrics().toMutableList() }
    }

    fun saveFabrics(items: List<Fabric>) {
        val a = JSONArray()
        items.forEach { f -> a.put(JSONObject().apply {
            put("id", f.id); put("catalog", f.catalog); put("code", f.code); put("color", f.color); put("unit", f.unit); put("stock", f.stock); put("salePrice", f.salePrice); put("lowStockAt", f.lowStockAt)
        }) }
        prefs.edit().putString("fabrics", a.toString()).apply()
    }

    fun loadOrders(): MutableList<SalesOrder> {
        val raw = prefs.getString("orders", null) ?: return mutableListOf()
        return runCatching {
            val a = JSONArray(raw)
            MutableList(a.length()) { i -> orderFromJson(a.getJSONObject(i)) }
        }.getOrElse { mutableListOf() }
    }

    fun saveOrders(items: List<SalesOrder>) {
        val a = JSONArray(); items.forEach { a.put(orderToJson(it)) }
        prefs.edit().putString("orders", a.toString()).apply()
    }

    fun loadPayments(): MutableList<Payment> {
        val raw = prefs.getString("payments", null) ?: return mutableListOf()
        return runCatching {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                Payment(o.getString("id"), o.getString("customerId"), o.getString("customerName"), o.optDouble("amount"), o.optString("note"), o.optLong("createdAt"))
            }
        }.getOrElse { mutableListOf() }
    }

    fun savePayments(items: List<Payment>) {
        val a = JSONArray(); items.forEach { p -> a.put(JSONObject().apply {
            put("id", p.id); put("customerId", p.customerId); put("customerName", p.customerName); put("amount", p.amount); put("note", p.note); put("createdAt", p.createdAt)
        }) }
        prefs.edit().putString("payments", a.toString()).apply()
    }

    fun loadLanguage(): AppLanguage = runCatching { AppLanguage.valueOf(prefs.getString("language", "AR") ?: "AR") }.getOrDefault(AppLanguage.AR)
    fun saveLanguage(language: AppLanguage) { prefs.edit().putString("language", language.name).apply() }

    private fun orderToJson(o: SalesOrder) = JSONObject().apply {
        put("id", o.id); put("number", o.number); put("customerId", o.customerId); put("customerName", o.customerName); put("customerPhone", o.customerPhone)
        put("marketer", o.marketer); put("createdAt", o.createdAt); put("discount", o.discount); put("paid", o.paid); put("note", o.note); put("status", o.status.name)
        put("lines", JSONArray().apply { o.lines.forEach { l -> put(JSONObject().apply {
            put("fabricId", l.fabricId); put("catalog", l.catalog); put("code", l.code); put("color", l.color); put("quantity", l.quantity); put("unit", l.unit); put("unitPrice", l.unitPrice)
        }) } })
    }

    private fun orderFromJson(o: JSONObject): SalesOrder {
        val a = o.getJSONArray("lines")
        val lines = List(a.length()) { i -> a.getJSONObject(i).let { l -> OrderLine(l.getString("fabricId"), l.getString("catalog"), l.getString("code"), l.optString("color"), l.optDouble("quantity"), l.optString("unit"), l.optDouble("unitPrice")) } }
        return SalesOrder(o.getString("id"), o.getString("number"), o.getString("customerId"), o.getString("customerName"), o.optString("customerPhone"), o.optString("marketer"), o.optLong("createdAt"), lines, o.optDouble("discount"), o.optDouble("paid"), o.optString("note"), runCatching { OrderStatus.valueOf(o.optString("status", "NEW")) }.getOrDefault(OrderStatus.NEW))
    }

    private fun sampleCustomers() = listOf(
        Customer(name = "Al Noor Tailoring", phone = "50000001", area = "Kuwait", marketer = "Hadi"),
        Customer(name = "Al Fakhama Customer", phone = "50000002", area = "Ahmadi", marketer = "Hadi")
    )

    private fun sampleFabrics() = listOf(
        Fabric(catalog = "Mestre Winter", code = "M-108", color = "White", unit = "m", stock = 124.0, salePrice = 3.750, lowStockAt = 20.0),
        Fabric(catalog = "Mestre Winter", code = "M-115", color = "Cream", unit = "m", stock = 62.0, salePrice = 3.950, lowStockAt = 20.0),
        Fabric(catalog = "Royal", code = "R-204", color = "White", unit = "m", stock = 18.0, salePrice = 4.500, lowStockAt = 20.0),
        Fabric(catalog = "Royal", code = "R-219", color = "Beige", unit = "m", stock = 8.0, salePrice = 4.750, lowStockAt = 15.0)
    )
}

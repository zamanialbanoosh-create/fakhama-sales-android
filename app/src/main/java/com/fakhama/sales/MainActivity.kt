package com.fakhama.sales

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FakhamaSalesApp() }
    }
}

private val Navy = Color(0xFF102A43)
private val Gold = Color(0xFFC59A3D)
private val Cream = Color(0xFFF6F3EA)
private val Green = Color(0xFF198754)
private val Red = Color(0xFFB42318)

enum class Screen { HOME, NEW_ORDER, CUSTOMERS, CATALOG, INVENTORY, ORDERS, ACCOUNTS, SETTINGS }
data class DraftLine(val fabric: Fabric, val quantity: Double, val price: Double)

@Composable
fun FakhamaSalesApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }
    var language by remember { mutableStateOf(store.loadLanguage()) }
    val direction = if (language == AppLanguage.EN) LayoutDirection.Ltr else LayoutDirection.Rtl
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Gold, background = Cream, surface = Color.White)) {
            SalesApp(store, language) { language = it; store.saveLanguage(it) }
        }
    }
}

@Composable
private fun SalesApp(store: AppStore, language: AppLanguage, onLanguage: (AppLanguage) -> Unit) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var customers by remember { mutableStateOf(store.loadCustomers().toList()) }
    var fabrics by remember { mutableStateOf(store.loadFabrics().toList()) }
    var orders by remember { mutableStateOf(store.loadOrders().toList()) }
    var payments by remember { mutableStateOf(store.loadPayments().toList()) }
    val t: (String) -> String = { I18n.t(language, it) }

    Surface(Modifier.fillMaxSize(), color = Cream) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = Navy, contentColor = Color.White) {
                Spacer(Modifier.height(10.dp))
                Text("F", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Gold, modifier = Modifier.padding(12.dp))
                NavButton("⌂", t("home"), screen == Screen.HOME) { screen = Screen.HOME }
                NavButton("＋", t("newOrder"), screen == Screen.NEW_ORDER) { screen = Screen.NEW_ORDER }
                NavButton("👥", t("customers"), screen == Screen.CUSTOMERS) { screen = Screen.CUSTOMERS }
                NavButton("▦", t("catalog"), screen == Screen.CATALOG) { screen = Screen.CATALOG }
                NavButton("▣", t("inventory"), screen == Screen.INVENTORY) { screen = Screen.INVENTORY }
                NavButton("☷", t("orders"), screen == Screen.ORDERS) { screen = Screen.ORDERS }
                NavButton("KD", t("accounts"), screen == Screen.ACCOUNTS) { screen = Screen.ACCOUNTS }
                Spacer(Modifier.weight(1f))
                NavButton("⚙", t("settings"), screen == Screen.SETTINGS) { screen = Screen.SETTINGS }
                Spacer(Modifier.height(10.dp))
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(18.dp)) {
                when (screen) {
                    Screen.HOME -> Dashboard(t, orders, fabrics, customers, payments) { screen = it }
                    Screen.NEW_ORDER -> NewOrderScreen(t, customers, fabrics, onAddCustomer = { c -> customers = customers + c; store.saveCustomers(customers) }, onSave = { order, updatedFabrics -> orders = listOf(order) + orders; fabrics = updatedFabrics; store.saveOrders(orders); store.saveFabrics(fabrics) })
                    Screen.CUSTOMERS -> CustomersScreen(t, customers) { c -> customers = customers + c; store.saveCustomers(customers) }
                    Screen.CATALOG -> CatalogScreen(t, fabrics) { f -> fabrics = fabrics + f; store.saveFabrics(fabrics) }
                    Screen.INVENTORY -> InventoryScreen(t, fabrics)
                    Screen.ORDERS -> OrdersScreen(t, orders)
                    Screen.ACCOUNTS -> AccountsScreen(t, customers, orders, payments) { p -> payments = listOf(p) + payments; store.savePayments(payments) }
                    Screen.SETTINGS -> SettingsScreen(t, language, onLanguage)
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationRailItem(selected = selected, onClick = onClick, icon = { Text(icon, fontSize = 18.sp, color = if (selected) Navy else Color.White) }, label = { Text(label, fontSize = 10.sp, color = Color.White) }, colors = NavigationRailItemDefaults.colors(selectedIconColor = Navy, indicatorColor = Gold, selectedTextColor = Color.White, unselectedTextColor = Color.White))
}

@Composable
private fun Dashboard(t: (String) -> String, orders: List<SalesOrder>, fabrics: List<Fabric>, customers: List<Customer>, payments: List<Payment>, open: (Screen) -> Unit) {
    val today = dayKey(System.currentTimeMillis())
    val todayOrders = orders.filter { dayKey(it.createdAt) == today }
    val receivables = customers.sumOf { c -> customerBalance(c, orders, payments) }.coerceAtLeast(0.0)
    Column(Modifier.fillMaxSize()) {
        Header(t("app"), "Tablet • Offline")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(t("todaySales"), kd(todayOrders.sumOf { it.total }), Modifier.weight(1f))
            StatCard(t("todayOrders"), todayOrders.size.toString(), Modifier.weight(1f))
            StatCard(t("receivables"), kd(receivables), Modifier.weight(1f))
            StatCard(t("lowStock"), fabrics.count { it.stock <= it.lowStockAt }.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BigAction("＋", t("newOrder"), Modifier.weight(1f)) { open(Screen.NEW_ORDER) }
            BigAction("👥", t("customers"), Modifier.weight(1f)) { open(Screen.CUSTOMERS) }
            BigAction("▦", t("catalog"), Modifier.weight(1f)) { open(Screen.CATALOG) }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BigAction("▣", t("inventory"), Modifier.weight(1f)) { open(Screen.INVENTORY) }
            BigAction("☷", t("orders"), Modifier.weight(1f)) { open(Screen.ORDERS) }
            BigAction("KD", t("accounts"), Modifier.weight(1f)) { open(Screen.ACCOUNTS) }
        }
    }
}

@Composable private fun Header(title: String, sub: String = "") { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column { Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Navy); if (sub.isNotBlank()) Text(sub, color = Color.Gray) } } }
@Composable private fun StatCard(label: String, value: String, modifier: Modifier) { Card(modifier.height(105.dp), shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color.Gray); Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy) } } }
@Composable private fun BigAction(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) { Card(onClick = onClick, modifier = modifier.height(145.dp), shape = RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon, fontSize = 34.sp, color = Gold); Spacer(Modifier.height(10.dp)); Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy) } } }

@Composable
private fun NewOrderScreen(t: (String) -> String, customers: List<Customer>, fabrics: List<Fabric>, onAddCustomer: (Customer) -> Unit, onSave: (SalesOrder, List<Fabric>) -> Unit) {
    val context = LocalContext.current
    var customer by remember { mutableStateOf<Customer?>(null) }
    var customerSearch by remember { mutableStateOf("") }
    var fabricSearch by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(listOf<DraftLine>()) }
    var discount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showNewCustomer by remember { mutableStateOf(false) }
    var lastOrder by remember { mutableStateOf<SalesOrder?>(null) }
    val filteredCustomers = customers.filter { customerSearch.isBlank() || it.name.contains(customerSearch, true) || it.phone.contains(customerSearch) }.take(8)
    val filteredFabrics = fabrics.filter { fabricSearch.isBlank() || it.code.contains(fabricSearch, true) || it.catalog.contains(fabricSearch, true) || it.color.contains(fabricSearch, true) }

    Column(Modifier.fillMaxSize()) {
        Header(t("newOrder"), if (customer == null) t("selectCustomer") else customer!!.name)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.weight(1.05f).fillMaxHeight(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    if (customer == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(customerSearch, { customerSearch = it }, label = { Text(t("search")) }, modifier = Modifier.weight(1f)); Button(onClick = { showNewCustomer = true }, modifier = Modifier.height(56.dp)) { Text("＋ ${t("addCustomer")}") } }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn { items(filteredCustomers) { c -> ListItem(headlineContent = { Text(c.name, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${c.phone} • ${c.area}") }, modifier = Modifier.clickable { customer = c }) } }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(customer!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("${customer!!.phone} • ${customer!!.area}", color = Color.Gray) }; TextButton(onClick = { customer = null }) { Text(t("back")) } }
                        OutlinedTextField(fabricSearch, { fabricSearch = it }, label = { Text("${t("search")} • ${t("code")}") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        LazyColumn { items(filteredFabrics) { f -> FabricPickRow(t, f) { qty, price -> val existing = draft.indexOfFirst { it.fabric.id == f.id }; draft = if (existing < 0) draft + DraftLine(f, qty, price) else draft.mapIndexed { i, d -> if (i == existing) d.copy(quantity = d.quantity + qty, price = price) else d } } } }
                    }
                }
            }
            Card(Modifier.weight(.95f).fillMaxHeight(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Text(t("cart"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f)) { items(draft) { d -> Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${d.fabric.code} • ${d.fabric.color}", fontWeight = FontWeight.Bold); Text("${d.quantity.clean()} ${d.fabric.unit} × ${kd(d.price)}", color = Color.Gray) }; Text(kd(d.quantity * d.price)); TextButton(onClick = { draft = draft.filterNot { it === d } }) { Text("×", color = Red) } }; HorizontalDivider() } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(discount, { discount = numeric(it) }, label = { Text(t("discount")) }, modifier = Modifier.weight(1f)); OutlinedTextField(paid, { paid = numeric(it) }, label = { Text(t("paid")) }, modifier = Modifier.weight(1f)) }
                    OutlinedTextField(note, { note = it }, label = { Text(t("note")) }, modifier = Modifier.fillMaxWidth())
                    val subtotal = draft.sumOf { it.quantity * it.price }; val total = (subtotal - (discount.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(t("total"), fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(kd(total), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gold) }
                    Button(enabled = customer != null && draft.isNotEmpty(), onClick = {
                        val c = customer ?: return@Button
                        val number = "F-${SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())}"
                        val order = SalesOrder(number = number, customerId = c.id, customerName = c.name, customerPhone = c.phone, marketer = c.marketer, lines = draft.map { OrderLine(it.fabric.id, it.fabric.catalog, it.fabric.code, it.fabric.color, it.quantity, it.fabric.unit, it.price) }, discount = discount.toDoubleOrNull() ?: 0.0, paid = paid.toDoubleOrNull() ?: 0.0, note = note)
                        val updated = fabrics.map { f -> val sold = draft.filter { it.fabric.id == f.id }.sumOf { it.quantity }; if (sold > 0) f.copy(stock = (f.stock - sold).coerceAtLeast(0.0)) else f }
                        onSave(order, updated); lastOrder = order; draft = emptyList(); discount = ""; paid = ""; note = ""
                    }, modifier = Modifier.fillMaxWidth().height(58.dp)) { Text(t("confirmOrder"), fontSize = 18.sp) }
                    if (lastOrder != null) { Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { shareWhatsApp(context, lastOrder!!) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("WhatsApp • ${lastOrder!!.number}") } }
                }
            }
        }
    }
    if (showNewCustomer) CustomerDialog(t, onDismiss = { showNewCustomer = false }, onSave = { onAddCustomer(it); customer = it; showNewCustomer = false })
}

@Composable
private fun FabricPickRow(t: (String) -> String, f: Fabric, add: (Double, Double) -> Unit) {
    var qty by remember(f.id) { mutableStateOf("1") }; var price by remember(f.id) { mutableStateOf(f.salePrice.toString()) }
    ListItem(headlineContent = { Text("${f.catalog} • ${f.code}", fontWeight = FontWeight.Bold) }, supportingContent = { Text("${f.color} • ${t("stock")}: ${f.stock.clean()} ${f.unit}") }, trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(qty, { qty = numeric(it) }, modifier = Modifier.width(72.dp), singleLine = true); Spacer(Modifier.width(6.dp)); OutlinedTextField(price, { price = numeric(it) }, modifier = Modifier.width(90.dp), singleLine = true); Spacer(Modifier.width(6.dp)); FilledTonalButton(enabled = (qty.toDoubleOrNull() ?: 0.0) > 0 && (qty.toDoubleOrNull() ?: 0.0) <= f.stock, onClick = { add(qty.toDoubleOrNull() ?: 1.0, price.toDoubleOrNull() ?: f.salePrice) }) { Text("＋") } } })
}

@Composable
private fun CustomersScreen(t: (String) -> String, customers: List<Customer>, add: (Customer) -> Unit) {
    var q by remember { mutableStateOf("") }; var dialog by remember { mutableStateOf(false) }
    Column { Header(t("customers"), customers.size.toString()); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(q, { q = it }, label = { Text(t("search")) }, modifier = Modifier.weight(1f)); Button(onClick = { dialog = true }, modifier = Modifier.height(56.dp)) { Text("＋ ${t("addCustomer")}") } }; LazyColumn { items(customers.filter { q.isBlank() || it.name.contains(q, true) || it.phone.contains(q) }) { c -> ListItem(headlineContent = { Text(c.name, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${c.phone} • ${c.area} • ${c.marketer}") }) } } }
    if (dialog) CustomerDialog(t, { dialog = false }) { add(it); dialog = false }
}

@Composable
private fun CustomerDialog(t: (String) -> String, onDismiss: () -> Unit, onSave: (Customer) -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var area by remember { mutableStateOf("") }; var marketer by remember { mutableStateOf("") }; var balance by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(t("addCustomer")) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedTextField(name, { name = it }, label = { Text(t("name")) }); OutlinedTextField(phone, { phone = it }, label = { Text(t("phone")) }); OutlinedTextField(area, { area = it }, label = { Text(t("area")) }); OutlinedTextField(marketer, { marketer = it }, label = { Text(t("marketer")) }); OutlinedTextField(balance, { balance = numeric(it) }, label = { Text(t("openingBalance")) }) } }, confirmButton = { Button(enabled = name.isNotBlank() && phone.isNotBlank(), onClick = { onSave(Customer(name = name.trim(), phone = phone.trim(), area = area.trim(), marketer = marketer.trim(), openingBalance = balance.toDoubleOrNull() ?: 0.0)) }) { Text(t("save")) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(t("cancel")) } })
}

@Composable
private fun CatalogScreen(t: (String) -> String, fabrics: List<Fabric>, add: (Fabric) -> Unit) {
    var q by remember { mutableStateOf("") }; var dialog by remember { mutableStateOf(false) }
    Column { Header(t("catalog"), fabrics.map { it.catalog }.distinct().size.toString()); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(q, { q = it }, label = { Text(t("search")) }, modifier = Modifier.weight(1f)); Button(onClick = { dialog = true }, modifier = Modifier.height(56.dp)) { Text("＋ ${t("addFabric")}") } }; LazyColumn { items(fabrics.filter { q.isBlank() || it.catalog.contains(q, true) || it.code.contains(q, true) || it.color.contains(q, true) }) { f -> ListItem(headlineContent = { Text("${f.catalog} • ${f.code}", fontWeight = FontWeight.Bold) }, supportingContent = { Text("${f.color} • ${f.stock.clean()} ${f.unit}") }, trailingContent = { Text(kd(f.salePrice), color = Gold, fontWeight = FontWeight.Bold) }) } } }
    if (dialog) FabricDialog(t, { dialog = false }) { add(it); dialog = false }
}

@Composable
private fun FabricDialog(t: (String) -> String, onDismiss: () -> Unit, onSave: (Fabric) -> Unit) {
    var catalog by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var color by remember { mutableStateOf("") }; var unit by remember { mutableStateOf("m") }; var stock by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(t("addFabric")) }, text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { OutlinedTextField(catalog, { catalog = it }, label = { Text(t("catalogName")) }); OutlinedTextField(code, { code = it }, label = { Text(t("code")) }); OutlinedTextField(color, { color = it }, label = { Text(t("color")) }); OutlinedTextField(unit, { unit = it }, label = { Text(t("unit")) }); OutlinedTextField(stock, { stock = numeric(it) }, label = { Text(t("stock")) }); OutlinedTextField(price, { price = numeric(it) }, label = { Text(t("price")) }) } }, confirmButton = { Button(enabled = catalog.isNotBlank() && code.isNotBlank(), onClick = { onSave(Fabric(catalog = catalog, code = code, color = color, unit = unit.ifBlank { "m" }, stock = stock.toDoubleOrNull() ?: 0.0, salePrice = price.toDoubleOrNull() ?: 0.0)) }) { Text(t("save")) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(t("cancel")) } })
}

@Composable
private fun InventoryScreen(t: (String) -> String, fabrics: List<Fabric>) { Column { Header(t("inventory"), "${fabrics.count { it.stock <= it.lowStockAt }} ${t("lowStock")}"); Spacer(Modifier.height(10.dp)); LazyColumn { items(fabrics.sortedBy { it.stock }) { f -> Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = if (f.stock <= f.lowStockAt) Color(0xFFFFE8E5) else Color.White)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${f.catalog} • ${f.code}", fontWeight = FontWeight.Bold); Text(f.color, color = Color.Gray) }; Text("${f.stock.clean()} ${f.unit}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (f.stock <= f.lowStockAt) Red else Green) } } } } } }

@Composable
private fun OrdersScreen(t: (String) -> String, orders: List<SalesOrder>) { val context = LocalContext.current; Column { Header(t("orders"), orders.size.toString()); Spacer(Modifier.height(10.dp)); if (orders.isEmpty()) Text(t("empty")) else LazyColumn { items(orders) { o -> Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${o.number} • ${o.customerName}", fontWeight = FontWeight.Bold); Text("${dateTime(o.createdAt)} • ${o.lines.size} items", color = Color.Gray) }; Column(horizontalAlignment = Alignment.End) { Text(kd(o.total), fontWeight = FontWeight.Bold, color = Gold); Text("${t("balance")}: ${kd(o.balance)}", color = if (o.balance > 0) Red else Green) }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { shareWhatsApp(context, o) }) { Text("WhatsApp") } } } } } } }

@Composable
private fun AccountsScreen(t: (String) -> String, customers: List<Customer>, orders: List<SalesOrder>, payments: List<Payment>, addPayment: (Payment) -> Unit) {
    var selected by remember { mutableStateOf<Customer?>(null) }; var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    Column { Header(t("accounts")); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Card(Modifier.weight(1f).fillMaxHeight()) { LazyColumn { items(customers.sortedByDescending { customerBalance(it, orders, payments) }) { c -> val b = customerBalance(c, orders, payments); ListItem(headlineContent = { Text(c.name, fontWeight = FontWeight.Bold) }, supportingContent = { Text(c.phone) }, trailingContent = { Text(kd(b), color = if (b > 0) Red else Green, fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { selected = c }) } } }; Card(Modifier.weight(1f).fillMaxHeight()) { Column(Modifier.fillMaxSize().padding(16.dp)) { val c = selected; if (c == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(t("selectCustomer")) } else { Text(c.name, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("${t("balance")}: ${kd(customerBalance(c, orders, payments))}", fontSize = 22.sp, color = Red); Spacer(Modifier.height(15.dp)); OutlinedTextField(amount, { amount = numeric(it) }, label = { Text(t("paid")) }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(note, { note = it }, label = { Text(t("note")) }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)); Button(enabled = (amount.toDoubleOrNull() ?: 0.0) > 0, onClick = { addPayment(Payment(customerId = c.id, customerName = c.name, amount = amount.toDoubleOrNull() ?: 0.0, note = note)); amount = ""; note = "" }, modifier = Modifier.fillMaxWidth().height(55.dp)) { Text(t("newPayment")) }; Spacer(Modifier.height(10.dp)); LazyColumn { items(payments.filter { it.customerId == c.id }) { p -> ListItem(headlineContent = { Text(kd(p.amount), color = Green, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${dateTime(p.createdAt)} • ${p.note}") }) } } } } } } } }

@Composable
private fun SettingsScreen(t: (String) -> String, language: AppLanguage, onLanguage: (AppLanguage) -> Unit) { Column { Header(t("settings")); Spacer(Modifier.height(20.dp)); Text(t("language"), fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = language == AppLanguage.AR, onClick = { onLanguage(AppLanguage.AR) }, label = { Text("العربية") }); FilterChip(selected = language == AppLanguage.FA, onClick = { onLanguage(AppLanguage.FA) }, label = { Text("فارسی") }); FilterChip(selected = language == AppLanguage.EN, onClick = { onLanguage(AppLanguage.EN) }, label = { Text("English") }) }; Spacer(Modifier.height(24.dp)); Text("Fakhama Sales Android • v0.2", color = Color.Gray); Text("Offline data is stored on this tablet.", color = Color.Gray) } }

private fun customerBalance(c: Customer, orders: List<SalesOrder>, payments: List<Payment>): Double = c.openingBalance + orders.filter { it.customerId == c.id }.sumOf { it.total - it.paid } - payments.filter { it.customerId == c.id }.sumOf { it.amount }
private fun kd(v: Double) = String.format(Locale.US, "%.3f KD", v)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
private fun numeric(s: String): String = s.filter { it.isDigit() || it == '.' }.let { x -> if (x.count { it == '.' } <= 1) x else x.dropLast(1) }
private fun dayKey(ms: Long) = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms))
private fun dateTime(ms: Long) = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(ms))
private fun shareWhatsApp(context: android.content.Context, o: SalesOrder) {
    val lines = o.lines.joinToString("\n") { "• ${it.catalog} / ${it.code} / ${it.color} — ${it.quantity.clean()} ${it.unit} × ${kd(it.unitPrice)} = ${kd(it.total)}" }
    val text = "Fakhama Wholesale\nOrder: ${o.number}\nCustomer: ${o.customerName}\nPhone: ${o.customerPhone}\n$lines\nTotal: ${kd(o.total)}\nPaid: ${kd(o.paid)}\nBalance: ${kd(o.balance)}${if (o.note.isNotBlank()) "\nNote: ${o.note}" else ""}"
    val uri = Uri.parse("https://wa.me/?text=${Uri.encode(text)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { context.startActivity(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }) }
}

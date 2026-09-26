package com.fakhama.sales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FakhamaSalesApp() }
    }
}

data class Product(val name: String, val subtitle: String, val price: Double)
data class CartLine(val product: Product, val qty: Int)

private val Gold = Color(0xFFC59A3D)
private val Navy = Color(0xFF102A43)
private val Cream = Color(0xFFF6F3EA)

@Composable
fun FakhamaSalesApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Gold, background = Cream)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Cream) { SalesScreen() }
        }
    }
}

@Composable
private fun SalesScreen() {
    val products = remember {
        listOf(
            Product("تفصيل دشداشة", "خياطة حسب القياس", 6.0),
            Product("قماش فاخر", "اختيار القماش والموديل", 12.0),
            Product("VIP سنوي", "10 دشاديش خلال سنة", 50.0),
            Product("تعديل / إصلاح", "خدمة سريعة", 2.0)
        )
    }
    var cart by remember { mutableStateOf(listOf<CartLine>()) }

    Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(1.65f).fillMaxHeight()) {
            Text("تاج الفخامة", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("نقطة بيع سريعة", fontSize = 16.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            products.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { product ->
                        Card(
                            onClick = {
                                val current = cart.firstOrNull { it.product == product }
                                cart = if (current == null) cart + CartLine(product, 1)
                                else cart.map { if (it.product == product) it.copy(qty = it.qty + 1) else it }
                            },
                            modifier = Modifier.weight(1f).height(150.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(product.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                                    Text(product.subtitle, fontSize = 14.sp, color = Color.Gray)
                                }
                                Text("${product.price.toInt()} د.ك", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        Card(
            Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("الطلب الحالي", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
                Spacer(Modifier.height(14.dp))
                if (cart.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("اضغط على أي خدمة لإضافتها", color = Color.Gray)
                    }
                } else {
                    Column(Modifier.weight(1f)) {
                        cart.forEach { line ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${line.qty} × ${line.product.name}", fontWeight = FontWeight.SemiBold)
                                Text("${"%.3f".format(line.product.price * line.qty)} د.ك")
                            }
                            HorizontalDivider(color = Color(0xFFEAEAEA))
                        }
                    }
                }
                val total = cart.sumOf { it.product.price * it.qty }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${"%.3f".format(total)} د.ك", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { },
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("متابعة الطلب", fontSize = 20.sp) }
                TextButton(onClick = { cart = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("طلب جديد") }
            }
        }
    }
}

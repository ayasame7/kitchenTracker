package com.example.fridgetracker

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Data Models
data class KitchenItem(
    val quantity: Double = 0.0,
    val unit: String = "",
    val category: String = "",
    val expiryDate: Long? = null,
    val addedDate: Long = System.currentTimeMillis()
)

data class KitchenNote(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PredefinedItem(
    val key: String,
    val unit: String,
    val category: String,
    val nameRes: Int
)

val commonKitchenItems = listOf(
    PredefinedItem("milk", "l", "dairy", R.string.item_milk),
    PredefinedItem("eggs", "pcs", "dairy", R.string.item_eggs),
    PredefinedItem("cheese", "kg", "dairy", R.string.item_cheese),
    PredefinedItem("yogurt", "pcs", "dairy", R.string.item_yogurt),
    PredefinedItem("butter", "kg", "dairy", R.string.item_butter),
    PredefinedItem("bread", "pcs", "bakery", R.string.item_bread),
    PredefinedItem("apples", "kg", "fruits", R.string.item_apples),
    PredefinedItem("bananas", "kg", "fruits", R.string.item_bananas),
    PredefinedItem("oranges", "kg", "fruits", R.string.item_oranges),
    PredefinedItem("potatoes", "kg", "vegetables", R.string.item_potatoes),
    PredefinedItem("onions", "kg", "vegetables", R.string.item_onions),
    PredefinedItem("tomatoes", "kg", "vegetables", R.string.item_tomatoes),
    PredefinedItem("garlic", "pcs", "vegetables", R.string.item_garlic),
    PredefinedItem("ginger", "g", "vegetables", R.string.item_ginger),
    PredefinedItem("carrots", "kg", "vegetables", R.string.item_carrots),
    PredefinedItem("cucumber", "kg", "vegetables", R.string.item_cucumber),
    PredefinedItem("lettuce", "pcs", "vegetables", R.string.item_lettuce),
    PredefinedItem("green_pepper", "pcs", "vegetables", R.string.item_green_pepper),
    PredefinedItem("red_pepper", "pcs", "vegetables", R.string.item_red_pepper),
    PredefinedItem("yellow_pepper", "pcs", "vegetables", R.string.item_yellow_pepper),
    PredefinedItem("chicken", "kg", "chicken", R.string.item_chicken),
    PredefinedItem("beef", "kg", "meat", R.string.item_beef),
    PredefinedItem("fish", "kg", "meat", R.string.item_fish),
    PredefinedItem("rice", "kg", "pantry", R.string.item_rice),
    PredefinedItem("pasta", "packet", "pantry", R.string.item_pasta),
    PredefinedItem("flour", "kg", "pantry", R.string.item_flour),
    PredefinedItem("sugar", "kg", "pantry", R.string.item_sugar),
    PredefinedItem("coffee", "g", "pantry", R.string.item_coffee),
    PredefinedItem("tea", "g", "pantry", R.string.item_tea),
    PredefinedItem("salt", "kg", "pantry", R.string.item_salt),
    PredefinedItem("oil", "l", "pantry", R.string.item_oil),
    PredefinedItem("water", "l", "pantry", R.string.item_water),
    PredefinedItem("juice", "l", "pantry", R.string.item_juice),
    PredefinedItem("honey", "jar", "pantry", R.string.item_honey),
    PredefinedItem("jam", "jar", "pantry", R.string.item_jam),
    PredefinedItem("powdered_milk", "bag", "dairy", R.string.item_powdered_milk),
    PredefinedItem("spaghetti", "packet", "pantry", R.string.item_spaghetti),
    PredefinedItem("ditalini", "packet", "pantry", R.string.item_ditalini),
    PredefinedItem("penne", "packet", "pantry", R.string.item_penne),
    PredefinedItem("vermicelli", "packet", "pantry", R.string.item_vermicelli),
    PredefinedItem("orzo", "packet", "pantry", R.string.item_orzo),
    PredefinedItem("yellow_lentils", "kg", "pantry", R.string.item_yellow_lentils),
    PredefinedItem("oats", "packet", "pantry", R.string.item_oats),
    PredefinedItem("kidney_beans", "kg", "pantry", R.string.item_kidney_beans),
    PredefinedItem("various_fruits", "kg", "fruits", R.string.item_various_fruits),
    PredefinedItem("various_vegetables", "kg", "vegetables", R.string.item_various_vegetables),
    PredefinedItem("cheddar_cheese", "kg", "dairy", R.string.item_cheddar_cheese),
    PredefinedItem("romano_cheese", "kg", "dairy", R.string.item_romano_cheese),
    PredefinedItem("white_cheese", "kg", "dairy", R.string.item_white_cheese),
    PredefinedItem("ghee", "kg", "pantry", R.string.item_ghee),
    PredefinedItem("tomato_paste", "jar", "pantry", R.string.item_tomato_paste),
    PredefinedItem("basmati_rice", "kg", "pantry", R.string.item_basmati_rice),
    PredefinedItem("tuna", "can", "pantry", R.string.item_tuna),
    PredefinedItem("ketchup", "bottle", "pantry", R.string.item_ketchup),
    PredefinedItem("mayonnaise", "jar", "pantry", R.string.item_mayonnaise),
    PredefinedItem("baking_powder", "pcs", "pantry", R.string.item_baking_powder),
    PredefinedItem("bean", "kg", "pantry", R.string.item_bean),
    PredefinedItem("corn_flour", "kg", "pantry", R.string.item_corn_flour),
    PredefinedItem("dates", "kg", "fruits", R.string.item_dates),
    PredefinedItem("elbow_pasta", "packet", "pantry", R.string.item_elbow_pasta),
    PredefinedItem("nescafe", "g", "pantry", R.string.item_nescafe),
    PredefinedItem("vanilla_powder", "pcs", "pantry", R.string.item_vanilla_powder),
    PredefinedItem("yeast", "pcs", "pantry", R.string.item_yeast),
    PredefinedItem("molokhia", "bag", "frozen", R.string.item_molokhia),
    PredefinedItem("peas_with_carrots", "bag", "frozen", R.string.item_peas_with_carrots),
    PredefinedItem("peas", "bag", "frozen", R.string.item_peas),
    PredefinedItem("taro", "bag", "frozen", R.string.item_taro),
    PredefinedItem("green_beans", "bag", "frozen", R.string.item_green_beans),
    PredefinedItem("okra", "bag", "frozen", R.string.item_okra),
    PredefinedItem("spinach", "bag", "frozen", R.string.item_spinach),
    PredefinedItem("tomato_juice", "bottle", "pantry", R.string.item_tomato_juice),
    PredefinedItem("falafel", "pcs", "frozen", R.string.item_falafel),
    PredefinedItem("pumpkin", "kg", "vegetables", R.string.item_pumpkin),
    PredefinedItem("pastrami", "kg", "meat", R.string.item_pastrami),
    PredefinedItem("meat_with_vegetables", "kg", "meat", R.string.item_meat_with_vegetables),
    PredefinedItem("entrecote", "kg", "meat", R.string.item_entrecote),
    PredefinedItem("minced_meat", "kg", "meat", R.string.item_minced_meat),
    PredefinedItem("burger", "pcs", "meat", R.string.item_burger),
    PredefinedItem("kofta", "kg", "meat", R.string.item_kofta),
    PredefinedItem("rice_kofta", "kg", "meat", R.string.item_rice_kofta),
    PredefinedItem("liver", "kg", "meat", R.string.item_liver),
    PredefinedItem("chicken_shawarma", "kg", "chicken", R.string.item_chicken_shawarma),
    PredefinedItem("beef_shawarma", "kg", "meat", R.string.item_beef_shawarma),
    PredefinedItem("sausage", "kg", "meat", R.string.item_sausage),
    PredefinedItem("lamb", "kg", "meat", R.string.item_lamb),
    PredefinedItem("beef_steak", "kg", "meat", R.string.item_beef_steak),
    PredefinedItem("farm_frites", "bag", "frozen", R.string.item_farm_frites),
    PredefinedItem("bread_crumbs", "bag", "pantry", R.string.item_bread_crumbs),
    PredefinedItem("vinegar", "bottle", "pantry", R.string.item_vinegar),
    PredefinedItem("corn_oil", "l", "pantry", R.string.item_corn_oil),
    PredefinedItem("olive_oil", "l", "pantry", R.string.item_olive_oil),
    PredefinedItem("apple_cider_vinegar", "bottle", "pantry", R.string.item_apple_cider_vinegar),
    PredefinedItem("anise", "packet", "pantry", R.string.item_anise),
    PredefinedItem("caraway", "packet", "pantry", R.string.item_caraway),
    PredefinedItem("fennel", "packet", "pantry", R.string.item_fennel),
    PredefinedItem("cloves", "g", "pantry", R.string.item_cloves),
    PredefinedItem("nesquik", "can", "pantry", R.string.item_nesquik),
    PredefinedItem("black_pepper", "g", "pantry", R.string.item_black_pepper),
    PredefinedItem("cumin", "g", "pantry", R.string.item_cumin),
    PredefinedItem("coriander", "g", "pantry", R.string.item_coriander),
    PredefinedItem("garlic_powder", "g", "pantry", R.string.item_garlic_powder),
    PredefinedItem("onion_powder", "g", "pantry", R.string.item_onion_powder),

    // Spice Items
    PredefinedItem("paprika", "g", "spices", R.string.item_paprika),
    PredefinedItem("turmeric", "g", "spices", R.string.item_turmeric),
    PredefinedItem("cinnamon", "g", "spices", R.string.item_cinnamon),
    PredefinedItem("ginger_powder", "g", "spices", R.string.item_ginger_powder),
    PredefinedItem("chili_powder", "g", "spices", R.string.item_chili_powder),
    PredefinedItem("cumin_powder", "g", "spices", R.string.item_cumin_powder),
    PredefinedItem("coriander_powder", "g", "spices", R.string.item_coriander_powder),
    PredefinedItem("black_pepper_powder", "g", "spices", R.string.item_black_pepper_powder),
    PredefinedItem("nutmeg", "g", "spices", R.string.item_nutmeg),
    PredefinedItem("cloves_powder", "g", "spices", R.string.item_cloves_powder),
    PredefinedItem("cardamom", "g", "spices", R.string.item_cardamom),
    PredefinedItem("laurel_leaf", "g", "spices", R.string.item_laurel_leaf),
    PredefinedItem("seven_spices", "g", "spices", R.string.item_seven_spices),
    PredefinedItem("meat_spices", "g", "spices", R.string.item_meat_spices),
    PredefinedItem("chicken_spices", "g", "spices", R.string.item_chicken_spices),
    PredefinedItem("sumac", "g", "spices", R.string.item_sumac),
    PredefinedItem("thyme", "g", "spices", R.string.item_thyme),

    PredefinedItem("dish_soap", "bottle", "cleaners", R.string.item_dish_soap),
    PredefinedItem("all_purpose_cleaner", "bottle", "cleaners", R.string.item_all_purpose_cleaner),
    PredefinedItem("glass_cleaner", "bottle", "cleaners", R.string.item_glass_cleaner),
    PredefinedItem("floor_cleaner", "bottle", "cleaners", R.string.item_floor_cleaner),
    PredefinedItem("laundry_detergent", "bottle", "cleaners", R.string.item_laundry_detergent),
    PredefinedItem("bleach", "bottle", "cleaners", R.string.item_bleach),
    PredefinedItem("sponges", "packet", "cleaners", R.string.item_sponges),
    PredefinedItem("fabric_softener", "bottle", "cleaners", R.string.item_fabric_softener),
    PredefinedItem("dishwasher_tablets", "packet", "cleaners", R.string.item_dishwasher_tablets),
    PredefinedItem("toilet_cleaner", "bottle", "cleaners", R.string.item_toilet_cleaner),
    PredefinedItem("degreaser", "bottle", "cleaners", R.string.item_degreaser),
    PredefinedItem("disinfectant_wipes", "packet", "cleaners", R.string.item_disinfectant_wipes),
    PredefinedItem("aluminum_foil", "roll", "cleaners", R.string.item_aluminum_foil),
    PredefinedItem("parchment_paper", "roll", "cleaners", R.string.item_parchment_paper),
    PredefinedItem("storage_bags", "packet", "cleaners", R.string.item_storage_bags),
    PredefinedItem("soap", "pcs", "cleaners", R.string.item_soap),
    PredefinedItem("dishwasher_detergent", "bottle", "cleaners", R.string.item_dishwasher_detergent),
    PredefinedItem("wood_cleaner", "bottle", "cleaners", R.string.item_wood_cleaner),
    PredefinedItem("carpet_cleaner", "bottle", "cleaners", R.string.item_carpet_cleaner),
    PredefinedItem("soft_scouring_pad", "pcs", "cleaners", R.string.item_soft_scouring_pad),
    PredefinedItem("chicken_panee", "kg", "chicken", R.string.item_chicken_panee)
)

fun getEnglishItemName(key: String): String {
    return key.replace("_", " ").replaceFirstChar { it.uppercase() }
}

fun getLocalizedItemNameWithArabic(context: Context, key: String): String {
    val englishName = getEnglishItemName(key)
    val predefined = commonKitchenItems.find { it.key == key }
    if (predefined != null && predefined.nameRes != -1) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag("ar"))
        val arabicContext = context.createConfigurationContext(configuration)
        val arabicName = arabicContext.getString(predefined.nameRes)
        return "$englishName ($arabicName)"
    }
    return englishName
}

fun getLocalizedUnit(context: Context, unit: String): String {
    val resId = context.resources.getIdentifier("unit_$unit", "string", context.packageName)
    return if (resId != 0) context.getString(resId) else unit
}

fun getLocalizedCategory(context: Context, category: String): String {
    val resId = context.resources.getIdentifier("cat_$category", "string", context.packageName)
    return if (resId != 0) context.getString(resId) else category
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )
    
    val panBob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panBob"
    )

    LaunchedEffect(Unit) {
        delay(3500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))),
        contentAlignment = Alignment.Center
    ) {
        KitchenBackground()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Flame
                Text(
                    text = "🔥",
                    fontSize = 50.sp,
                    modifier = Modifier
                        .offset(y = 45.dp)
                        .graphicsLayer(
                            scaleX = flameScale,
                            scaleY = flameScale,
                            alpha = flameScale.coerceIn(0.6f, 1f)
                        )
                )
                // Pan
                Text(
                    text = "🍳",
                    fontSize = 120.sp,
                    modifier = Modifier.offset(y = panBob.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Kitcheneering",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Text(
                text = "Master Your Inventory",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun IntroPage(features: List<String>, startIndex: Int, onSkip: () -> Unit) {
    var visibleFeatures by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in 1..features.size) {
            delay(800)
            visibleFeatures = i
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))),
        contentAlignment = Alignment.Center
    ) {
        KitchenBackground()
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨ Why Kitcheneering?! ✨",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                features.forEachIndexed { index, feature ->
                    AnimatedVisibility(
                        visible = visibleFeatures > index,
                        enter = fadeIn() + slideInHorizontally()
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + startIndex}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = feature,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        @Suppress("ControlFlowWithEmptyBody")
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
        ) {
            Text("Skip >>", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun PromoPage(onSkip: () -> Unit) {
    val promoLines = listOf(
        "“Don’t just cook, Engineer your kitchen.”",
        "“Kitcheneering: Because a smart kitchen starts with smart tracking.”",
        "“From Pantry to Plate, track it all with Kitcheneering.”"
    )
    
    var visibleLines by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        for (i in 1..promoLines.size) {
            delay(1200)
            visibleLines = i
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C)))),
        contentAlignment = Alignment.Center
    ) {
        KitchenBackground()
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            promoLines.forEachIndexed { index, line ->
                AnimatedVisibility(
                    visible = visibleLines > index,
                    enter = fadeIn(animationSpec = tween(1000)) + expandVertically()
                ) {
                    Text(
                        text = line,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        @Suppress("ControlFlowWithEmptyBody")
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
        ) {
            Text("Skip >>", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun SummaryPage(onStart: () -> Unit) {
    val checklistItems = listOf(
        "✅ Tracks what’s available",
        "✅ Know what’s missing",
        "✅ Save time and reduce food waste"
    )
    
    var visibilityStage by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(500)
        visibilityStage = 1 // Title fades in first
        for (i in 1..checklistItems.size) {
            delay(800)
            visibilityStage = 1 + i // Checklist items appear one by one
        }
        delay(800)
        visibilityStage = 5 // Get Started Button fades in
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))),
        contentAlignment = Alignment.Center
    ) {
        KitchenBackground()
        
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main text (center)
            AnimatedVisibility(
                visible = visibilityStage >= 1,
                enter = fadeIn(animationSpec = tween(1200))
            ) {
                Text(
                    text = "Smart Kitcheneering helps you manage your kitchen easily",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Checklist items with slide-in/bounce
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                checklistItems.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = visibilityStage >= (index + 2),
                        enter = fadeIn(tween(600)) + slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        Text(
                            text = item,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Get Started Button
            AnimatedVisibility(
                visible = visibilityStage >= 5,
                enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(60.dp)
                        .graphicsLayer(
                            scaleX = glowScale,
                            scaleY = glowScale
                        )
                        .shadow(elevation = 12.dp * glowAlpha, shape = RoundedCornerShape(30.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("Get Started ✨", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // Force Debug Provider
        Log.d("AppCheck", "Initializing App Check with Debug Provider")
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())

        setContent {
            val context = LocalContext.current
            var introStep by remember { mutableIntStateOf(0) } // 0: Splash, 1: Intro1, 2: Intro2, 3: Promo, 4: Summary, 5: Auth/Main
            var user by remember { mutableStateOf(auth.currentUser) }
            
            // Music logic using DisposableEffect to handle lifecycle properly within Compose
            DisposableEffect(Unit) {
                mediaPlayer = MediaPlayer.create(context, R.raw.a).apply {
                    isLooping = true
                }
                onDispose {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
            }

            LaunchedEffect(introStep) {
                if (introStep in 0..4) {
                    if (mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                } else {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                }
            }

            val features1 = listOf(
                "Private, synced inventory via Firebase Email & Password login 🔒",
                "Real‑time Jetpack Compose interface showing all items instantly 📊",
                "Over 100 predefined essentials across 11 categories (Dairy, Spices, Cleaners, etc.), plus flexible custom ingredient entry",
                "Color‑coded expiry warnings to prevent waste and save money ⚠️",
                "Detects low stock and generates lists with smart thresholds 🛒"
            )

            val features2 = listOf(
                "English interface enriched with Arabic item names 🌍",
                "Editable notepad for recipes, plans, and reminders 📝",
                "Twice‑daily reminders to keep inventory accurate 🔔",
                "Built‑in step‑by‑step guide for mastering features 💡",
                "Interactive onboarding showing time‑saving, waste‑reducing benefits 📖"
            )

            when (introStep) {
                0 -> SplashScreen(onTimeout = { introStep = 1 })
                1 -> IntroPage(features = features1, startIndex = 1, onSkip = { introStep = 2 })
                2 -> IntroPage(features = features2, startIndex = 6, onSkip = { introStep = 3 })
                3 -> PromoPage(onSkip = { introStep = 4 })
                4 -> SummaryPage(onStart = { introStep = 5 })
                else -> {
                    if (user == null) {
                        AuthScreen(onAuthSuccess = { user = auth.currentUser })
                    } else {
                        MainAppScreen(user!!.uid, onLogout = {
                            auth.signOut()
                            user = null
                        })
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Content will resume via LaunchedEffect if needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
fun KitchenBackground() {
    val items = listOf("🍳", "🍎", "🥦", "🥛", "🥩", "🍗", "🍞", "❄️", "🧼", "🧂", "📦", "🥑", "🍔", "🍕", "🍷", "🍰", "🍦", "☕", "🍹")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scope = this
        val spacing = 100.dp
        val columns = (scope.maxWidth / spacing).toInt() + 1
        val rows = (scope.maxHeight / spacing).toInt() + 1
        
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                val index = r * columns + c
                val item = items[index % items.size]
                
                // Hexagonal-style grid to prevent overlap and create a better distribution
                val xOffset = spacing * c + (if (r % 2 == 1) spacing / 2 else 0.dp)
                val yOffset = spacing * r
                
                // Add minor deterministic jitter for a more natural look
                val jitterX = (((r * 31 + c * 71) % 40) - 20).dp
                val jitterY = (((r * 43 + c * 17) % 40) - 20).dp
                
                val rotation = ((r * 31 + c * 71) % 360).toFloat()
                
                Text(
                    text = item,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .offset(x = xOffset + jitterX, y = yOffset + jitterY)
                        .alpha(0.12f)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))),
        contentAlignment = Alignment.Center
    ) {
        KitchenBackground()
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isLogin) stringResource(R.string.title_welcome_back) else stringResource(R.string.title_create_account),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.label_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                @Suppress("ControlFlowWithEmptyBody")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.label_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                if (!isLogin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.label_confirm_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }

                errorMessage?.let {
                    Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill all fields"
                            return@Button
                        }
                        if (!isLogin && password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }

                        if (isLogin) {
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) onAuthSuccess() else errorMessage = task.exception?.message
                            }
                        } else {
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) onAuthSuccess() else errorMessage = task.exception?.message
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(if (isLogin) stringResource(R.string.btn_log_in) else stringResource(R.string.btn_sign_up))
                }

                TextButton(onClick = { isLogin = !isLogin }) {
                    @Suppress("ControlFlowWithEmptyBody")
                    Text(if (isLogin) stringResource(R.string.btn_no_account) else stringResource(R.string.btn_already_account))
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(userId: String, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inventory, 1: Notes

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), selectedTextColor = Color(0xFF2E7D32))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                    label = { Text("Notes") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32), selectedTextColor = Color(0xFF2E7D32))
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                InventoryScreen(userId, onLogout)
            } else {
                KitchenNotesScreen(userId)
            }
        }
    }
}

@Composable
fun InventoryScreen(userId: String, onLogout: () -> Unit) {
    var items by remember { mutableStateOf<Map<String, KitchenItem>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Pair<String, KitchenItem>?>(null) }
    val database = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("inventory")

    LaunchedEffect(userId) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newItems = mutableMapOf<String, KitchenItem>()
                snapshot.children.forEach { child ->
                    child.getValue(KitchenItem::class.java)?.let { newItems[child.key!!] = it }
                }
                items = newItems
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF2E7D32)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    @Suppress("ControlFlowWithEmptyBody")
                    Text("Kitcheneering 🍳", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your kitchen...", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF2E7D32), contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        val filteredItems = items.filter { it.key.contains(searchQuery, ignoreCase = true) }

        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F8E9))) {
            KitchenBackground()
            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧊", fontSize = 64.sp)
                        Text("Your kitchen is empty!", fontSize = 18.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredItems.toList()) { (key, item) ->
                        KitchenItemCard(key, item, onEdit = { editingItem = key to item }, onDelete = { database.child(key).removeValue() })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(onDismiss = { showAddDialog = false }, onItemAdded = { key, item ->
            // Use push() to generate a unique ID for custom items, or use key for predefined
            // Wait, the original code used key as child name. If it's the same key, it overwrites.
            // To fix "items not appearing" (if they are being overwritten), we could use push() for all, 
            // but the original logic seems to intend one entry per item type.
            // Let's stick to original but ensure it's working.
            database.child(key).setValue(item)
            showAddDialog = false
        })
    }

    editingItem?.let { (key, item) ->
        EditItemDialog(itemKey = key, item = item, onDismiss = { editingItem = null }, onItemUpdated = { updatedItem ->
            if (updatedItem != null) database.child(key).setValue(updatedItem)
            editingItem = null
        })
    }
}

@Composable
fun KitchenNotesScreen(userId: String) {
    var notes by remember { mutableStateOf<List<KitchenNote>>(emptyList()) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    val database = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("notes")

    LaunchedEffect(userId) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newNotes = mutableListOf<KitchenNote>()
                snapshot.children.forEach { child ->
                    child.getValue(KitchenNote::class.java)?.let { newNotes.add(it.copy(id = child.key ?: "")) }
                }
                notes = newNotes.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kitchen Notes 📝",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F8E9))) {
            KitchenBackground()
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes yet! 📝", fontSize = 18.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        NoteCard(note, onDelete = { database.child(note.id).removeValue() })
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false; noteText = "" },
            title = { Text("Add New Note", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Enter your note...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            val ref = database.push()
                            val newNote = KitchenNote(id = ref.key ?: "", text = noteText)
                            ref.setValue(newNote)
                            noteText = ""
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false; noteText = "" }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun NoteCard(note: KitchenNote, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.text,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sdf.format(Date(note.timestamp)),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Note",
                    tint = Color.Red.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun KitchenItemCard(key: String, item: KitchenItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val isExpired = item.expiryDate?.let { it < System.currentTimeMillis() } ?: false

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFFFEBEE).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
                val icon = when(item.category) {
                    "fruits" -> "🍎"
                    "vegetables" -> "🥦"
                    "dairy" -> "🥛"
                    "meat" -> "🥩"
                    "chicken" -> "🍗"
                    "bakery" -> "🍞"
                    "frozen" -> "❄️"
                    "cleaners" -> "🧼"
                    "spices" -> "🧂"
                    else -> "📦"
                }
                Text(icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(getLocalizedItemNameWithArabic(context, key), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${item.quantity} ${getLocalizedUnit(context, item.unit)} • ${getLocalizedCategory(context, item.category)}", fontSize = 12.sp, color = Color.Gray)
                item.expiryDate?.let {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Text("Expires: ${sdf.format(Date(it))}", fontSize = 11.sp, color = if (isExpired) Color.Red else Color.Gray)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(onDismiss: () -> Unit, onItemAdded: (String, KitchenItem) -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPredefined by remember { mutableStateOf<PredefinedItem?>(null) }
    var isCustomItemMode by remember { mutableStateOf(false) }

    var customItemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    var category by remember { mutableStateOf("pantry") }
    var expiryDate by remember { mutableStateOf<Long?>(null) }

    if (selectedPredefined != null || isCustomItemMode) {
        val itemName = if (isCustomItemMode) customItemName else getLocalizedItemNameWithArabic(context, selectedPredefined!!.key)
        if (selectedPredefined != null && !isCustomItemMode) {
            unit = selectedPredefined!!.unit
            category = selectedPredefined!!.category
        }

        AlertDialog(
            onDismissRequest = { selectedPredefined = null; isCustomItemMode = false },
            title = { Text("Add $itemName", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                        OutlinedTextField(
                            value = getLocalizedUnit(context, unit),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            listOf("pcs", "kg", "g", "ml", "l", "packet", "jar", "plate", "box", "bottle", "bag", "carton", "dozen", "can", "roll").forEach { u ->
                                DropdownMenuItem(text = { Text(getLocalizedUnit(context, u)) }, onClick = { unit = u; unitExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var catExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                        OutlinedTextField(
                            value = getLocalizedCategory(context, category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            listOf("fruits", "vegetables", "dairy", "meat", "chicken", "bakery", "frozen", "pantry", "cleaners", "spices").forEach { cat ->
                                DropdownMenuItem(text = { Text(getLocalizedCategory(context, cat)) }, onClick = { category = cat; catExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(context, { _, y, m, d ->
                                val newDate = Calendar.getInstance()
                                newDate.set(y, m, d)
                                expiryDate = newDate.timeInMillis
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                    ) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        Text(if (expiryDate == null) "Set Expiry Date 📅" else "Expires: ${sdf.format(Date(expiryDate!!))}", color = Color.Black)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val key = if (isCustomItemMode) customItemName.lowercase().replace(" ", "_") else selectedPredefined!!.key
                    onItemAdded(key, KitchenItem(quantityStr.toDoubleOrNull() ?: 1.0, unit, category, expiryDate))
                    isCustomItemMode = false
                    selectedPredefined = null
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { selectedPredefined = null; isCustomItemMode = false }) { Text("Back") }
            }
        )
    } else {
        val configuration = remember(context) { Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ar")) } }
        val arabicContext = remember(context, configuration) { context.createConfigurationContext(configuration) }

        val filteredPredefined = commonKitchenItems.filter { predefined ->
            val englishMatch = predefined.key.contains(searchQuery, ignoreCase = true) ||
                    (predefined.nameRes != -1 && context.getString(predefined.nameRes).contains(searchQuery, ignoreCase = true))
            val arabicMatch = if (predefined.nameRes != -1) arabicContext.getString(predefined.nameRes).contains(searchQuery, ignoreCase = true) else false
            englishMatch || arabicMatch
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Item", fontSize = 15.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search or type new item") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (searchQuery.isNotBlank() && filteredPredefined.none { getLocalizedItemNameWithArabic(context, it.key).equals(searchQuery, true) }) {
                        Spacer(modifier = Modifier.height(8.dp))
                        @Suppress("ControlFlowWithEmptyBody")
                        Button(
                            onClick = { customItemName = searchQuery; isCustomItemMode = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                        ) { Text("Add \"$searchQuery\" as new item") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredPredefined) { predefined ->
                            ListItem(
                                headlineContent = { Text(getLocalizedItemNameWithArabic(context, predefined.key), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                supportingContent = { Text("${getLocalizedCategory(context, predefined.category)} • ${predefined.unit}", fontSize = 12.sp) },
                                leadingContent = {
                                    val icon = when(predefined.category) {
                                        "fruits" -> "🍎"
                                        "vegetables" -> "🥦"
                                        "dairy" -> "🥛"
                                        "meat" -> "🥩"
                                        "chicken" -> "🍗"
                                        "bakery" -> "🍞"
                                        "frozen" -> "❄️"
                                        "cleaners" -> "🧼"
                                        "spices" -> "🧂"
                                        else -> "📦"
                                    }
                                    Text(icon, fontSize = 24.sp)
                                },
                                modifier = Modifier.clickable { selectedPredefined = predefined }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    itemKey: String,
    item: KitchenItem,
    onDismiss: () -> Unit,
    onItemUpdated: (KitchenItem?) -> Unit
) {
    val context = LocalContext.current
    var quantityStr by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var category by remember { mutableStateOf(item.category) }
    var expiryDate by remember { mutableStateOf(item.expiryDate) }

    var unitExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val unitsList = listOf("pcs", "kg", "g", "ml", "l", "packet", "jar", "plate", "box", "bottle", "bag", "carton", "dozen", "can", "roll")
    val categoriesList = listOf("fruits", "vegetables", "dairy", "meat", "chicken", "bakery", "frozen", "pantry", "cleaners", "spices")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                @Suppress("DEPRECATION")
                Text("Edit ${getLocalizedItemNameWithArabic(context, itemKey)}", fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        val current = quantityStr.toDoubleOrNull() ?: 0.0
                        quantityStr = (current - 1.0).coerceAtLeast(0.0).toString()
                    },
                    modifier = Modifier.size(32.dp).background(Color(0xFFB71C1C), RoundedCornerShape(8.dp))
                ) { Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val current = quantityStr.toDoubleOrNull() ?: 0.0
                        quantityStr = (current + 1.0).toString()
                    },
                    modifier = Modifier.size(32.dp).background(Color(0xFF003300), RoundedCornerShape(8.dp))
                ) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = getLocalizedUnit(context, unit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        unitsList.forEach { u ->
                            DropdownMenuItem(text = { Text(getLocalizedUnit(context, u)) }, onClick = { unit = u; unitExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = getLocalizedCategory(context, category),
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(text = { Text(getLocalizedCategory(context, cat)) }, onClick = { category = cat; categoryExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        expiryDate?.let { calendar.timeInMillis = it }
                        DatePickerDialog(context, { _, y, m, d ->
                            val newDate = Calendar.getInstance()
                            newDate.set(y, m, d)
                            expiryDate = newDate.timeInMillis
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                ) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    @Suppress("ControlFlowWithEmptyBody")
                    Text(if (expiryDate == null) "Set Expiry Date 📅" else "Expires: ${sdf.format(Date(expiryDate!!))}", color = Color.Black)
                }
            }
        },
        confirmButton = { Button(onClick = { onItemUpdated(item.copy(quantity = quantityStr.toDoubleOrNull() ?: 1.0, unit = unit, category = category, expiryDate = expiryDate)) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

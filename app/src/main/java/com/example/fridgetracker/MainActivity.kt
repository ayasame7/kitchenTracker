package com.example.fridgetracker

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.fridgetracker.ui.theme.FridgeTrackerTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Attractive Light Colorful Background Gradient: Light Red, Light Green, Light Blue, Light Pink
val EntryGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFCDD2), // Light Red
        Color(0xFFC8E6C9), // Light Green
        Color(0xFFFFF9C4), // Light Yellow
        Color(0xFFB3E5FC), // Light Blue
        Color(0xFFFFD1DC)  // Light Pink
    )
)

@Composable
fun BackgroundDecoration() {
    val items = listOf("🍎", "🥛", "🍞", "🥦", "🥩", "🧀", "🥕", "🍗", "🍯", "🍇", "🥚", "🥫")
    Box(modifier = Modifier.fillMaxSize()) {
        items.forEachIndexed { index, emoji ->
            val horizontalBias = ((index % 3) - 1) * 0.8f
            val verticalBias = ((index / 3) - 1.5f) * 0.6f
            
            Text(
                text = emoji,
                fontSize = 90.sp,
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias, verticalBias))
                    .graphicsLayer(
                        alpha = 0.15f,
                        rotationZ = (index * 35f) % 360f
                    )
            )
        }
    }
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EntryGradient)
    ) {
        BackgroundDecoration()
        content()
    }
}

// Helper to change language
@Suppress("DEPRECATION")
fun updateLocale(context: Context, languageCode: String) {
    val locale = Locale.forLanguageTag(languageCode)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    
    // Save preference
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    prefs.edit { putString("lang", languageCode) }
}

// Data model for Kitchen Items
@Keep
data class KitchenItem(
    val quantity: Double = 0.0, 
    val expiryDate: Long? = null, 
    val category: String = "pantry",
    val unit: String = "pcs"
)

@Keep
data class KitchenNote(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = 0L
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
    PredefinedItem("disinfectant_wipes", "packet", "cleaners", R.string.item_disinfectant_wipes)
)

fun getEnglishItemName(key: String): String {
    return key.replace("_", " ").replaceFirstChar { it.uppercase() }
}

fun getLocalizedItemName(context: Context, key: String): String {
    val predefined = commonKitchenItems.find { it.key == key }
    return if (predefined != null && predefined.nameRes != -1) context.getString(predefined.nameRes) else getEnglishItemName(key)
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

fun getLocalizedCategory(context: Context, key: String): String {
    return when (key.lowercase()) {
        "fruits" -> context.getString(R.string.cat_fruits)
        "vegetables" -> context.getString(R.string.cat_vegetables)
        "dairy" -> context.getString(R.string.cat_dairy)
        "meat" -> context.getString(R.string.cat_meat)
        "chicken" -> context.getString(R.string.item_chicken)
        "bakery" -> context.getString(R.string.cat_bakery)
        "frozen" -> context.getString(R.string.cat_frozen)
        "pantry" -> context.getString(R.string.cat_pantry)
        "cleaners" -> context.getString(R.string.cat_cleaners)
        "spices" -> context.getString(R.string.cat_spices)
        else -> key.replaceFirstChar { it.uppercase() }
    }
}

fun getLocalizedCategoryWithArabic(context: Context, key: String): String {
    val englishName = if (key.lowercase() == "all") "All" else key.replaceFirstChar { it.uppercase() }
    val resId = when (key.lowercase()) {
        "fruits" -> R.string.cat_fruits
        "vegetables" -> R.string.cat_vegetables
        "dairy" -> R.string.cat_dairy
        "meat" -> R.string.cat_meat
        "chicken" -> R.string.item_chicken
        "bakery" -> R.string.cat_bakery
        "frozen" -> R.string.cat_frozen
        "pantry" -> R.string.cat_pantry
        "cleaners" -> R.string.cat_cleaners
        "spices" -> R.string.cat_spices
        else -> -1
    }
    
    val arabicName = if (key.lowercase() == "all") {
        "الكل"
    } else if (resId != -1) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale("ar"))
        val arabicContext = context.createConfigurationContext(configuration)
        arabicContext.getString(resId)
    } else {
        null
    }
    
    return if (arabicName != null) "$englishName ($arabicName)" else englishName
}

fun getLocalizedUnit(context: Context, key: String): String {
    return when (key.lowercase()) {
        "pcs" -> context.getString(R.string.unit_pcs)
        "kg" -> context.getString(R.string.unit_kg)
        "g" -> context.getString(R.string.unit_g)
        "ml" -> context.getString(R.string.unit_ml)
        "l" -> context.getString(R.string.unit_l)
        "packet" -> context.getString(R.string.unit_packet)
        "jar" -> context.getString(R.string.unit_jar)
        "plate" -> context.getString(R.string.unit_plate)
        "box" -> context.getString(R.string.unit_box)
        "bottle" -> context.getString(R.string.unit_bottle)
        "bag" -> context.getString(R.string.unit_bag)
        "carton" -> context.getString(R.string.unit_carton)
        "dozen" -> context.getString(R.string.unit_dozen)
        "can" -> context.getString(R.string.unit_can)
        else -> key
    }
}

fun isLowStock(key: String, item: KitchenItem): Boolean {
    val keyLower = key.lowercase()
    val isPowderOrSpecial = keyLower == "tea" || keyLower == "coffee" || keyLower == "nescafe" || 
                            keyLower == "salt" || keyLower.contains("powder") || 
                            keyLower == "paprika" || keyLower == "turmeric" || keyLower == "cinnamon" ||
                            keyLower == "sumac" || keyLower == "thyme" || keyLower == "nutmeg" ||
                            keyLower == "cardamom" || keyLower == "seven_spices" || keyLower.contains("spices")
    
    return when (item.unit.lowercase()) {
        "g", "ml" -> if (isPowderOrSpecial) item.quantity < 260.0 else item.quantity < 400.0
        "kg" -> if (isPowderOrSpecial) item.quantity < 0.26 else item.quantity < 0.4
        "l" -> item.quantity < 0.6
        else -> item.quantity < 5.0
    }
}

fun scheduleReminders(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    val scheduleTime = { hour: Int, minute: Int, id: Int ->
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            id, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // Schedule 10:00 AM and 8:00 PM
    scheduleTime(10, 0, 1001)
    scheduleTime(20, 0, 1002)
}

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleReminders(this)
        }
    }

    // Global selected category state
    private var globalSelectedCategory = mutableStateOf("All")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force English language
        updateLocale(this, "en")
        
        auth = Firebase.auth

        // Request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scheduleReminders(this)
            }
        } else {
            scheduleReminders(this)
        }
        
        setContent {
            FridgeTrackerTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("intro") }

                    var showExitDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("Exit") },
                            text = { Text("Do you want to exit?") },
                            confirmButton = {
                                TextButton(onClick = { (context as? Activity)?.finish() }) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text("No")
                                }
                            }
                        )
                    }

                    BackHandler {
                        when (currentScreen) {
                            "main" -> showExitDialog = true
                            "welcome" -> currentScreen = "hello"
                            "hello" -> currentScreen = "intro"
                            "intro" -> (context as? Activity)?.finish()
                            else -> currentScreen = "main"
                        }
                    }

                    when (currentScreen) {
                        "intro" -> IntroPage(onContinue = { currentScreen = "hello" })
                        "hello" -> HelloPage(onContinue = {
                            currentScreen = if (auth.currentUser != null) "main" else "welcome"
                        })
                        "welcome" -> AuthPage(onAuthSuccess = { currentScreen = "main" })
                        "main" -> MainScreen(
                            initialCategory = globalSelectedCategory.value,
                            onCategoryChanged = { globalSelectedCategory.value = it },
                            onLogout = {
                                auth.signOut()
                                currentScreen = "welcome"
                            },
                            onGoToShopping = { currentScreen = "shopping" },
                            onGoToHelp = { currentScreen = "help" },
                            onGoToNotes = { currentScreen = "notes" }
                        )
                        "shopping" -> ShoppingPage(onBack = { currentScreen = "main" })
                        "help" -> HelpPage(onBack = { currentScreen = "main" })
                        "notes" -> NotesPage(onBack = { currentScreen = "main" })
                    }
                }
            }
        }
    }
}

@Composable
fun IntroPage(onContinue: () -> Unit) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Why Kitchen Tracker!?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Smart Kitchen Tracker helps you manage your kitchen easily",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val bulletPoints = listOf(
                        "Tracks what's available",
                        "Know what's missing",
                        "Save time and reduce food waste"
                    )
                    
                    bulletPoints.forEach { point ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = point, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1B5E20))
            ) {
                Text("Get Started ✨", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HelloPage(onContinue: () -> Unit) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hello! 👋\nYour kitchen called… it's tired of forgotten food and hidden ingredients! 🍅🥫\nLet’s check what’s actually in there.",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1B5E20))
            ) {
                Text("Open the Magic Box! 🕵️‍♂️🥛", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialCategory: String,
    onCategoryChanged: (String) -> Unit,
    onLogout: () -> Unit,
    onGoToShopping: () -> Unit,
    onGoToHelp: () -> Unit,
    onGoToNotes: () -> Unit
) {
    val context = LocalContext.current
    val database = Firebase.database.reference
    val user = Firebase.auth.currentUser
    
    var fridgeItems by remember { mutableStateOf<Map<String, KitchenItem>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var selectedItemToEdit by remember { mutableStateOf<Pair<String, KitchenItem>?>(null) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            database.child("users").child(user.uid).child("fridge")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val items = mutableMapOf<String, KitchenItem>()
                        snapshot.children.forEach {
                            val item = it.getValue(KitchenItem::class.java)
                            if (item != null) items[it.key!!] = item
                        }
                        fridgeItems = items
                        isLoading = false
                    }
                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                    }
                })
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.title_kitchen_tracker), 
                        fontWeight = FontWeight.Bold, 
                        color = Color.Black,
                        fontSize = 18.sp
                    ) 
                },
                actions = {
                    IconButton(onClick = onGoToShopping) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping", tint = Color.Black)
                    }
                    IconButton(onClick = onGoToNotes) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Notes", tint = Color.Black)
                    }
                    IconButton(onClick = onGoToHelp) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = Color.Black)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = Color(0xFF0D47A1), // Dark Blue
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        AppBackground {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Search and Category
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_placeholder), color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    var showCategoryMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(
                                Icons.Default.FilterList, 
                                contentDescription = "Filter",
                                tint = Color.Black
                            )
                        }
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            val categories = listOf("All", "fruits", "vegetables", "dairy", "meat", "chicken", "bakery", "frozen", "pantry", "cleaners", "spices")
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(getLocalizedCategoryWithArabic(context, cat)) },
                                    onClick = {
                                        selectedCategory = cat
                                        onCategoryChanged(cat)
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                } else {
                    val filteredItems = fridgeItems.filter { (key, item) ->
                        val name = getLocalizedItemNameWithArabic(context, key)
                        (searchQuery.isEmpty() || name.contains(searchQuery, ignoreCase = true)) &&
                        (selectedCategory.equals("All", ignoreCase = true) || item.category.equals(selectedCategory, ignoreCase = true))
                    }.toList().sortedBy { it.first }

                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (searchQuery.isEmpty()) "Your kitchen is empty! 🏜️" else "No matches found! 🕵️",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (searchQuery.isEmpty()) "Tap the + button to add items!" else "Try a different search.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredItems) { (key, item) ->
                                val isExpiringSoon = item.expiryDate?.let {
                                    val daysLeft = (it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                                    daysLeft in 0..3
                                } ?: false
                                
                                val isExpired = item.expiryDate?.let { it < System.currentTimeMillis() } ?: false
                                val lowStock = isLowStock(key, item)

                                KitchenItemCard(
                                    name = getLocalizedItemNameWithArabic(context, key),
                                    item = item,
                                    isExpiringSoon = isExpiringSoon,
                                    isExpired = isExpired,
                                    isLowStock = lowStock,
                                    onClick = {
                                        selectedItemToEdit = key to item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onItemAdded = { key, item ->
                if (user != null) {
                    database.child("users").child(user.uid).child("fridge").child(key).setValue(item)
                }
                showAddItemDialog = false
            }
        )
    }

    selectedItemToEdit?.let { (key, item) ->
        EditItemDialog(
            itemKey = key,
            item = item,
            onDismiss = { selectedItemToEdit = null },
            onItemUpdated = { updatedItem ->
                if (user != null) {
                    if (updatedItem == null) {
                        database.child("users").child(user.uid).child("fridge").child(key).removeValue()
                    } else {
                        database.child("users").child(user.uid).child("fridge").child(key).setValue(updatedItem)
                    }
                }
                selectedItemToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onItemAdded: (String, KitchenItem) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPredefined by remember { mutableStateOf<PredefinedItem?>(null) }
    
    val unitsList = listOf("pcs", "kg", "g", "ml", "l", "packet", "jar", "plate", "box", "bottle", "bag", "carton", "dozen", "can")
    val categoriesList = listOf("fruits", "vegetables", "dairy", "meat", "chicken", "bakery", "frozen", "pantry", "cleaners", "spices")

    if (selectedPredefined != null) {
        var quantityStr by remember { mutableStateOf("1.0") }
        var unit by remember { mutableStateOf(selectedPredefined!!.unit) }
        var category by remember { mutableStateOf(selectedPredefined!!.category) }
        var expiryDate by remember { mutableStateOf<Long?>(null) }
        
        var unitExpanded by remember { mutableStateOf(false) }
        var categoryExpanded by remember { mutableStateOf(false) }

        val itemName = if (selectedPredefined!!.nameRes != -1) {
            context.getString(selectedPredefined!!.nameRes)
        } else {
            selectedPredefined!!.key.replace("_", " ").replaceFirstChar { it.uppercase() }
        }

        AlertDialog(
            onDismissRequest = { selectedPredefined = null },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Configure $itemName", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = {
                            val current = quantityStr.toDoubleOrNull() ?: 0.0
                            quantityStr = (current - 1.0).coerceAtLeast(0.0).toString()
                        },
                        modifier = Modifier.size(32.dp).background(Color(0xFFB71C1C), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            val current = quantityStr.toDoubleOrNull() ?: 0.0
                            quantityStr = (current + 1.0).toString()
                        },
                        modifier = Modifier.size(32.dp).background(Color(0xFF003300), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
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
                    
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = getLocalizedUnit(context, unit),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            unitsList.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(getLocalizedUnit(context, u)) },
                                    onClick = { unit = u; unitExpanded = false }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = getLocalizedCategory(context, category),
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(getLocalizedCategory(context, cat)) },
                                    onClick = { category = cat; categoryExpanded = false }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            expiryDate?.let { calendar.timeInMillis = it }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newDate = Calendar.getInstance()
                                    newDate.set(year, month, dayOfMonth)
                                    expiryDate = newDate.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                    ) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        Text(
                            text = if (expiryDate == null) "Set Expiry Date 📅" else "Expires: ${sdf.format(Date(expiryDate!!))}",
                            color = Color.Black
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onItemAdded(
                        selectedPredefined!!.key,
                        KitchenItem(
                            quantity = quantityStr.toDoubleOrNull() ?: 1.0,
                            unit = unit,
                            category = category,
                            expiryDate = expiryDate
                        )
                    )
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { selectedPredefined = null }) { Text("Back") }
            }
        )
    } else {
        val configuration = remember(context) {
            Configuration(context.resources.configuration).apply {
                setLocale(Locale("ar"))
            }
        }
        val arabicContext = remember(context, configuration) {
            context.createConfigurationContext(configuration)
        }

        val filteredPredefined = commonKitchenItems.filter { predefined ->
            val englishMatch = predefined.key.contains(searchQuery, ignoreCase = true) ||
                    (predefined.nameRes != -1 && context.getString(predefined.nameRes).contains(searchQuery, ignoreCase = true))
            
            val arabicMatch = if (predefined.nameRes != -1) {
                arabicContext.getString(predefined.nameRes).contains(searchQuery, ignoreCase = true)
            } else false
            
            englishMatch || arabicMatch
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Item", fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredPredefined) { predefined ->
                            ListItem(
                                headlineContent = { Text(getLocalizedItemNameWithArabic(context, predefined.key), fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${getLocalizedCategory(context, predefined.category)} • ${predefined.unit}") },
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
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
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

    val unitsList = listOf("pcs", "kg", "g", "ml", "l", "packet", "jar", "plate", "box", "bottle", "bag", "carton", "dozen", "can")
    val categoriesList = listOf("fruits", "vegetables", "dairy", "meat", "chicken", "bakery", "frozen", "pantry", "cleaners", "spices")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Edit ${getLocalizedItemNameWithArabic(context, itemKey)}", fontSize = 16.sp, modifier = Modifier.weight(1f))
                
                IconButton(
                    onClick = {
                        val current = quantityStr.toDoubleOrNull() ?: 0.0
                        quantityStr = (current - 1.0).coerceAtLeast(0.0).toString()
                    },
                    modifier = Modifier.size(32.dp).background(Color(0xFFB71C1C), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        val current = quantityStr.toDoubleOrNull() ?: 0.0
                        quantityStr = (current + 1.0).toString()
                    },
                    modifier = Modifier.size(32.dp).background(Color(0xFF003300), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                }
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
                
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = getLocalizedUnit(context, unit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                        ) {
                        unitsList.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(getLocalizedUnit(context, u)) },
                                onClick = { unit = u; unitExpanded = false }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = getLocalizedCategory(context, category),
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(getLocalizedCategory(context, cat)) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        expiryDate?.let { calendar.timeInMillis = it }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newDate = Calendar.getInstance()
                                newDate.set(year, month, dayOfMonth)
                                expiryDate = newDate.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                ) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Text(
                        text = if (expiryDate == null) "Set Expiry Date 📅" else "Expires: ${sdf.format(Date(expiryDate!!))}",
                        color = Color.Black
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantityStr.toDoubleOrNull() ?: item.quantity
                onItemUpdated(item.copy(quantity = q, unit = unit, category = category, expiryDate = expiryDate))
            }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = { onItemUpdated(null) }) { 
                Text("Delete", color = Color.Red) 
            }
        }
    )
}

@Composable
fun KitchenItemCard(
    name: String,
    item: KitchenItem,
    isExpiringSoon: Boolean,
    isExpired: Boolean,
    isLowStock: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val borderColor = when {
        isExpired -> Color.Red
        isExpiringSoon -> Color(0xFFFFA000)
        isLowStock -> Color.Black.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val quantityText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                    Text(
                        text = "$quantityText ${getLocalizedUnit(context, item.unit)} • ${getLocalizedCategory(context, item.category)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    
                    if (isLowStock) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = Color.DarkGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (item.expiryDate != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dateStr = sdf.format(Date(item.expiryDate))
                    Text(
                        text = "Exp: $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isExpired -> Color.Red
                            isExpiringSoon -> Color(0xFFFFA000)
                            else -> Color.Black.copy(alpha = 0.6f)
                        },
                        fontSize = 10.sp
                    )
                }
            }
            
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

// Colored Background Wrapper
@Composable
fun AuthBackground(authContent: @Composable ColumnScope.() -> Unit) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = authContent
        )
    }
}

@Composable
fun AuthPage(onAuthSuccess: () -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val auth = Firebase.auth
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        email = prefs.getString("saved_email", "") ?: ""
        password = prefs.getString("saved_password", "") ?: ""
    }

    AuthBackground {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineSmall,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isSignUp) "Organize your kitchen effortlessly." else "Log in to track your supplies.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.5f)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.5f)
                    )
                )
                
                if (isSignUp) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isSignUp && password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        if (isSignUp) {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit {
                                            putString("saved_email", email)
                                            putString("saved_password", password)
                                        }
                                        onAuthSuccess()
                                    } else {
                                        Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit {
                                            putString("saved_email", email)
                                            putString("saved_password", password)
                                        }
                                        onAuthSuccess()
                                    } else {
                                        Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1B5E20)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF1B5E20))
                    } else {
                        Text(if (isSignUp) "Sign Up" else "Log In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(
                if (isSignUp) "Already have an account? Log In." else "Don't have an account? Sign Up!",
                color = Color.Black,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = Firebase.database.reference
    val user = Firebase.auth.currentUser
    
    var fridgeItems by remember { mutableStateOf<Map<String, KitchenItem>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            database.child("users").child(user.uid).child("fridge")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val items = mutableMapOf<String, KitchenItem>()
                        snapshot.children.forEach {
                            val item = it.getValue(KitchenItem::class.java)
                            if (item != null) items[it.key!!] = item
                        }
                        fridgeItems = items
                        isLoading = false
                    }
                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                    }
                })
        }
    }

    val lowStockItems = fridgeItems.filter { (key, item) -> isLowStock(key, item) }.toList().sortedBy { it.first }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Shopping List 🛒", color = Color.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        AppBackground {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                } else if (lowStockItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Your fridge is full! 🏰", color = Color.Black, fontSize = 16.sp)
                            Text("Nothing is running low.", color = Color.Black.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(lowStockItems) { (key, item) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(getLocalizedItemNameWithArabic(context, key), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                        Text(
                                            "Current: ${item.quantity} ${getLocalizedUnit(context, item.unit)}", 
                                            fontSize = 11.sp, 
                                            color = Color.Black.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(Icons.Default.PriorityHigh, contentDescription = "Low Stock", tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesPage(onBack: () -> Unit) {
    val database = Firebase.database.reference
    val user = Firebase.auth.currentUser
    
    var notes by remember { mutableStateOf<List<KitchenNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            database.child("users").child(user.uid).child("notes")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<KitchenNote>()
                        snapshot.children.forEach {
                            it.getValue(KitchenNote::class.java)?.let { note ->
                                list.add(note.copy(id = it.key!!))
                            }
                        }
                        notes = list.sortedByDescending { it.timestamp }
                        isLoading = false
                    }
                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                    }
                })
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Notes 📝", color = Color.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                containerColor = Color(0xFF0D47A1),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        AppBackground {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                } else if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes yet! 🖋️", color = Color.Black, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(notes) { note ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(note.text, color = Color.Black, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        Text(
                                            sdf.format(Date(note.timestamp)),
                                            fontSize = 10.sp,
                                            color = Color.Black.copy(alpha = 0.5f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (user != null) {
                                                    database.child("users").child(user.uid).child("notes").child(note.id).removeValue()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Type your note here...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isNotBlank() && user != null) {
                            val newNoteRef = database.child("users").child(user.uid).child("notes").push()
                            newNoteRef.setValue(KitchenNote(text = noteText, timestamp = System.currentTimeMillis()))
                        }
                        showAddNoteDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpPage(onBack: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Help & Tips 💡", color = Color.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        AppBackground {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HelpCard(
                        title = "Adding Items ➕",
                        description = "1. Tap the blue '+' button at the bottom right.\n" +
                                    "2. Search for the item you want to add.\n" +
                                    "3. Configure its quantity, unit, category, and expiry date.\n" +
                                    "4. Tap 'Add' to save it to your kitchen."
                    )
                }
                item {
                    HelpCard(
                        title = "Updating Items ✏️",
                        description = "1. Find the item in your kitchen list.\n" +
                                    "2. Tap on the item card.\n" +
                                    "3. Use the green '+' or red '-' buttons for quick quantity adjustments, or type in the quantity field.\n" +
                                    "4. Tap 'Update' to save your changes."
                    )
                }
                item {
                    HelpCard(
                        title = "Removing Items 🗑️",
                        description = "1. Tap on the item you wish to remove.\n" +
                                    "2. In the edit dialog, tap the red 'Delete' button at the bottom left.\n" +
                                    "3. The item will be permanently removed from your list."
                    )
                }
                item {
                    HelpCard(
                        title = "Kitchen Notes 📝",
                        description = "1. Access it via the new Note icon 📝 in the top bar of the Main screen.\n" +
                                    "2. Add text notes with an 'Add Note' button.\n" +
                                    "3. See the date and time each note was created.\n" +
                                    "4. Delete notes you no longer need."
                    )
                }
                item {
                    HelpCard(
                        title = "Shopping List & Low Stock 🛒",
                        description = "Items that are running low (usually less than 5 units, 0.6 liters, 400g, or 260g for powders/tea/coffee) will automatically appear in your Shopping List. Tap the cart icon at the top to view them."
                    )
                }
                item {
                    HelpCard(
                        title = "Searching & Filtering 🔍",
                        description = "Use the search bar at the top to find specific items by name. Use the filter icon (next to search) to view items from a specific category like Fruits, Dairy, or Frozen."
                    )
                }
                item {
                    HelpCard(
                        title = "Expiry Alerts ⚠️",
                        description = "Items expiring within 3 days will have an orange border. Expired items will have a red border to help you track freshness."
                    )
                }
            }
        }
    }
}

@Composable
fun HelpCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, color = Color.Black.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

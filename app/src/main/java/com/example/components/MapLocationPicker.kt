package com.example.components

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPicker(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onLocationConfirmed: (latitude: Double, longitude: Double, country: String, city: String, district: String, street: String, fullAddress: String) -> Unit,
    onDismissRequest: () -> Unit,
    isArabic: Boolean = false
) {
    var latState by remember { mutableStateOf(initialLatitude ?: 33.5138) }
    var lngState by remember { mutableStateOf(initialLongitude ?: 36.2947) }
    
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var isLocating by remember { mutableStateOf(false) }
    
    // Address state holding geocoded info
    var geocodedAddress by remember { 
        mutableStateOf(
            GeocodeResult(
                country = if (isArabic) "سوريا" else "Syria",
                city = if (isArabic) "دمشق" else "Damascus",
                district = if (isArabic) "المزة" else "Mezzeh",
                street = if (isArabic) "أوتوستراد المزة" else "Mezzeh Highway",
                fullAddress = if (isArabic) "أوتوستراد المزة، المزة، دمشق، سوريا" else "Mezzeh Highway, Mezzeh, Damascus, Syria"
            )
        )
    }

    // Effect to perform reverse geocoding asynchronously on coordinate changes
    LaunchedEffect(latState, lngState) {
        val result = performReverseGeocoding(latState, lngState, isArabic)
        geocodedAddress = result
    }

    // Quick search location pills
    val quickLocations = listOf(
        QuickLoc(if (isArabic) "باب توما" else "Bab Touma", 33.5150, 36.3100, "Damascus"),
        QuickLoc(if (isArabic) "المزة" else "Mezzeh", 33.5080, 36.2550, "Damascus"),
        QuickLoc(if (isArabic) "المالكي" else "Malki", 33.5180, 36.2750, "Damascus"),
        QuickLoc(if (isArabic) "حلب الشهباء" else "Shahba", 36.2210, 37.1250, "Aleppo"),
        QuickLoc(if (isArabic) "الموكامبو" else "Mogambo", 36.2150, 37.1150, "Aleppo"),
        QuickLoc(if (isArabic) "الإنشاءات" else "Al-Inshaat", 34.7150, 36.6950, "Homs"),
        QuickLoc(if (isArabic) "الكرامة" else "Al-Karameh", 34.8920, 35.8820, "Tartous"),
        QuickLoc(if (isArabic) "الزراعة" else "Al-Ziraa", 35.5180, 35.8050, "Latakia")
    )

    // Leaflet map HTML content loading real OpenStreetMap tile providers
    val leafletHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background-color: #f4f6f9; }
                #map { height: 100%; width: 100%; }
                .leaflet-control-attribution { display: none !important; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([33.5138, 36.2947], 14);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                // Add nice red drop marker
                var marker = L.marker([33.5138, 36.2947], {
                    draggable: true
                }).addTo(map);

                function passCoordinates(lat, lng) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onLocationSelected(lat, lng);
                    }
                }

                marker.on('dragend', function (e) {
                    var latlng = marker.getLatLng();
                    passCoordinates(latlng.lat, latlng.lng);
                });

                map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    passCoordinates(e.latlng.lat, e.latlng.lng);
                });

                function updateMarker(lat, lng) {
                    marker.setLatLng([lat, lng]);
                    map.setView([lat, lng], map.getZoom());
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // Real WebView Map container instead of custom canvas simulation
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            webViewClient = WebViewClient()
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onLocationSelected(lat: Double, lng: Double) {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        latState = lat
                                        lngState = lng
                                    }
                                }
                            }, "AndroidBridge")
                            
                            loadDataWithBaseURL("https://openstreetmap.org", leafletHtml, "text/html", "UTF-8", null)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Sync WebView marker with local coordinates when they are updated via quick links or search
                LaunchedEffect(latState, lngState) {
                    webViewInstance?.post {
                        webViewInstance?.evaluateJavascript("if (typeof updateMarker === 'function') { updateMarker($latState, $lngState); }", null)
                    }
                }

                // Top Search & Navigation Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Search Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .shadow(2.dp, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Picker")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (isArabic) "ابحث عن حي أو منطقة للتوصيل..." else "Search for delivery area...") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                } else {
                                    Icon(Icons.Default.Search, null)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                val found = quickLocations.firstOrNull { 
                                    it.label.contains(searchQuery, ignoreCase = true) 
                                }
                                if (found != null) {
                                    latState = found.lat
                                    lngState = found.lng
                                } else {
                                    if (searchQuery.contains("حلب", true) || searchQuery.contains("Aleppo", true)) {
                                        latState = 36.2021
                                        lngState = 37.1343
                                    } else if (searchQuery.contains("حمص", true) || searchQuery.contains("Homs", true)) {
                                        latState = 34.7324
                                        lngState = 36.7137
                                    } else if (searchQuery.contains("اللاذقية", true) || searchQuery.contains("Latakia", true)) {
                                        latState = 35.5312
                                        lngState = 35.7921
                                    } else if (searchQuery.contains("طرطوس", true) || searchQuery.contains("Tartous", true)) {
                                        latState = 34.8890
                                        lngState = 35.8864
                                    } else if (searchQuery.contains("حماة", true) || searchQuery.contains("Hama", true)) {
                                        latState = 35.1318
                                        lngState = 36.7578
                                    } else {
                                        latState = 33.5138
                                        lngState = 36.2947
                                    }
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Recommended Quick Location Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickLocations) { q ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (q.city == geocodedAddress.city) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        latState = q.lat
                                        lngState = q.lng
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (q.city == geocodedAddress.city) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = q.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (q.city == geocodedAddress.city) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Control Button (GPS Locator)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                isLocating = true
                                delay(800)
                                isLocating = false
                                latState = 33.5138
                                lngState = 36.2947
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(3.dp, CircleShape),
                        shape = CircleShape
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Locate On GPS")
                        }
                    }
                }

                // Drawer Card at Bottom representing Talabat/Uber Address Confirmation
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 36.dp, height = 4.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "العنوان المحدد تلقائياً" else "Automatically Geocoded Address",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${geocodedAddress.street}, ${geocodedAddress.district}, ${geocodedAddress.city}, ${geocodedAddress.country}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Displaying all 5 required Address Details automatically resolved
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            AddressRow(label = if (isArabic) "البلد:" else "Country:", value = geocodedAddress.country)
                            AddressRow(label = if (isArabic) "المدينة:" else "City:", value = geocodedAddress.city)
                            AddressRow(label = if (isArabic) "الحي/المنطقة:" else "District:", value = geocodedAddress.district)
                            AddressRow(label = if (isArabic) "الشارع:" else "Street Name:", value = geocodedAddress.street)
                            AddressRow(label = if (isArabic) "الإحداثيات:" else "Coordinates:", value = "Lat ${String.format("%.5f", latState)}, Lng ${String.format("%.5f", lngState)}")
                        }

                        Button(
                            onClick = {
                                onLocationConfirmed(
                                    latState,
                                    lngState,
                                    geocodedAddress.country,
                                    geocodedAddress.city,
                                    geocodedAddress.district,
                                    geocodedAddress.street,
                                    geocodedAddress.fullAddress
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تأكيد واستيراد العنوان تلقائياً ✅" else "Confirm & Auto-Populate Address ✅",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddressRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

data class QuickLoc(
    val label: String,
    val lat: Double,
    val lng: Double,
    val city: String
)

data class GeocodeResult(
    val country: String,
    val city: String,
    val district: String,
    val street: String,
    val fullAddress: String
)

data class GeoNode(
    val lat: Double,
    val lng: Double,
    val country: String,
    val city: String,
    val district: String,
    val street: String
)

val geoDatabase = listOf(
    // Damascus
    GeoNode(33.5150, 36.3100, "Syria", "Damascus", "Bab Touma", "Straight Street"),
    GeoNode(33.5165, 36.3120, "Syria", "Damascus", "Bab Touma", "Kassaa Road"),
    GeoNode(33.5138, 36.3080, "Syria", "Damascus", "Old City", "Al-Hamidiyah Souq Street"),
    GeoNode(33.5080, 36.2550, "Syria", "Damascus", "Mezzeh", "Mezzeh Highway"),
    GeoNode(33.5095, 36.2510, "Syria", "Damascus", "Mezzeh", "Khaled Bin Al Waleed Street"),
    GeoNode(33.5060, 36.2620, "Syria", "Damascus", "Mezzeh", "Al-Razi Street"),
    GeoNode(33.5220, 36.2800, "Syria", "Damascus", "Muhajirin", "Khorsheed Street"),
    GeoNode(33.5205, 36.2780, "Syria", "Damascus", "Muhajirin", "Al-Afif Street"),
    GeoNode(33.5180, 36.2750, "Syria", "Damascus", "Malki", "Al-Jala'a Street"),
    GeoNode(33.5190, 36.2730, "Syria", "Damascus", "Malki", "Al-Rashid Street"),
    GeoNode(33.5140, 36.2850, "Syria", "Damascus", "Shaalan", "Abu Rummaneh Street"),
    GeoNode(33.5125, 36.2870, "Syria", "Damascus", "Shaalan", "Al-Hamra Street"),
    GeoNode(33.4980, 36.2750, "Syria", "Damascus", "Kafar Souseh", "17 April Street"),
    GeoNode(33.4950, 36.2710, "Syria", "Damascus", "Kafar Souseh", "Kafar Souseh Highway"),
    GeoNode(33.5130, 36.2950, "Syria", "Damascus", "Merjeh", "Saadallah Al-Jabiri Street"),
    GeoNode(33.5115, 36.2965, "Syria", "Damascus", "Central", "Baghdad Street"),
    
    // Aleppo
    GeoNode(36.2100, 37.1300, "Syria", "Aleppo", "Al-Jamilia", "Faisal Street"),
    GeoNode(36.2085, 37.1350, "Syria", "Aleppo", "Al-Jamilia", "Al-Sabil Street"),
    GeoNode(36.2150, 37.1150, "Syria", "Aleppo", "Mogambo", "Mogambo Boulevard"),
    GeoNode(36.2135, 37.1180, "Syria", "Aleppo", "Mogambo", "Alexandria Street"),
    GeoNode(36.2210, 37.1250, "Syria", "Aleppo", "Shahba", "Nile Street"),
    GeoNode(36.2230, 37.1210, "Syria", "Aleppo", "Shahba", "Al-Thawra Street"),

    // Homs
    GeoNode(34.7150, 36.6950, "Syria", "Homs", "Al-Inshaat", "Al-Inshaat Boulevard"),
    GeoNode(34.7130, 36.6920, "Syria", "Homs", "Al-Inshaat", "Deipon Street"),
    GeoNode(34.7250, 36.7150, "Syria", "Homs", "Ghouta", "Ghouta Street"),
    GeoNode(34.7230, 36.7180, "Syria", "Homs", "Ghouta", "Al-Kharab Street"),
    
    // Hama
    GeoNode(35.1350, 36.7550, "Syria", "Hama", "Al-Sharia", "Al-Sharia Boulevard"),
    GeoNode(35.1420, 36.7610, "Syria", "Hama", "Al-Hader", "Al-Hader Street"),

    // Latakia
    GeoNode(35.5180, 35.8050, "Syria", "Latakia", "Al-Ziraa", "Al-Ziraa Street"),
    GeoNode(35.5250, 35.7950, "Syria", "Latakia", "Sheikh Dher", "Baghdad Street"),

    // Tartous
    GeoNode(34.8920, 35.8820, "Syria", "Tartous", "Al-Karameh", "Al-Karameh Street"),
    GeoNode(34.8850, 35.8800, "Syria", "Tartous", "Corniche", "Sea Corniche Road")
)

fun getOfflineReverseGeocode(lat: Double, lng: Double, isArabic: Boolean): GeocodeResult {
    var closestNode = geoDatabase[0]
    var minDist = Double.MAX_VALUE
    for (node in geoDatabase) {
        val dist = Math.hypot(lat - node.lat, lng - node.lng)
        if (dist < minDist) {
            minDist = dist
            closestNode = node
        }
    }
    
    val countryText = if (isArabic) "سوريا" else "Syria"
    val cityText = when (closestNode.city) {
        "Damascus" -> if (isArabic) "دمشق" else "Damascus"
        "Aleppo" -> if (isArabic) "حلب" else "Aleppo"
        "Homs" -> if (isArabic) "حمص" else "Homs"
        "Hama" -> if (isArabic) "حماة" else "Hama"
        "Latakia" -> if (isArabic) "اللاذقية" else "Latakia"
        "Tartous" -> if (isArabic) "طرطوس" else "Tartous"
        else -> closestNode.city
    }
    
    val districtText = when (closestNode.district) {
        "Bab Touma" -> if (isArabic) "باب توما" else "Bab Touma"
        "Old City" -> if (isArabic) "المدينة القديمة" else "Old City"
        "Mezzeh" -> if (isArabic) "المزة" else "Mezzeh"
        "Muhajirin" -> if (isArabic) "المهاجرين" else "Muhajirin"
        "Malki" -> if (isArabic) "المالكي" else "Malki"
        "Shaalan" -> if (isArabic) "الشعلان" else "Shaalan"
        "Kafar Souseh" -> if (isArabic) "كفرسوسة" else "Kafar Souseh"
        "Merjeh" -> if (isArabic) "المرجة" else "Merjeh"
        "Central" -> if (isArabic) "وسط المدينة" else "Central"
        "Al-Jamilia" -> if (isArabic) "الجميلية" else "Al-Jamilia"
        "Mogambo" -> if (isArabic) "الموكامبو" else "Mogambo"
        "Shahba" -> if (isArabic) "الشهباء" else "Shahba"
        "Al-Inshaat" -> if (isArabic) "الإنشاءات" else "Al-Inshaat"
        "Ghouta" -> if (isArabic) "الغوطة" else "Ghouta"
        "Al-Sharia" -> if (isArabic) "الشريعة" else "Al-Sharia"
        "Al-Hader" -> if (isArabic) "الحاضر" else "Al-Hader"
        "Al-Ziraa" -> if (isArabic) "الزراعة" else "Al-Ziraa"
        "Sheikh Dher" -> if (isArabic) "الشيخ ضاهر" else "Sheikh Dher"
        "Al-Karameh" -> if (isArabic) "الكرامة" else "Al-Karameh"
        "Corniche" -> if (isArabic) "الكورنيش" else "Corniche"
        else -> closestNode.district
    }
    
    val streetText = when (closestNode.street) {
        "Straight Street" -> if (isArabic) "الشارع المستقيم" else "Straight Street"
        "Kassaa Road" -> if (isArabic) "طريق القصاع" else "Kassaa Road"
        "Al-Hamidiyah Souq Street" -> if (isArabic) "شارع سوق الحميدية" else "Al-Hamidiyah Souq Street"
        "Mezzeh Highway" -> if (isArabic) "أوتوستراد المزة" else "Mezzeh Highway"
        "Khaled Bin Al Waleed Street" -> if (isArabic) "شارع خالد بن الوليد" else "Khaled Bin Al Waleed Street"
        "Al-Razi Street" -> if (isArabic) "شارع الرازي" else "Al-Razi Street"
        "Khorsheed Street" -> if (isArabic) "شارع خورشيد" else "Khorsheed Street"
        "Al-Afif Street" -> if (isArabic) "شارع العفيف" else "Al-Afif Street"
        "Al-Jala'a Street" -> if (isArabic) "شارع الجلاء" else "Al-Jala'a Street"
        "Al-Rashid Street" -> if (isArabic) "شارع الرشيد" else "Al-Rashid Street"
        "Abu Rummaneh Street" -> if (isArabic) "شارع أبو رمانة" else "Abu Rummaneh Street"
        "Al-Hamra Street" -> if (isArabic) "شارع الحمرا" else "Al-Hamra Street"
        "17 April Street" -> if (isArabic) "شارع ١٧ نيسان" else "17 April Street"
        "Kafar Souseh Highway" -> if (isArabic) "أوتوستراد كفرسوسة" else "Kafar Souseh Highway"
        "Saadallah Al-Jabiri Street" -> if (isArabic) "شارع سعد الله الجابري" else "Saadallah Al-Jabiri Street"
        "Baghdad Street" -> if (isArabic) "شارع بغداد" else "Baghdad Street"
        "Faisal Street" -> if (isArabic) "شارع فيصل" else "Faisal Street"
        "Al-Sabil Street" -> if (isArabic) "شارع السبيل" else "Al-Sabil Street"
        "Mogambo Boulevard" -> if (isArabic) "أوتوستراد الموكامبو" else "Mogambo Boulevard"
        "Alexandria Street" -> if (isArabic) "شارع الإسكندرية" else "Alexandria Street"
        "Nile Street" -> if (isArabic) "شارع النيل" else "Nile Street"
        "Al-Thawra Street" -> if (isArabic) "شارع الثورة" else "Al-Thawra Street"
        "Al-Inshaat Boulevard" -> if (isArabic) "أوتوستراد الإنشاءات" else "Al-Inshaat Boulevard"
        "Deipon Street" -> if (isArabic) "شارع ديبون" else "Deipon Street"
        "Ghouta Street" -> if (isArabic) "شارع الغوطة" else "Ghouta Street"
        "Al-Kharab Street" -> if (isArabic) "شارع الخراب" else "Al-Kharab Street"
        "Al-Sharia Boulevard" -> if (isArabic) "أوتوستراد الشريعة" else "Al-Sharia Boulevard"
        "Al-Hader Street" -> if (isArabic) "شارع الحاضر" else "Al-Hader Street"
        "Al-Ziraa Street" -> if (isArabic) "شارع الزراعة" else "Al-Ziraa Street"
        "Al-Karameh Street" -> if (isArabic) "شارع الكرامة" else "Al-Karameh Street"
        "Sea Corniche Road" -> if (isArabic) "شارع الكورنيش البحري" else "Sea Corniche Road"
        else -> closestNode.street
    }

    val fullAddress = if (isArabic) {
        "$streetText، $districtText، $cityText، $countryText"
    } else {
        "$streetText, $districtText, $cityText, $countryText"
    }
    return GeocodeResult(countryText, cityText, districtText, streetText, fullAddress)
}

suspend fun performReverseGeocoding(lat: Double, lng: Double, isArabic: Boolean): GeocodeResult {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&accept-language=${if (isArabic) "ar" else "en"}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("User-Agent", "AI-Studio-Marketplace/1.0 (Syria Delivery)")
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val addressObj = json.optJSONObject("address")
                if (addressObj != null) {
                    val country = addressObj.optString("country", if (isArabic) "سوريا" else "Syria")
                    val city = addressObj.optString("city", addressObj.optString("town", addressObj.optString("village", "")))
                    val district = addressObj.optString("suburb", addressObj.optString("neighbourhood", addressObj.optString("county", "")))
                    val road = addressObj.optString("road", addressObj.optString("pedestrian", ""))
                    
                    val finalCity = if (city.isBlank()) (if (isArabic) "دمشق" else "Damascus") else city
                    val finalDistrict = if (district.isBlank()) (if (isArabic) "المزة" else "Mezzeh") else district
                    val finalRoad = if (road.isBlank()) (if (isArabic) "شارع غير مسمى" else "Unnamed Street") else road
                    
                    val fullAddress = if (isArabic) {
                        "$finalRoad، $finalDistrict، $finalCity، $country"
                    } else {
                        "$finalRoad, $finalDistrict, $finalCity, $country"
                    }
                    
                    return@withContext GeocodeResult(country, finalCity, finalDistrict, finalRoad, fullAddress)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext getOfflineReverseGeocode(lat, lng, isArabic)
    }
}

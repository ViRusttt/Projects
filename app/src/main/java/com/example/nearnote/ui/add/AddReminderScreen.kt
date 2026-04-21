package com.example.nearnote.ui.add

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.nearnote.domain.model.Reminder
import com.example.nearnote.ui.theme.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

fun parseCoordinates(input: String): Pair<Double, Double>? {
    val cleaned = input.trim().replace(" ", ",").replace(",,", ",")
    val parts = cleaned.split(",")
    return try {
        if (parts.size == 2) {
            val lat = parts[0].trim().toDouble()
            val lon = parts[1].trim().toDouble()
            if (lat in -90.0..90.0 && lon in -180.0..180.0) Pair(lat, lon)
            else null
        } else null
    } catch (e: Exception) { null }
}

sealed class SearchItem {
    data class Coordinate(val lat: Double, val lon: Double) : SearchItem()
    data class Place(val address: Address) : SearchItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onBack: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    
    var title by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedRadius by remember { mutableStateOf(300f) }
    var selectedPoint by remember { mutableStateOf(GeoPoint(13.7563, 100.5018)) } // Bangkok default
    val radii = listOf(100f, 300f, 500f, 1000f)
    val radiiLabels = listOf("100 m", "300 m", "500 m", "1 km")

    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    Configuration.getInstance().userAgentValue = context.packageName

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    selectedPoint = GeoPoint(loc.latitude, loc.longitude)
                }
            }
        }
    }

    fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    selectedPoint = GeoPoint(loc.latitude, loc.longitude)
                }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun doSearch() {
        if (searchQuery.isBlank()) return
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isSearching = true }
            try {
                val coords = parseCoordinates(searchQuery)
                if (coords != null) {
                    withContext(Dispatchers.Main) {
                        suggestions = listOf(SearchItem.Coordinate(coords.first, coords.second))
                        showSuggestions = true
                    }
                } else {
                    val results = geocoder.getFromLocationName(searchQuery, 5) ?: emptyList()
                    withContext(Dispatchers.Main) {
                        suggestions = results.map { SearchItem.Place(it) }
                        showSuggestions = results.isNotEmpty()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showSuggestions = false }
            } finally {
                withContext(Dispatchers.Main) { isSearching = false }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        val coords = parseCoordinates(searchQuery)
        if (coords != null) {
            suggestions = listOf(SearchItem.Coordinate(coords.first, coords.second))
            showSuggestions = true
            return@LaunchedEffect
        }

        if (searchQuery.length < 3) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        delay(500)
        withContext(Dispatchers.IO) {
            try {
                val results = geocoder.getFromLocationName(searchQuery, 5) ?: emptyList()
                withContext(Dispatchers.Main) {
                    suggestions = results.map { SearchItem.Place(it) }
                    showSuggestions = results.isNotEmpty()
                }
            } catch (e: Exception) { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Green)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Map
            Text("Pick Location", fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium)
            
            Box {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search place or enter coordinates") },
                    trailingIcon = {
                        IconButton(onClick = { doSearch() }) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Green)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Green)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() })
                )

                DropdownMenu(
                    expanded = showSuggestions,
                    onDismissRequest = { showSuggestions = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    suggestions.forEach { item ->
                        when (item) {
                            is SearchItem.Place -> {
                                val address = item.address
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.LocationOn, 
                                                contentDescription = null, 
                                                tint = Green, 
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                val mainText = address.featureName ?: address.thoroughfare ?: "Unknown location"
                                                val subText = "${address.locality ?: ""}${if(address.locality != null && address.countryName != null) ", " else ""}${address.countryName ?: ""}"
                                                Text(mainText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                                if (subText.isNotBlank()) {
                                                    Text(subText, color = GrayText, fontSize = 12.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPoint = GeoPoint(address.latitude, address.longitude)
                                        searchQuery = ""
                                        showSuggestions = false
                                    }
                                )
                            }
                            is SearchItem.Coordinate -> {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.MyLocation,
                                                contentDescription = null,
                                                tint = Green,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text("Go to coordinates", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                                Text("${item.lat}, ${item.lon}", color = GrayText, fontSize = 12.sp, maxLines = 1)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPoint = GeoPoint(item.lat, item.lon)
                                        searchQuery = ""
                                        showSuggestions = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxSize()) {
                    OsmMapPicker(
                        modifier = Modifier.fillMaxSize(),
                        context = context,
                        point = selectedPoint,
                        radius = selectedRadius,
                        onPointSelected = { selectedPoint = it }
                    )
                }
                
                FloatingActionButton(
                    onClick = { fetchCurrentLocation() },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Green,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }
            Text("Tap on map to set location", fontSize = 11.sp, color = GrayText)

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Name") },
                placeholder = { Text("e.g. Buy groceries") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green)
            )

            // Radius
            Text("Alert Radius", fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                radii.forEachIndexed { i, r ->
                    val selected = selectedRadius == r
                    FilterChip(
                        selected = selected,
                        onClick = { selectedRadius = r },
                        label = { Text(radiiLabels[i], fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenLight,
                            selectedLabelColor = GreenDark
                        )
                    )
                }
            }

            // Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Notes (things to do)") },
                placeholder = { Text("Milk\nEggs\nShampoo") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(10.dp),
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green)
            )

            // Save
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(Reminder(
                            title = title,
                            latitude = selectedPoint.latitude,
                            longitude = selectedPoint.longitude,
                            radiusMeters = selectedRadius,
                            noteText = noteText
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun OsmMapPicker(
    modifier: Modifier,
    context: Context,
    point: GeoPoint,
    radius: Float,
    onPointSelected: (GeoPoint) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(point)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            // Marker
            val marker = Marker(mapView).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            // Radius circle
            val circle = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(point, radius.toDouble())
                fillColor = 0x221D9E75
                strokeColor = 0xFF1D9E75.toInt()
                strokeWidth = 2f
            }
            // Tap handler
            val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    onPointSelected(p)
                    return true
                }
                override fun longPressHelper(p: GeoPoint) = false
            })
            mapView.overlays.addAll(listOf(circle, marker, tapOverlay))
            mapView.controller.animateTo(point)
            mapView.invalidate()
        }
    )
}

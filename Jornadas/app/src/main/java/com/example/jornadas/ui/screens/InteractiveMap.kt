package com.example.jornadas.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import com.example.jornadas.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.jornadas.ui.components.MemoryDetailDialog
import com.example.jornadas.viewmodels.AppViewModelProvider
import com.example.jornadas.viewmodels.interactivemap.InteractiveUiState
import com.example.jornadas.viewmodels.interactivemap.InteractiveViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMap(
    onBackClick: () -> Unit,
    viewModel: InteractiveViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {

    val context = LocalContext.current
    val markers by viewModel.markers.collectAsState()
    var selectedMemory by remember { mutableStateOf<InteractiveUiState?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            value = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-5.20, -39.53), 5f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if(!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if(location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                        durationMs = 1500
                    )
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.loadMemorieMarkers(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interactive_map)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                onMapClick = { selectedMemory = null }
            ) {
                markers.forEach { state ->
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.memory.imageUri)
                            .allowHardware(false)
                            .size(256)
                            .error(R.drawable.placeholder)
                            .fallback(R.drawable.placeholder)
                            .placeholder(R.drawable.placeholder)
                            .build()
                    )
                    MarkerComposable(
                        keys = arrayOf(painter.state),
                        state = rememberMarkerState(position = state.location),
                        title = state.memory.title,
                        onClick = {
                            selectedMemory = state
                            true // retornar true impede que o infoview do google apareça
                        }
                    ) {
                        val imageState = painter.state

                        Image(
                            painter = painter,
                            contentDescription = state.memory.description,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(3.dp, Color.White, RoundedCornerShape(14.dp))
                        )
                    }
                }

                selectedMemory?.let { state ->
                    MemoryDetailDialog(memory = state.memory, onDismiss = {selectedMemory = null})
                }
            }
        }
    }
}
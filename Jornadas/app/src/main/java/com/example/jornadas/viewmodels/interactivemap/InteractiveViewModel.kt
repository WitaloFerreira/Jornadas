package com.example.jornadas.viewmodels.interactivemap

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jornadas.data.repository.MemoryRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class InteractiveViewModel(private val repository: MemoryRepository) : ViewModel() {
    private val _markers = MutableStateFlow<List<InteractiveUiState>>(emptyList())
    val markers: StateFlow<List<InteractiveUiState>> = _markers

    private val userId = Firebase.auth.currentUser?.uid?: ""

    fun loadMemorieMarkers(context: Context) {
        viewModelScope.launch {
            repository.getMemoriesStream(userId).collect() { memories ->
                val resolvedMarkers = mutableListOf<InteractiveUiState>()
                val geocoder = Geocoder(context, Locale.getDefault())

                memories.forEach { memory ->
                    Log.d("MARKER_DEBUG", "titulo: ${memory.title} | imageUri: ${memory.imageUri}")
                    val coords = resolveCoords(geocoder, memory.location)
                    if(coords != null) {
                        resolvedMarkers.add(InteractiveUiState(memory, coords))
                    }
                }
                _markers.value = resolvedMarkers
            }
        }
    }

    private suspend fun resolveCoords(geocoder: Geocoder, adress: String): LatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val results = geocoder.getFromLocationName(adress, 1)
                results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
            } catch (e: Exception) {
                null
            }
        }
    }

}
package com.example.jornadas.viewmodels.interactivemap

import com.example.jornadas.data.entities.Memory
import com.google.android.gms.maps.model.LatLng

data class InteractiveUiState(
    val memory: Memory,
    val location: LatLng
)

# --- Gson / Retrofit ---
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.cryptocompare.model.** { *; }
-keep class com.cryptocompare.network.dto.** { *; }

# --- WebSocket DTO ---
-keep class com.cryptocompare.network.dto.webSocketDTO.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }

# --- Coroutines (иногда нужно) ---
-keepclassmembers class kotlinx.coroutines.** { *; }

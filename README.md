# HiFi Smart EQ — FINAL

**Creado por Gilbert Centeno**

Proyecto Android en Kotlin + Jetpack Compose Material 3 para experimentar con procesamiento de audio por sesión usando APIs públicas de Android.

## Incluye

- 10 bandas.
- Frecuencia 20 Hz–20 kHz.
- Ganancia ±12 dB.
- Parámetro Q conservado en el modelo/UI.
- Preamp.
- Limiter real mediante `DynamicsProcessing`.
- BassBoost y Virtualizer cuando el dispositivo los admite.
- Detección de sesiones con `AudioManager.getActivePlaybackConfigurations()` y `AudioPlaybackCallback`.
- Obtención del `audioSessionId` de configuraciones de reproducción.
- Analizador FFT mediante `Visualizer` cuando la sesión permite capturarlo.
- Algoritmo Smart EQ conservador con smoothing y límite de ±6 dB.
- Foreground Service `mediaPlayback`.
- Notificación persistente.
- WakeLock temporal y liberado al destruir el servicio.
- Notification Listener opcional.
- Tests unitarios.
- GitHub Actions.

## Limitación de Android

`DynamicsProcessing` se adjunta a un `audioSessionId`; no es un mezclador maestro universal. Android documenta que el efecto se asocia a la sesión del `AudioTrack`/`MediaPlayer`. `DynamicsProcessing.EqBand` tiene `enabled`, `cutoffFrequency` y `gain`, pero no un parámetro Q real.

Por eso el Q se mantiene en el modelo y se utiliza para la representación/Smart EQ, pero **no se afirma falsamente que Android esté aplicando Q mediante `DynamicsProcessing.EqBand`**.

El proyecto usa `AudioManager` para detectar configuraciones de reproducción activas y obtener sesiones cuando están expuestas por el sistema. Esto mejora mucho la detección frente a depender únicamente de MediaSession, pero no convierte el procesamiento en universal: algunas aplicaciones, rutas directas o dispositivos pueden no permitir la asociación.

El FFT también es best-effort: `Visualizer` puede ser rechazado por la plataforma, el dispositivo o la sesión. Cuando eso ocurre, Smart EQ no inventa datos.

## Sin root / ADB / VPN

No usa root, ADB, `VpnService`, APIs privadas, reflection para saltarse restricciones ni modificación del sistema.

## Compilar

Requiere Android Studio con JDK 17 y Android SDK 34.

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

En este archivo generado en un entorno sin red no fue posible incluir el binario `gradle-wrapper.jar`. El `gradle-wrapper.properties` ya está preparado para Gradle 8.7. Una vez generado el wrapper real en un equipo con Gradle, GitHub Actions podrá usar `./gradlew`.

## GitHub Actions

El workflow compila y ejecuta tests automáticamente y publica la APK debug como artifact.

## Privacidad

No incluye Firebase, AdMob, Crashlytics, Sentry, Mixpanel ni telemetría propia.

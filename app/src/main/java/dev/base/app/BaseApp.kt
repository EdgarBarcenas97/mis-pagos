package dev.base.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: genera el contenedor de dependencias de la app. */
@HiltAndroidApp
class BaseApp : Application()

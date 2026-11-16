package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe Application raiz do Westeros Codex, responsável por inicializar
 * o grafo de dependências do Hilt em nível de aplicação.
 */
@HiltAndroidApp
class WesterosCodexApp : Application()

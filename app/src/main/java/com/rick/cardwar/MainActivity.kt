package com.rick.cardwar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rick.cardwar.ui.GameScreen
import com.rick.cardwar.ui.theme.CardWarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CardWarTheme(darkTheme = true, dynamicColor = false) {
                GameScreen()
            }
        }
    }
}

package com.kyoten.kymusic


import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyoten.kymusic.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {

    var startAnimation by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "logoScale"
    )

    LaunchedEffect(true) {
        startAnimation = true

        delay(2200)

        onFinished()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060818),
                        Color(0xFF12172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(R.drawable.splash_image),
            contentDescription = "KMusic Logo",
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
        )
    }
}
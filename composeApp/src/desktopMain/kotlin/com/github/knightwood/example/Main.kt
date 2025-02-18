package com.github.knightwood.example

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.core.startApplication
import com.github.knightwood.example.acts.SplashActivity
import com.github.knightwood.example.acts.TestActivity
import kotlin.random.Random

//fun main() = startApplication(
//    SplashActivity::class.java,
//    MainApplication::class.java
//)

//或者
fun main() = startApplication<SplashActivity, MainApplication>()


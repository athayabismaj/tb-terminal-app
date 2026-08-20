package com.tbterminal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.tbterminal.app.data.session.SessionEvent
import com.tbterminal.app.navigation.AppNavGraph
import com.tbterminal.app.navigation.AppRoute
import com.tbterminal.app.navigation.AppRouteAccessPolicy
import com.tbterminal.app.navigation.resolveStartDestination
import com.tbterminal.app.ui.auth.AuthViewModel
import com.tbterminal.app.ui.theme.TbterminalappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as TbTerminalApplication).appContainer
        setContent {
            TbterminalappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.factory(appContainer.authRepository)
                    )
                    val sessionManager = appContainer.sessionManager
                    val startDestination = remember {
                        sessionManager.resolveStartDestination()
                    }

                    LaunchedEffect(navController) {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            sessionManager.events.collect { event ->
                                when (event) {
                                    SessionEvent.Unauthorized -> {
                                        authViewModel.notifySessionExpired()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(navController, sessionManager) {
                        navController.currentBackStackEntryFlow.collect { entry ->
                            val route = entry.destination.route
                            val role = sessionManager.readSessionUser()?.role
                            if (!AppRouteAccessPolicy.isAllowed(route, role)) {
                                navController.navigate(AppRoute.Dashboard.route) {
                                    popUpTo(route ?: AppRoute.Dashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    LaunchedEffect(appContainer) {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            appContainer.networkMonitor.observeOnline().collect { isOnline ->
                                if (isOnline) {
                                    appContainer.offlineSyncScheduler.scheduleIfEnabled()
                                }
                            }
                        }
                    }

                    DisposableEffect(navController, sessionManager) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> sessionManager.lockForResume()
                                Lifecycle.Event.ON_START -> {
                                    if (sessionManager.requiresPinUnlock()) {
                                        navController.navigate(AppRoute.Pin.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                }

                                else -> Unit
                            }
                        }

                        lifecycle.addObserver(observer)

                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        authViewModel = authViewModel,
                        sessionManager = sessionManager,
                        appContainer = appContainer
                    )
                }
            }
        }
    }
}


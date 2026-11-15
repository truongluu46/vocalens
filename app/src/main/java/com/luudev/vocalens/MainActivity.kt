package com.luudev.vocalens

import com.luudev.vocalens.presentation.vocabulary.CollectionViewModel
import com.luudev.vocalens.data.repository.LocalCollectionRepositoryImpl
import com.luudev.vocalens.domain.usecase.ObserveCollections
import com.luudev.vocalens.domain.usecase.ObserveCollectionWords
import com.luudev.vocalens.domain.usecase.SaveCollection
import com.luudev.vocalens.domain.usecase.DeleteCollection
import com.luudev.vocalens.domain.usecase.SetCollectionWords
import com.luudev.vocalens.domain.usecase.RemoveCollectionWord
import android.os.Bundle
import com.luudev.vocalens.presentation.camera.CameraViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModelProvider
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import com.luudev.vocalens.presentation.welcome.WelcomeScreen
import com.luudev.vocalens.presentation.welcome.WelcomeViewModel
import com.luudev.vocalens.presentation.login.LoginScreen
import com.luudev.vocalens.presentation.login.LoginViewModel
import com.luudev.vocalens.presentation.register.RegisterScreen
import com.luudev.vocalens.presentation.register.RegisterViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.luudev.vocalens.data.repository.LocalDashboardRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetDashboard
import com.luudev.vocalens.presentation.navigation.MainNavigation
import com.luudev.vocalens.presentation.home.HomeViewModel
import androidx.lifecycle.createSavedStateHandle
import com.luudev.vocalens.data.repository.LocalVocabularyRepositoryImpl
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.presentation.vocabulary.VocabularyViewModel
import com.luudev.vocalens.data.repository.LocalPracticeRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetPracticeLessons
import com.luudev.vocalens.presentation.practice.PracticeViewModel
import androidx.compose.runtime.LaunchedEffect
import com.luudev.vocalens.data.repository.LocalProfileRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetProfile
import com.luudev.vocalens.domain.usecase.SaveProfile
import com.luudev.vocalens.presentation.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.luudev.vocalens.data.repository.FirebaseAuthRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetCurrentUserUseCase
import com.luudev.vocalens.domain.usecase.LoginUseCase
import com.luudev.vocalens.domain.usecase.LogoutUseCase
import com.luudev.vocalens.domain.usecase.RegisterUseCase
import com.luudev.vocalens.domain.usecase.SendPasswordResetUseCase
import com.luudev.vocalens.data.local.LocalLearningStatistics
import com.luudev.vocalens.data.repository.LocalUserRepositoryImpl
import com.luudev.vocalens.domain.usecase.SynchronizeLocalUser
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        val viewModel = ViewModelProvider(this)[WelcomeViewModel::class.java]
        val cameraViewModel = ViewModelProvider(this)[CameraViewModel::class.java]
        val database = VocabLensDatabase.getInstance(applicationContext)
        val statistics = LocalLearningStatistics(database.statisticsDao(), database.studyDao())
        val synchronizeLocalUser = SynchronizeLocalUser(LocalUserRepositoryImpl(database.userDao()))
        val authRepository = FirebaseAuthRepositoryImpl(FirebaseAuth.getInstance())
        val loginViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                LoginViewModel(createSavedStateHandle(), LoginUseCase(authRepository),
                    SendPasswordResetUseCase(authRepository), LogoutUseCase(authRepository), GetCurrentUserUseCase(authRepository))
            }
        })[LoginViewModel::class.java]
        val registerViewModel = ViewModelProvider(this, viewModelFactory {
            initializer { RegisterViewModel(createSavedStateHandle(), RegisterUseCase(authRepository)) }
        })[RegisterViewModel::class.java]
        val homeViewModel = ViewModelProvider(this, viewModelFactory {
            initializer { HomeViewModel(GetDashboard(LocalDashboardRepositoryImpl(database.userDao(), statistics))) }
        })[HomeViewModel::class.java]
        val vocabularyViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                val repository = LocalVocabularyRepositoryImpl(database.vocabularyDao())
                VocabularyViewModel(createSavedStateHandle(), GetSavedVocabulary(repository), DeleteVocabularyWord(repository),
                    com.luudev.vocalens.domain.usecase.LookupVocabulary(com.luudev.vocalens.data.repository.RemoteDictionaryRepositoryImpl()),
                    com.luudev.vocalens.domain.usecase.SaveVocabularyWord(repository))
            }
        })[VocabularyViewModel::class.java]
        val collectionViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                val repository = LocalCollectionRepositoryImpl(database.collectionDao())
                CollectionViewModel(ObserveCollections(repository), ObserveCollectionWords(repository),
                    SaveCollection(repository), DeleteCollection(repository), SetCollectionWords(repository),
                    RemoveCollectionWord(repository), GetSavedVocabulary(LocalVocabularyRepositoryImpl(database.vocabularyDao())))
            }
        })[CollectionViewModel::class.java]
        val practiceViewModel = ViewModelProvider(this, viewModelFactory {
            initializer { PracticeViewModel(createSavedStateHandle(), GetPracticeLessons(LocalPracticeRepositoryImpl(database.lessonDao()))) }
        })[PracticeViewModel::class.java]
        val profileViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                val repository = LocalProfileRepositoryImpl(database.userDao(), statistics)
                ProfileViewModel(GetProfile(repository), SaveProfile(repository))
            }
        })[ProfileViewModel::class.java]
        setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                var sessionReady by remember { mutableStateOf(false) }
                var sessionError by remember { mutableStateOf(false) }
                var retry by remember { mutableIntStateOf(0) }
                val user = if (loginViewModel.uiState.showHome) authRepository.currentUser() else null
                LaunchedEffect(user?.uid, retry) {
                    sessionReady = false
                    sessionError = false
                    vocabularyViewModel.onUserChanged(null)
                    collectionViewModel.onUserChanged(null)
                    profileViewModel.onUserChanged(null)
                    practiceViewModel.onUserChanged(null)
                    homeViewModel.onUserChanged(null)
                    if (user != null) {
                        try {
                            synchronizeLocalUser(user)
                            vocabularyViewModel.onUserChanged(user.uid)
                            collectionViewModel.onUserChanged(user.uid)
                            profileViewModel.onUserChanged(user.uid)
                            practiceViewModel.onUserChanged(user.uid)
                            homeViewModel.onUserChanged(user.uid)
                            sessionReady = true
                        } catch (error: CancellationException) { throw error }
                        catch (error: Exception) { sessionError = true }
                    }
                }
                LaunchedEffect(registerViewModel.uiState.registered) {
                    if (registerViewModel.uiState.registered) {
                        loginViewModel.onRegistrationSucceeded()
                        registerViewModel.clearPasswords()
                    }
                }
                BackHandler(enabled = viewModel.uiState.showLogin && !loginViewModel.uiState.showHome) {
                    if (loginViewModel.uiState.isLoading || registerViewModel.uiState.isLoading) {
                        // Keep the request on screen until its result is handled.
                    } else if (loginViewModel.uiState.showRegistration) {
                        registerViewModel.clearPasswords()
                        loginViewModel.onBackFromRegistration()
                    } else {
                        viewModel.onBackToWelcome()
                    }
                }
                if (loginViewModel.uiState.showHome && !sessionReady) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        if (sessionError) {
                            Text(stringResource(R.string.local_storage_error))
                            TextButton(onClick = { retry++ }) { Text(stringResource(R.string.local_storage_retry)) }
                            TextButton(onClick = loginViewModel::onLogout) { Text(stringResource(R.string.profile_logout)) }
                        } else CircularProgressIndicator()
                    }
                } else if (loginViewModel.uiState.showHome) {
                    MainNavigation(
                        homeUiState = homeViewModel.uiState,
                        onHomeAction = homeViewModel::onAction,
                        onDismissHomeAction = homeViewModel::onDismissAction,
                        vocabularyViewModel = vocabularyViewModel,
                        collectionViewModel = collectionViewModel,
                        cameraViewModel = cameraViewModel,
                        practiceViewModel = practiceViewModel,
                        profileViewModel = profileViewModel,
                        onLogoutConfirmed = {
                            vocabularyViewModel.onUserChanged(null)
                            collectionViewModel.onUserChanged(null)
                            profileViewModel.onUserChanged(null)
                            practiceViewModel.onUserChanged(null)
                            homeViewModel.onUserChanged(null)
                            profileViewModel.onDismissDialog()
                            homeViewModel.onDismissAction()
                            registerViewModel.clearPasswords()
                            loginViewModel.onLogout()
                            viewModel.onStartClick()
                        }
                    )
                } else if (viewModel.uiState.showLogin && loginViewModel.uiState.showRegistration) {
                    RegisterScreen(
                        uiState = registerViewModel.uiState,
                        onNameChange = registerViewModel::onNameChange,
                        onEmailChange = registerViewModel::onEmailChange,
                        onPasswordChange = registerViewModel::onPasswordChange,
                        onConfirmPasswordChange = registerViewModel::onConfirmPasswordChange,
                        onTogglePassword = registerViewModel::onTogglePassword,
                        onCreateAccountClick = registerViewModel::onCreateAccountClick,
                        onLoginClick = {
                            registerViewModel.clearPasswords()
                            loginViewModel.onBackFromRegistration()
                        },
                        onNoticeDismissed = registerViewModel::onNoticeDismissed
                    )
                } else if (viewModel.uiState.showLogin) {
                    LoginScreen(
                        uiState = loginViewModel.uiState,
                        onEmailChange = loginViewModel::onEmailChange,
                        onPasswordChange = loginViewModel::onPasswordChange,
                        onTogglePassword = loginViewModel::onTogglePassword,
                        onLoginClick = loginViewModel::onLoginClick,
                        onForgotPasswordClick = loginViewModel::onForgotPasswordClick,
                        onRegisterClick = loginViewModel::onRegisterClick,
                        onNoticeDismissed = loginViewModel::onNoticeDismissed
                    )
                } else {
                    WelcomeScreen(onStartClick = viewModel::onStartClick)
                }
            }
        }
    }
}

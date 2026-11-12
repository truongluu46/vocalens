package com.luudev.vocalens.presentation.login

import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.luudev.vocalens.presentation.components.FormIcon
import com.luudev.vocalens.presentation.components.FormGlyph
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.BrandHeader
import com.luudev.vocalens.presentation.components.messageResource
import com.luudev.vocalens.presentation.theme.VocaLensTheme

private val LoginBlue = KoiColors.Primary
private val LoginInk = KoiColors.Ink
private val LoginMuted = KoiColors.Muted

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onNoticeDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val noticeText = uiState.error?.let { stringResource(it.messageResource()) } ?: uiState.notice?.let {
        stringResource(when (it) {
            LoginNotice.PasswordResetSent -> R.string.auth_reset_sent
        })
    }
    LaunchedEffect(uiState.notice, uiState.error) {
        if (noticeText != null) {
            snackbarHostState.showSnackbar(noticeText)
            onNoticeDismissed()
        }
    }
    val submit = {
        focusManager.clearFocus()
        onLoginClick()
    }

    Box(modifier.fillMaxSize().pondBackground().safeDrawingPadding().imePadding()) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val viewportHeight = maxHeight
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
                    .padding(horizontal = 24.dp).padding(top = 40.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader()
                Spacer(Modifier.height(30.dp))
                Text(stringResource(R.string.login_title), color = LoginInk,
                    fontSize = 24.sp, lineHeight = 31.sp, letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                Text(stringResource(R.string.login_subtitle), color = LoginMuted,
                    fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(27.dp))
                Column(Modifier.fillMaxWidth()) {
                    FieldLabel(stringResource(R.string.login_email_label))
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(
                        value = uiState.email,
                        enabled = !uiState.isLoading,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { FieldHint(stringResource(R.string.login_email_hint)) },
                        leadingIcon = { FormIcon(FormGlyph.Email) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = loginFieldColors(),
                        textStyle = TextStyle(color = LoginInk, fontSize = 14.sp),
                        isError = uiState.invalidEmail,
                        supportingText = if (uiState.invalidEmail) {
                            { Text(stringResource(R.string.login_email_error)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    Spacer(Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.login_password_label))
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(
                        value = uiState.password,
                        enabled = !uiState.isLoading,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { FieldHint(stringResource(R.string.login_password_hint)) },
                        leadingIcon = { FormIcon(FormGlyph.Lock) },
                        trailingIcon = {
                            IconButton(onClick = onTogglePassword) {
                                FormIcon(
                                    glyph = if (uiState.passwordVisible) FormGlyph.EyeOff else FormGlyph.Eye,
                                    description = stringResource(if (uiState.passwordVisible)
                                        R.string.login_hide_password else R.string.login_show_password)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = loginFieldColors(),
                        textStyle = TextStyle(color = LoginInk, fontSize = 14.sp),
                        visualTransformation = if (uiState.passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        isError = uiState.invalidPassword,
                        supportingText = if (uiState.invalidPassword) {
                            { Text(stringResource(R.string.login_password_error)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                    TextButton(onClick = onForgotPasswordClick,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(start = 12.dp)) {
                        Text(stringResource(R.string.login_forgot_password), color = LoginBlue,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = submit,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .shadow(14.dp, RoundedCornerShape(16.dp),
                                ambientColor = LoginBlue.copy(alpha = 0.25f),
                                spotColor = LoginBlue.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoginBlue)
                    ) {
                        Text(stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.login_submit), color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(32.dp))
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.login_no_account), color = LoginMuted, fontSize = 12.sp)
                    TextButton(onClick = onRegisterClick,
                        enabled = !uiState.isLoading,
                        contentPadding = PaddingValues(horizontal = 5.dp)) {
                        Text(stringResource(R.string.login_register), color = LoginBlue,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = LoginInk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun FieldHint(text: String) {
    Text(text, color = LoginMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = KoiColors.Surface,
    unfocusedContainerColor = KoiColors.Surface,
    errorContainerColor = KoiColors.Surface,
    focusedBorderColor = LoginBlue,
    unfocusedBorderColor = KoiColors.Outline,
    cursorColor = LoginBlue
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun LoginScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        LoginScreen(LoginUiState(), {}, {}, {}, {}, {}, {}, {})
    }
}

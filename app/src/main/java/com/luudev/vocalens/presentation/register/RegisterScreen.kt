package com.luudev.vocalens.presentation.register

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.luudev.vocalens.presentation.components.FormGlyph
import com.luudev.vocalens.presentation.components.FormIcon
import com.luudev.vocalens.presentation.theme.VocaLensTheme

private val RegisterBlue = KoiColors.Primary
private val RegisterInk = KoiColors.Ink
private val RegisterMuted = KoiColors.Muted

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    onNoticeDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(uiState.error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onNoticeDismissed()
        }
    }
    val submit = {
        focusManager.clearFocus()
        onCreateAccountClick()
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
                Text(stringResource(R.string.register_title), color = RegisterInk,
                    fontSize = 24.sp, lineHeight = 31.sp, letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                Text(stringResource(R.string.register_subtitle), color = RegisterMuted,
                    fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(27.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RegistrationField(
                        enabled = !uiState.isLoading,
                        label = stringResource(R.string.register_name_label),
                        value = uiState.fullName, onValueChange = onNameChange,
                        hint = stringResource(R.string.register_name_hint), glyph = FormGlyph.Person,
                        error = if (uiState.validation.invalidName) stringResource(R.string.register_name_error) else null
                    )
                    RegistrationField(
                        enabled = !uiState.isLoading,
                        label = stringResource(R.string.login_email_label),
                        value = uiState.email, onValueChange = onEmailChange,
                        hint = stringResource(R.string.login_email_hint), glyph = FormGlyph.Email,
                        keyboardType = KeyboardType.Email,
                        error = if (uiState.validation.invalidEmail) stringResource(R.string.login_email_error) else null
                    )
                    RegistrationField(
                        enabled = !uiState.isLoading,
                        label = stringResource(R.string.login_password_label),
                        value = uiState.password, onValueChange = onPasswordChange,
                        hint = stringResource(R.string.login_password_hint), glyph = FormGlyph.Lock,
                        keyboardType = KeyboardType.Password,
                        obscureText = !uiState.passwordVisible,
                        error = if (uiState.validation.invalidPassword) stringResource(R.string.login_password_error) else null,
                        trailingIcon = {
                            IconButton(onClick = onTogglePassword) {
                                FormIcon(
                                    if (uiState.passwordVisible) FormGlyph.EyeOff else FormGlyph.Eye,
                                    stringResource(if (uiState.passwordVisible)
                                        R.string.login_hide_password else R.string.login_show_password)
                                )
                            }
                        }
                    )
                    RegistrationField(
                        enabled = !uiState.isLoading,
                        label = stringResource(R.string.register_confirmation_label),
                        value = uiState.confirmPassword, onValueChange = onConfirmPasswordChange,
                        hint = stringResource(R.string.register_confirmation_hint), glyph = FormGlyph.Lock,
                        keyboardType = KeyboardType.Password, obscureText = true,
                        imeAction = ImeAction.Done, onDone = submit,
                        error = if (uiState.validation.invalidConfirmation)
                            stringResource(R.string.register_confirmation_error) else null
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = submit,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                        .shadow(14.dp, RoundedCornerShape(16.dp),
                            ambientColor = RegisterBlue.copy(alpha = 0.25f),
                            spotColor = RegisterBlue.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RegisterBlue)
                ) {
                    Text(stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.register_submit), color = Color.White,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(32.dp))
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.register_has_account), color = RegisterMuted, fontSize = 12.sp)
                    TextButton(onClick = onLoginClick,
                        enabled = !uiState.isLoading,
                        contentPadding = PaddingValues(horizontal = 5.dp)) {
                        Text(stringResource(R.string.login_submit), color = RegisterBlue,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun RegistrationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    glyph: FormGlyph,
    error: String?,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    obscureText: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column {
        Text(label, color = RegisterInk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            enabled = enabled,
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            placeholder = { Text(hint, color = RegisterMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
            leadingIcon = { FormIcon(glyph) }, trailingIcon = trailingIcon,
            singleLine = true, shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = RegisterInk, fontSize = 14.sp),
            visualTransformation = if (obscureText) PasswordVisualTransformation() else VisualTransformation.None,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KoiColors.Surface, unfocusedContainerColor = KoiColors.Surface,
                errorContainerColor = KoiColors.Surface, focusedBorderColor = RegisterBlue,
                unfocusedBorderColor = KoiColors.Outline, cursorColor = RegisterBlue
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun RegisterScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        RegisterScreen(RegisterUiState(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

package com.example.whoareyou.ocrcontact

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whoareyou.R
import com.example.whoareyou.home.HomeScreen
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import com.example.whoareyou.home.CameraManager
import com.example.whoareyou.repository.ContactRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whoareyou.viewmodel.OcrState
import com.example.whoareyou.viewmodel.OcrViewModel

@Composable
fun DataConfirmScreen(
    onBack: () -> Unit, 
    contact: Contact,
    imageUri: Uri? = null,
    navController: NavController?,
    onRetakePhoto: (Uri) -> Unit
) {
    var emailValid by remember { mutableStateOf<Boolean?>(null) }
    val isSaveEnabled = emailValid == true
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember { ContactRepository() }

    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = cameraManager?.photoUri
            if (uri != null) {
                onRetakePhoto(uri)
            }
        }
    }

    LaunchedEffect(cameraLauncher) {
        cameraManager = CameraManager(context, cameraLauncher)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraManager?.launchCamera()
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(24.dp)
                    .clickable { onBack() },
                tint = Color.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "연락처 확인",
            fontSize = 25.sp,
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.pretendard_bold)),
        )

        Text(
            text = "명함에서 추출된 정보를 확인해주세요.",
            fontSize = 20.sp,
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.pretendard_light)),
        )

        Spacer(Modifier.height(20.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            ContactBox("이름", contact.name)
            DrawLine()
            ContactBox("전화번호", contact.phoneNumber)
            DrawLine()
            EmailValidationBox(
                email = contact.email,
                onResult = { result -> emailValid = result }
            )
            DrawLine()
            ContactBox("주소", contact.addressText)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 30.dp, end = 30.dp, top = 20.dp)
        ) {
            Button(
                onClick = {
                    android.util.Log.d("DataConfirm", "DataConfirmScreen에서 다시 찍기 버튼 클릭")
                    val permission = android.Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        cameraManager?.launchCamera()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "다시 찍기",
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.pretendard_medium)),
                    modifier = Modifier.padding(7.dp)
                )
            }

            Button(
                onClick = {
                    if (isSaveEnabled) {
                        scope.launch {
                            repository.saveContact(contact, imageUri)
                                .onSuccess {
                                    Toast.makeText(context, "명함이 저장되었습니다", Toast.LENGTH_SHORT).show()
                                    navController?.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                                .onFailure {
                                    Toast.makeText(context, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                },
                enabled = isSaveEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "저장하기",
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.pretendard_medium)),
                    modifier = Modifier.padding(7.dp)
                )
            }
        }
    }
}

@Composable
fun ContactBox(param: String, value: String) {
    Column(
    ) {
        Text(
            text = param,
            fontSize = 17.sp,
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.pretendard_light)),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.pretendard_medium)),
        )
    }
}

@Composable
fun EmailValidationBox(
    email: String,
    onResult: (Boolean?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isValid by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(email) {
        val result = EmailValidation.validateEmail(email)
        isValid = result?.isValid
        isLoading = false
        onResult(result?.isValid)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "이메일",
                fontSize = 17.sp,
                color = Color.Black,
                fontFamily = FontFamily(Font(R.font.pretendard_light))
            )

            Spacer(modifier = Modifier.weight(1f))

            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "검사 중...",
                            fontSize = 13.sp,
                            fontFamily = FontFamily(Font(R.font.pretendard_light)),
                            color = Color.Gray
                        )
                    }
                }

                isValid == true -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid Email",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "유효한 이메일",
                            fontSize = 13.sp,
                            color = Color(0xFF4CAF50),
                            fontFamily = FontFamily(Font(R.font.pretendard_light))
                        )
                    }
                }

                isValid == false -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Invalid Email",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "유효하지 않음",
                            fontSize = 13.sp,
                            color = Color.Red,
                            fontFamily = FontFamily(Font(R.font.pretendard_light))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = email,
            fontSize = 20.sp,
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.pretendard_medium))
        )
    }
}

@Composable
fun DrawLine() {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
    ) {
        drawLine(
            color = Color(0xAA808080),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun DataConfirmScreenWrapper(imageUri: Uri?, navController: NavController?) {
    var goToMain by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val ocrViewModel: OcrViewModel = viewModel()
    val ocrState by ocrViewModel.ocrState.collectAsState()
    var currentImageUri by remember { mutableStateOf(imageUri) }
    var lastProcessedUri by remember { mutableStateOf<Uri?>(null) }

    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = cameraManager?.photoUri
            if (uri != null) {
                android.util.Log.d("DataConfirm", "새로운 사진 촬영 완료: $uri")
                ocrViewModel.resetOcrState()
                lastProcessedUri = null
                currentImageUri = uri
            }
        }
    }

    LaunchedEffect(cameraLauncher) {
        cameraManager = CameraManager(context, cameraLauncher)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraManager?.launchCamera()
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentImageUri) {
        if (currentImageUri != null && currentImageUri != lastProcessedUri) {
            android.util.Log.d("DataConfirm", "새로운 이미지로 OCR 처리 시작: $currentImageUri")
            android.util.Log.d("DataConfirm", "이전 처리된 URI: $lastProcessedUri")
            ocrViewModel.processImage(context, currentImageUri!!)
            lastProcessedUri = currentImageUri
        }
    }

    LaunchedEffect(ocrState) {
        when (ocrState) {
            is OcrState.Initial -> android.util.Log.d("DataConfirm", "OCR 상태: Initial")
            is OcrState.Loading -> android.util.Log.d("DataConfirm", "OCR 상태: Loading")
            is OcrState.Success -> {
                val contact = (ocrState as OcrState.Success).contact
                android.util.Log.d("DataConfirm", "OCR 상태: Success - $contact")
            }
            is OcrState.Error -> {
                val error = (ocrState as OcrState.Error).message
                android.util.Log.e("DataConfirm", "OCR 상태: Error - $error")
            }
        }
    }

    if (goToMain) {
        if (navController != null) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }
    } else {
        when (ocrState) {
            is OcrState.Success -> DataConfirmScreen(
                onBack = { goToMain = true },
                contact = (ocrState as OcrState.Success).contact,
                imageUri = currentImageUri,
                navController = navController,
                onRetakePhoto = { newImageUri ->
                    android.util.Log.d("DataConfirm", "다시 찍기 요청: $newImageUri")
                    ocrViewModel.resetOcrState()
                    lastProcessedUri = null
                    currentImageUri = newImageUri
                }
            )
            is OcrState.Error -> {
                val errorMessage = (ocrState as OcrState.Error).message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F7))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "OCR 처리 실패",
                        fontSize = 24.sp,
                        color = Color.Black,
                        fontFamily = FontFamily(Font(R.font.pretendard_bold))
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = errorMessage,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily(Font(R.font.pretendard_light)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 30.dp)
                    ) {
                        Button(
                            onClick = { goToMain = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "돌아가기",
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.pretendard_medium)),
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                android.util.Log.d("DataConfirm", "오류 화면에서 다시 촬영 버튼 클릭")
                                val permission = android.Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    cameraManager?.launchCamera()
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "다시 촬영",
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.pretendard_medium)),
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("명함 정보 추출 중...")
                }
            }
        }
    }
}
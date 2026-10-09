package com.hazuny.noshnote.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.hazuny.noshnote.ocr.NutritionLabelParser
import com.hazuny.noshnote.ocr.NutritionLabelCandidates
import com.hazuny.noshnote.ocr.NutritionLabelValues
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun NutritionLabelScannerDialog(
    reviewNote: String,
    onDismiss: () -> Unit,
    onApply: (NutritionLabelValues) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var stableValues by remember { mutableStateOf<NutritionLabelValues?>(null) }
    var candidateDetected by remember { mutableStateOf(false) }
    var panelDetected by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("성분표를 화면에 비춰 주세요") }
    var scanGeneration by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (permissionGranted) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { viewContext ->
                            PreviewView(viewContext).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView = it },
                    )
                    Column(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("성분표를 화면에 비춰 주세요", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("칼로리와 단백질이 읽히면 자동으로 멈춰요", color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = if (panelDetected || candidateDetected || stableValues != null) Color(0xFF16885A) else Color.Black.copy(alpha = .45f),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                when {
                                    stableValues != null -> "✓ 영양정보 인식 완료"
                                    candidateDetected -> "영양정보 감지 · 값 확인 중"
                                    panelDetected -> "성분표 영역 감지 · 확대 인식 중"
                                    else -> "영양정보 인식 중"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(.88f).height(230.dp),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            2.dp,
                            if (panelDetected || candidateDetected || stableValues != null) Color(0xFF31D58A) else Color.White.copy(alpha = .85f),
                        ),
                    ) {}
                    TextButtonClose(onDismiss, Modifier.align(Alignment.TopEnd).padding(12.dp))
                    if (cameraError != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(cameraError!!, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val values = stableValues
                        if (values == null) {
                            Text(scanStatus, style = MaterialTheme.typography.titleSmall)
                            Text("표면을 펴고 빛 반사가 없도록 비스듬히 비춰 주세요. 초점이 흐리면 화면을 탭해 맞춰 주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            if (cameraError != null) Text("카메라를 사용할 수 없어요. 수동 입력으로 계속해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("읽은 값을 확인해 주세요", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Text("${formatNutritionValue(values.caloriesKcal)} kcal")
                                Text("단백질 ${formatNutritionValue(values.proteinG)} g")
                            }
                            Text(
                                "표기 기준: ${values.basisDescription ?: "확인되지 않음"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                reviewNote,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    stableValues = null
                                    candidateDetected = false
                                    panelDetected = false
                                    cameraError = null
                                    scanStatus = "성분표를 다시 찾고 있어요"
                                    scanGeneration++
                                },
                                enabled = stableValues != null,
                                modifier = Modifier.weight(1f),
                            ) { Text("다시 인식") }
                            Button(
                                onClick = { stableValues?.let(onApply) },
                                enabled = stableValues != null,
                                modifier = Modifier.weight(1f),
                            ) { Text("값 적용") }
                        }
                    }
                }
            } else {
                TextButtonClose(onDismiss, Modifier.align(Alignment.End).padding(12.dp))
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (permissionDenied) "카메라 권한이 필요해요" else "카메라 권한을 확인하고 있어요", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "권한 없이도 음식 정보를 직접 입력할 수 있어요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (permissionDenied) {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("권한 다시 요청") }
                    }
                }
            }
        }
    }

    DisposableEffect(previewView, permissionGranted, lifecycleOwner, scanGeneration) {
        val view = previewView
        if (view == null || !permissionGranted) {
            onDispose { }
        } else {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val analyzerExecutor = Executors.newSingleThreadExecutor()
            val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            val locked = AtomicBoolean(false)
            val disposed = AtomicBoolean(false)
            val recentCandidates = ArrayDeque<NutritionLabelCandidates>()
            var stableCalories: Double? = null
            var stableProtein: Double? = null
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            var cameraProvider: ProcessCameraProvider? = null
            var boundCamera: Camera? = null
            var previewUseCase: Preview? = null
            var analysisUseCase: ImageAnalysis? = null
            var lastCropAtMillis = 0L

            fun publishFrame(
                frameCandidates: NutritionLabelCandidates,
                hasPanel: Boolean,
                quality: FrameQuality,
                recognizedText: String,
            ) {
                recentCandidates.addLast(frameCandidates)
                while (recentCandidates.size > 3) recentCandidates.removeFirst()
                val candidateDetectedInRecentFrames = recentCandidates.any {
                    it.caloriesKcal != null || it.proteinG != null
                }
                fun stableValue(values: List<Double>, round: (Double) -> Long): Double? =
                    values.groupBy(round)
                        .maxByOrNull { it.value.size }
                        ?.value
                        ?.takeIf { it.size >= 2 }
                        ?.last()
                stableValue(
                    recentCandidates.mapNotNull(NutritionLabelCandidates::caloriesKcal),
                    { it.roundToLong() },
                )?.let { stableCalories = it }
                stableValue(
                    recentCandidates.mapNotNull(NutritionLabelCandidates::proteinG),
                    { (it * 10).roundToLong() },
                )?.let { stableProtein = it }
                val basis = recentCandidates.lastOrNull { it.basisDescription != null }?.basisDescription
                val confirmedValues = if (stableCalories != null && stableProtein != null) {
                    NutritionLabelValues(stableCalories, stableProtein, basis)
                } else {
                    null
                }
                val nextStatus = when {
                    quality.glareRatio >= 0.12 -> "빛 반사가 보여요. 조명을 비스듬히 비춰 주세요."
                    quality.sharpness < 18.0 -> "초점이 흐려요. 화면을 탭해 초점을 맞춰 주세요."
                    recognizedText.isBlank() -> "글자를 찾지 못했어요. 성분표를 더 가까이 비춰 주세요."
                    stableCalories != null && stableProtein == null -> "칼로리는 읽었어요. 단백질 표기를 찾고 있어요."
                    stableProtein != null && stableCalories == null -> "단백질은 읽었어요. 칼로리 표기를 찾고 있어요."
                    candidateDetectedInRecentFrames -> "칼로리와 단백질을 확인하고 있어요. 잠시 고정해 주세요."
                    hasPanel -> "성분표를 찾았어요. 영역을 확대해 영양값을 읽고 있어요."
                    else -> "칼로리·단백질 표기를 찾지 못했어요. 성분표를 가이드 안에 맞춰 주세요."
                }
                if (confirmedValues != null && locked.compareAndSet(false, true)) {
                    mainExecutor.execute {
                        panelDetected = hasPanel
                        candidateDetected = true
                        stableValues = confirmedValues
                        scanStatus = "수치를 찾았어요. 입력 전에 확인해 주세요."
                    }
                } else {
                    mainExecutor.execute {
                        if (stableValues == null) {
                            panelDetected = hasPanel
                            candidateDetected = candidateDetectedInRecentFrames
                            scanStatus = nextStatus
                        }
                    }
                }
            }

            cameraProviderFuture.addListener({
                try {
                    if (disposed.get()) return@addListener
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                    val targetResolution = if (view.display?.rotation == android.view.Surface.ROTATION_90 ||
                        view.display?.rotation == android.view.Surface.ROTATION_270
                    ) {
                        Size(1920, 1080)
                    } else {
                        Size(1080, 1920)
                    }
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                targetResolution,
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build()
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    previewUseCase = preview
                    analysisUseCase = analysis
                    analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        if (locked.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val quality = assessImageQuality(imageProxy)
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                        recognizer.process(inputImage)
                            .addOnSuccessListener { recognizedText ->
                                val fullCandidates = parseCandidatesWithLayout(recognizedText)
                                val panelBounds = findNutritionPanelBounds(
                                    recognizedText,
                                    imageProxy.width,
                                    imageProxy.height,
                                    rotationDegrees,
                                )
                                val canCrop = panelBounds != null &&
                                    System.currentTimeMillis() - lastCropAtMillis >= 700L
                                if (panelBounds != null) {
                                    mainExecutor.execute { if (stableValues == null) panelDetected = true }
                                }

                                if (canCrop) {
                                    lastCropAtMillis = System.currentTimeMillis()
                                    val proteinRowBounds = findProteinRowBounds(recognizedText, requireNotNull(panelBounds))
                                    val croppedBitmap = runCatching {
                                        cropNutritionPanel(
                                            imageProxy,
                                            proteinRowBounds ?: requireNotNull(panelBounds),
                                            rotationDegrees,
                                        )
                                    }.getOrNull()
                                    if (croppedBitmap != null) {
                                        recognizer.process(InputImage.fromBitmap(croppedBitmap, 0))
                                            .addOnSuccessListener { croppedText ->
                                                val croppedCandidates = parseCandidatesWithLayout(croppedText)
                                                publishFrame(
                                                    frameCandidates = NutritionLabelCandidates(
                                                        caloriesKcal = croppedCandidates.caloriesKcal ?: fullCandidates.caloriesKcal,
                                                        proteinG = croppedCandidates.proteinG ?: fullCandidates.proteinG,
                                                        basisDescription = croppedCandidates.basisDescription ?: fullCandidates.basisDescription,
                                                    ),
                                                    hasPanel = true,
                                                    quality = quality,
                                                    recognizedText = croppedText.text.ifBlank { recognizedText.text },
                                                )
                                            }
                                            .addOnFailureListener {
                                                publishFrame(fullCandidates, true, quality, recognizedText.text)
                                            }
                                            .addOnCompleteListener {
                                                croppedBitmap.recycle()
                                                imageProxy.close()
                                            }
                                    } else {
                                        publishFrame(fullCandidates, true, quality, recognizedText.text)
                                        imageProxy.close()
                                    }
                                } else {
                                    publishFrame(fullCandidates, panelBounds != null, quality, recognizedText.text)
                                    imageProxy.close()
                                }
                            }
                            .addOnFailureListener {
                                recentCandidates.addLast(NutritionLabelCandidates(null, null, null))
                                while (recentCandidates.size > 3) recentCandidates.removeFirst()
                                mainExecutor.execute {
                                    if (stableValues == null) {
                                        candidateDetected = recentCandidates.any {
                                            it.caloriesKcal != null || it.proteinG != null
                                        }
                                        scanStatus = "문자를 읽지 못했어요. 빛 반사를 줄이고 다시 비춰 주세요."
                                    }
                                }
                                imageProxy.close()
                            }
                    }
                    provider.unbindAll()
                    if (disposed.get()) return@addListener
                    boundCamera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    view.setOnTouchListener { touchedView, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val point = view.meteringPointFactory.createPoint(event.x, event.y)
                            boundCamera?.cameraControl?.startFocusAndMetering(
                                FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build(),
                            )
                            touchedView.performClick()
                            true
                        } else {
                            true
                        }
                    }
                } catch (_: Exception) {
                    mainExecutor.execute { cameraError = "카메라를 시작하지 못했어요. 권한과 카메라 연결을 확인해 주세요." }
                }
            }, mainExecutor)

            onDispose {
                disposed.set(true)
                analysisUseCase?.clearAnalyzer()
                view.setOnTouchListener(null)
                boundCamera = null
                cameraProvider?.let { provider ->
                    provider.unbind(*listOfNotNull(previewUseCase, analysisUseCase).toTypedArray())
                }
                recognizer.close()
                analyzerExecutor.shutdown()
            }
        }
    }
}

@Composable
private fun TextButtonClose(onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier) {
        Text("닫기", color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatNutritionValue(value: Double): String =
    formatInputAmount(value)

private val nutritionPanelAnchor = Regex(
    "영양정보|영양성분|나트륨|탄수화물|당류|단백질|포화지방|트랜스지방|지방|열량|칼로리|에너지",
)
private val proteinLabelAnchor = Regex("단백질|protein", RegexOption.IGNORE_CASE)
private val gramValueAnchor = Regex("(?:[0-9]+(?:[.,][0-9]+)*|[.,][0-9]+)(?:g|그램)", RegexOption.IGNORE_CASE)
private val numericValueAnchor = Regex("(?:[0-9]+(?:[.,][0-9]+)*|[.,][0-9]+)")
private val nutritionWhitespace = Regex("\\s+")

private fun parseCandidatesWithLayout(recognizedText: Text): NutritionLabelCandidates {
    val candidates = NutritionLabelParser.parseCandidates(recognizedText.text)
    if (candidates.proteinG != null) return candidates

    val lines = recognizedText.textBlocks
        .flatMap { it.lines }
        .mapNotNull { line -> line.boundingBox?.let { box -> line to box } }
    val labels = lines.filter { (line, _) -> proteinLabelAnchor.containsMatchIn(line.text) }
    val gramValues = lines.filter { (line, _) -> gramValueAnchor.containsMatchIn(line.text.replace(nutritionWhitespace, "")) }
    if (labels.isEmpty()) return candidates

    val layoutCandidates = labels.flatMap { (labelLine, labelBox) ->
        gramValues.mapNotNull { (valueLine, valueBox) ->
            if (labelLine === valueLine) return@mapNotNull null
            if (valueBox.centerX() < labelBox.centerX()) return@mapNotNull null
            val verticalDistance = abs(labelBox.centerY() - valueBox.centerY())
            val rowTolerance = maxOf(12f, maxOf(labelBox.height(), valueBox.height()) * 1.5f)
            val horizontalGap = (valueBox.left - labelBox.right).coerceAtLeast(0)
            val maxHorizontalGap = maxOf(240, (recognizedText.textBlocks.maxOfOrNull { it.boundingBox?.right ?: 0 } ?: 0) / 2)
            if (verticalDistance > rowTolerance || horizontalGap > maxHorizontalGap) {
                null
            } else {
                NutritionLabelParser.parseCandidates("${labelLine.text}\n${valueLine.text}")
                    .proteinG
                    ?.let { value -> value to (verticalDistance + horizontalGap * 0.01f) }
            }
        }
    }
    val layoutProtein = layoutCandidates.minByOrNull { it.second }?.first
    if (layoutProtein != null) return candidates.copy(proteinG = layoutProtein)

    // Some small or curved labels yield the nutrient name and number but lose the tiny "g".
    // Recover only the first number immediately after the explicit protein label, or the
    // nearest number to its right on the same visual row. This avoids borrowing total contents
    // or another nutrient's value from a different row.
    val unlabeledUnitCandidates = labels.flatMap { (labelLine, labelBox) ->
        val labelMatch = proteinLabelAnchor.find(labelLine.text) ?: return@flatMap emptyList()
        val tail = labelLine.text.substring(labelMatch.range.last + 1)
        val directNumber = numericValueAnchor.find(tail)
            ?.takeIf { it.range.first <= 8 }
            ?.value
            ?.let { raw ->
                NutritionLabelParser.parseCandidates("${labelMatch.value} ${raw}g").proteinG
            }
        val directCandidate = directNumber?.let { listOf(it to 0f) }.orEmpty()

        val nearbyCandidates = lines.mapNotNull { (valueLine, valueBox) ->
            if (valueLine === labelLine || valueBox.centerX() < labelBox.centerX()) return@mapNotNull null
            val verticalDistance = abs(labelBox.centerY() - valueBox.centerY())
            val rowTolerance = maxOf(12f, maxOf(labelBox.height(), valueBox.height()) * 1.5f)
            val horizontalGap = (valueBox.left - labelBox.right).coerceAtLeast(0)
            val maxHorizontalGap = maxOf(72, labelBox.height() * 8)
            if (verticalDistance > rowTolerance || horizontalGap > maxHorizontalGap) return@mapNotNull null
            val raw = numericValueAnchor.find(valueLine.text)?.value ?: return@mapNotNull null
            val value = NutritionLabelParser.parseCandidates("${labelMatch.value} ${raw}g").proteinG
                ?: return@mapNotNull null
            value to (verticalDistance + horizontalGap * 0.01f)
        }
        directCandidate + nearbyCandidates
    }
    val recoveredProtein = unlabeledUnitCandidates.minByOrNull { it.second }?.first
    return if (recoveredProtein != null) candidates.copy(proteinG = recoveredProtein) else candidates
}

private fun findNutritionPanelBounds(
    recognizedText: Text,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
): Rect? {
    val rotatedWidth = if (rotationDegrees % 180 == 0) imageWidth else imageHeight
    val rotatedHeight = if (rotationDegrees % 180 == 0) imageHeight else imageWidth
    val anchors = recognizedText.textBlocks
        .flatMap { it.lines }
        .filter { nutritionPanelAnchor.containsMatchIn(it.text.replace(nutritionWhitespace, "")) }
        .mapNotNull { it.boundingBox }
    if (anchors.isEmpty()) return null

    val left = anchors.minOf { it.left }
    val top = anchors.minOf { it.top }
    val right = anchors.maxOf { it.right }
    val bottom = anchors.maxOf { it.bottom }
    val anchorWidth = (right - left).coerceAtLeast(1)
    val anchorHeight = (bottom - top).coerceAtLeast(1)
    val horizontalPadding = maxOf(80, (anchorWidth * 0.75f).toInt())
    val topPadding = maxOf(64, (anchorHeight * 0.4f).toInt())
    val bottomPadding = maxOf(220, (anchorHeight * 1.2f).toInt())
    return Rect(
        (left - horizontalPadding).coerceAtLeast(0),
        (top - topPadding).coerceAtLeast(0),
        (right + horizontalPadding).coerceAtMost(rotatedWidth),
        (bottom + bottomPadding).coerceAtMost(rotatedHeight),
    ).takeIf { it.width() >= 80 && it.height() >= 80 }
}

/** Focuses the second OCR pass on the protein row when the first pass found its label. */
private fun findProteinRowBounds(recognizedText: Text, panelBounds: Rect): Rect? {
    val labelBox = recognizedText.textBlocks
        .flatMap { it.lines }
        .firstOrNull { proteinLabelAnchor.containsMatchIn(it.text.replace(nutritionWhitespace, "")) }
        ?.boundingBox
        ?: return null
    val rowPadding = maxOf(36, (labelBox.height() * 2.5f).toInt())
    return Rect(
        panelBounds.left,
        (labelBox.centerY() - rowPadding).coerceAtLeast(panelBounds.top),
        panelBounds.right,
        (labelBox.centerY() + rowPadding).coerceAtMost(panelBounds.bottom),
    ).takeIf { it.width() >= 80 && it.height() >= 40 }
}

private fun cropNutritionPanel(
    image: androidx.camera.core.ImageProxy,
    panelBounds: Rect,
    rotationDegrees: Int,
): Bitmap {
    val source = image.toBitmap()
    var rotated = source
    try {
        if (rotationDegrees % 360 != 0) {
            rotated = Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                Matrix().apply { postRotate(rotationDegrees.toFloat()) },
                true,
            )
        }
        val crop = Rect(panelBounds).apply {
            intersect(0, 0, rotated.width, rotated.height)
        }
        require(crop.width() > 0 && crop.height() > 0) { "Detected nutrition area is outside the camera frame" }
        val initialCrop = Bitmap.createBitmap(rotated, crop.left, crop.top, crop.width(), crop.height())
        val cropped = if (initialCrop === rotated) {
            initialCrop.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            initialCrop
        }
        val scale = (1600f / cropped.width).coerceIn(1f, 4f)
        if (scale <= 1f) return cropped
        return Bitmap.createScaledBitmap(
            cropped,
            (cropped.width * scale).toInt(),
            (cropped.height * scale).toInt(),
            true,
        ).also { cropped.recycle() }
    } finally {
        if (rotated !== source) rotated.recycle()
        source.recycle()
    }
}

private data class FrameQuality(
    val glareRatio: Double,
    val sharpness: Double,
)

/** Lightweight luma-only checks used to give capture guidance; they do not alter OCR pixels. */
private fun assessImageQuality(image: androidx.camera.core.ImageProxy): FrameQuality {
    val plane = image.planes.firstOrNull() ?: return FrameQuality(0.0, Double.MAX_VALUE)
    val buffer = plane.buffer.duplicate()
    val width = image.width
    val height = image.height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val step = 4
    var brightCount = 0
    var sampleCount = 0
    var laplacianSum = 0.0
    var laplacianSquaredSum = 0.0
    var sharpnessSamples = 0

    fun lumaAt(x: Int, y: Int): Int =
        buffer.get(y * rowStride + x * pixelStride).toInt() and 0xFF

    for (y in 1 until height - 1 step step) {
        for (x in 1 until width - 1 step step) {
            val center = lumaAt(x, y)
            sampleCount++
            if (center >= 248) brightCount++
            val laplacian = (
                lumaAt(x - 1, y) + lumaAt(x + 1, y) +
                    lumaAt(x, y - 1) + lumaAt(x, y + 1) - 4 * center
                ).toDouble()
            laplacianSum += laplacian
            laplacianSquaredSum += laplacian * laplacian
            sharpnessSamples++
        }
    }
    val mean = laplacianSum / sharpnessSamples.coerceAtLeast(1)
    val variance = laplacianSquaredSum / sharpnessSamples.coerceAtLeast(1) - mean * mean
    return FrameQuality(
        glareRatio = brightCount.toDouble() / sampleCount.coerceAtLeast(1),
        sharpness = variance.coerceAtLeast(0.0),
    )
}

package com.balancefriends.health_connect

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import java.lang.IllegalArgumentException

const val HEALTH_CONNECT_SETTINGS_ACTION = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
const val PERMISSION_REQUEST_CODE : Int = 522
const val OPEN_HEALTH_CONNECT_APP : Int = 1149
// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1
/** HealthConnectPlugin */
class HealthConnectPlugin : FlutterPlugin, Messages.HealthConnectApi, ActivityAware, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var context: Context
    private var activity: Activity? = null
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var permissionResult: Messages.Result<Messages.PermissionCheckResult>? = null
    private var openHealthConnectResult: Messages.Result<Messages.PermissionCheckResult>? = null
    private lateinit var expectedPermissions: Set<String>
    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set


    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        // checkAvailabilityInternal()
        Messages.HealthConnectApi.setUp(flutterPluginBinding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Messages.HealthConnectApi.setUp(binding.binaryMessenger, null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        binding.addActivityResultListener(this)
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == OPEN_HEALTH_CONNECT_APP) {
            getPermissions(expectedPermissions, openHealthConnectResult)
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val result = requestPermissionActivityContract.parseResult(resultCode, data)
            Log.d("HEALTH_CONNECT", "request: $requestCode, result: $resultCode, permissions: $result")

            if (result == expectedPermissions) {
                permissionResult?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.GRANTED).build()
                )
            } else {
                // 사용불가 상태
                if (result.isNotEmpty() && resultCode == -1) {
                    // 일부 권한만 선택
                    permissionResult?.success(
                        Messages.PermissionCheckResult.Builder()
                            .setStatus(Messages.PermissionStatus.LIMITED).build()
                    )
                } else if (resultCode == -1) {
                    // 설정 Pass : 직접 권한 요청 화면으로 이동 필요
                    permissionResult?.success(
                        Messages.PermissionCheckResult.Builder()
                            .setStatus(Messages.PermissionStatus.PROMPT).build()
                    )
                } else if (resultCode == 0) {
                    // 권한 거부 또는 요청 불가 상태 (앱 미설치, 낮은 API 사용 등)
                    permissionResult?.success(
                        Messages.PermissionCheckResult.Builder()
                            .setStatus(Messages.PermissionStatus.DENIED).build()
                    )
                }
            }
            return true
        }
        Log.d("HEALTH_CONNECT", "request $requestCode, result $resultCode")
        return false
    }

    private fun checkAvailabilityInternal() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            isSupported() -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    override fun checkAvailability(): Messages.ConnectionCheckResult {

        checkAvailabilityInternal()

        return when(availability.value) {
            HealthConnectAvailability.INSTALLED -> {
                PermissionController.createRequestPermissionResultContract()
                Messages.ConnectionCheckResult.Builder()
                        .setStatus(Messages.HealthConnectStatus.INSTALLED).build()
            }
            HealthConnectAvailability.NOT_INSTALLED -> Messages.ConnectionCheckResult.Builder()
                    .setStatus(Messages.HealthConnectStatus.NOT_INSTALLED).build()
            HealthConnectAvailability.NOT_SUPPORTED -> Messages.ConnectionCheckResult.Builder()
                    .setStatus(Messages.HealthConnectStatus.NOT_SUPPORTED).build()
        }
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    override fun hasAllPermissions(
        expected: MutableList<Messages.RecordPermission>,
        result: Messages.Result<Messages.PermissionCheckResult>
    ) {
        val permissions = createPermissions(expected)
        getPermissions(permissions, result)
    }

    /**
     * Determines whether all the specified permissions are already granted. It is recommended to
     * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
     * permissions are already granted then there is no need to request permissions via
     * [PermissionController.createRequestPermissionResultContract].
     */
    suspend fun hasAllPermissionsInternal(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    override fun requestPermission(
        expected: MutableList<Messages.RecordPermission>,
        result: Messages.Result<Messages.PermissionCheckResult>
    ) {
        try {
            val permissions = createPermissions(expected)
            permissionResult = result
            expectedPermissions = permissions

            val intent = requestPermissionActivityContract
            activity?.startActivityForResult(intent.createIntent(context, permissions), PERMISSION_REQUEST_CODE)
        } catch(e: ActivityNotFoundException) {
            Log.e("HEALTH_CONNECT", "failed to run HealthConnect Application", e)
        } catch (e: Exception) {
            result?.error(e)
        }
    }

    override fun openHealthConnect(
        permissions: MutableList<Messages.RecordPermission>,
        result: Messages.Result<Messages.PermissionCheckResult>
    ) {
        openHealthConnectResult = result
        expectedPermissions = createPermissions(permissions)
        try {
            val settingsIntent = Intent()
            settingsIntent.action = HEALTH_CONNECT_SETTINGS_ACTION
            checkAvailabilityInternal()
            if (availability.value == HealthConnectAvailability.INSTALLED && settingsIntent !== null) {
                // context.startActivity(settingsIntent)
                activity?.startActivityForResult(settingsIntent, OPEN_HEALTH_CONNECT_APP)
            } else {
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.RESTRICTED).build()
                )
            }
        } catch(e: ActivityNotFoundException) {
            Log.e("HEALTH_CONNECT", "failed to run HealthConnect Application", e)
        } catch (e: Exception) {
            result?.error(e)
        }
    }

    // Create the permissions launcher.
    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getActivities(
        startMillsEpoch: Long,
        endMillsEpoch: Long,
        result: Messages.Result<MutableList<Messages.ActivityRecord>>
    ) {
        val start = Instant.ofEpochMilli(startMillsEpoch)
        val end = Instant.ofEpochMilli(endMillsEpoch)
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )

        Log.d(
            "HEALTH_CONNECT",
            "Get Exercise Records between ${Date.from(start)} and ${Date.from(end)}"
        )

        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            result?.error(exception)
        }
        coroutineScope.launch(exceptionHandler) {
            val recordResult = healthConnectClient.readRecords(request)
            Log.d("HEALTH_CONNECT", recordResult.records.size.toString())

            val items = recordResult.records.map {
                Log.d("HEALTH_CONNECT", it.exerciseType.toString())
                Log.d("HEALTH_CONNECT", it.title ?: "no title")

                val aggregateRequest = AggregateRequest(
                    metrics = setOf(
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        StepsRecord.COUNT_TOTAL,
                        ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        SpeedRecord.SPEED_AVG,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(it.startTime, it.endTime)
                )

                val aggregated = healthConnectClient.aggregate(aggregateRequest)
                val activityResults = getActivityMetrics(aggregated, it.startTime, it.endTime)

                val sessionResult = Messages.ActivityRecord.Builder()
                sessionResult.setTitle(it.title)
                sessionResult.setExerciseType(it.toExerciseId())
                sessionResult.setBegin(it.startTime.toEpochMilli())
                sessionResult.setEnd(it.endTime.toEpochMilli())
                sessionResult.setId(it.metadata.id)
                sessionResult.setMetadata(it.metadata.toPigeon())
                sessionResult.setMetrics(activityResults)

                return@map sessionResult.build()
            }

            result?.success(items.toMutableList())
        }
    }

    private fun getPermissions(
        permissions: Set<String>,
        result: Messages.Result<Messages.PermissionCheckResult>?
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->

            if (exception is IllegalStateException) {
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.RESTRICTED).build()
                )
            } else {
                result?.error(exception)
            }
        }

        coroutineScope.launch(exceptionHandler) {
            val granted =
                healthConnectClient.permissionController.getGrantedPermissions()

            if (granted.containsAll(permissions)) {
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.GRANTED).build()
                )
            } else if (granted.isNotEmpty()) {
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.LIMITED).build()
                )
            } else {
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.DENIED).build()
                )
            }
        }
    }


    private fun createPermissions(expected: List<Messages.RecordPermission>): Set<String> {
        return expected.map {
            val type = it.type.kotlin()

            if (it.readonly) {
                HealthPermission.getReadPermission(type)
            } else {
                HealthPermission.getWritePermission(type)
            }
        }.toSet()
    }

    private fun Messages.RecordType.kotlin(): KClass<out Record> {
        return when (this) {
            Messages.RecordType.ACTIVE_CALORIES_BURNED_RECORD -> ActiveCaloriesBurnedRecord::class
            Messages.RecordType.BASAL_BODY_TEMPERATURE_RECORD -> BasalBodyTemperatureRecord::class
            Messages.RecordType.BASAL_METABOLIC_RATE_RECORD -> BasalMetabolicRateRecord::class
            Messages.RecordType.BLOOD_GLUCOSE_RECORD -> BloodGlucoseRecord::class
            Messages.RecordType.BLOOD_PRESSURE_RECORD -> BloodPressureRecord::class
            Messages.RecordType.BODY_FAT_RECORD -> BodyFatRecord::class
            Messages.RecordType.BODY_TEMPERATURE_RECORD -> BodyTemperatureRecord::class
            Messages.RecordType.BODY_WATER_MASS_RECORD -> BodyWaterMassRecord::class
            Messages.RecordType.BONE_MASS_RECORD -> BoneMassRecord::class
            Messages.RecordType.CERVICAL_MUCUS_RECORD -> CervicalMucusRecord::class
            Messages.RecordType.CYCLING_PEDALING_CADENCE_RECORD -> CyclingPedalingCadenceRecord::class
            Messages.RecordType.DISTANCE_RECORD -> DistanceRecord::class
            Messages.RecordType.ELEVATION_GAINED_RECORD -> ElevationGainedRecord::class
            Messages.RecordType.ELEVATION_GAINED_RECORD -> ElevationGainedRecord::class
            Messages.RecordType.EXERCISE_SESSION_RECORD -> ExerciseSessionRecord::class
            Messages.RecordType.FLOORS_CLIMBED_RECORD -> FloorsClimbedRecord::class
            Messages.RecordType.HEART_RATE_RECORD -> HeartRateRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_DIFFERENTIAL_INDEX_RECORD -> HeartRateVariabilityDifferentialIndexRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_RMSSD_RECORD -> HeartRateVariabilityRmssdRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_SD2RECORD -> HeartRateVariabilitySd2Record::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_SDANN_RECORD -> HeartRateVariabilitySdannRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_SDNN_INDEX_RECORD -> HeartRateVariabilitySdnnIndexRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_SDNN_RECORD -> HeartRateVariabilitySdnnRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_SDSD_RECORD -> HeartRateVariabilitySdsdRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_SRECORD -> HeartRateVariabilitySRecord::class
            // Messages.RecordType.HEART_RATE_VARIABILITY_TINN_RECORD -> HeartRateVariabilityTinnRecord::class
            Messages.RecordType.HEIGHT_RECORD -> HeightRecord::class
            // Messages.RecordType.HIP_CIRCUMFERENCE_RECORD -> HipCircumferenceRecord::class
            Messages.RecordType.HYDRATION_RECORD -> HydrationRecord::class
            Messages.RecordType.INTERMENSTRUAL_BLEEDING_RECORD -> IntermenstrualBleedingRecord::class
            Messages.RecordType.LEAN_BODY_MASS_RECORD -> LeanBodyMassRecord::class
            Messages.RecordType.MENSTRUATION_FLOW_RECORD -> MenstruationFlowRecord::class
            Messages.RecordType.NUTRITION_RECORD -> NutritionRecord::class
            Messages.RecordType.OVULATION_TEST_RECORD -> OvulationTestRecord::class
            Messages.RecordType.OXYGEN_SATURATION_RECORD -> OxygenSaturationRecord::class
            Messages.RecordType.POWER_RECORD -> PowerRecord::class
            Messages.RecordType.RESPIRATORY_RATE_RECORD -> RespiratoryRateRecord::class
            Messages.RecordType.RESTING_HEART_RATE_RECORD -> RestingHeartRateRecord::class
            Messages.RecordType.SEXUAL_ACTIVITY_RECORD -> SexualActivityRecord::class
            Messages.RecordType.SLEEP_SESSION_RECORD -> SleepSessionRecord::class
            Messages.RecordType.SLEEP_STAGE_RECORD -> SleepStageRecord::class
            Messages.RecordType.SPEED_RECORD -> SpeedRecord::class
            Messages.RecordType.STEPS_CADENCE_RECORD -> StepsCadenceRecord::class
            Messages.RecordType.STEPS_RECORD -> StepsRecord::class
            Messages.RecordType.TOTAL_CALORIES_BURNED_RECORD -> TotalCaloriesBurnedRecord::class
            Messages.RecordType.VO2MAX_RECORD -> Vo2MaxRecord::class
            // Messages.RecordType.WAIST_CIRCUMFERENCE_RECORD -> WaistCircumferenceRecord::class
            Messages.RecordType.WEIGHT_RECORD -> WeightRecord::class
            Messages.RecordType.WHEELCHAIR_PUSHES_RECORD -> WheelchairPushesRecord::class
            else -> throw IllegalArgumentException()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun androidx.health.connect.client.records.metadata.Metadata.toPigeon(): Messages.Metadata {
        val result = Messages.Metadata.Builder()
        result.setClientRecordId(this.clientRecordId)
        result.setClientRecordVersion(this.clientRecordVersion)
        result.setLastModifiedTime(this.lastModifiedTime.toEpochMilli())
        result.setOriginPackageName(this.dataOrigin.packageName)
        result.setDevice(this.device?.toPigeon())
        return result.build()
    }

    private fun Device.toPigeon(): Messages.Device {
        val result = Messages.Device.Builder()
        result.setManufacturer(this.manufacturer)
        result.setModel(this.model)
        result.setType((getDeviceType(this.type)))
        return result.build()
    }

    private fun getDeviceType(typeNumber: Int): Messages.DeviceType {
        return Messages.DeviceType.values()[typeNumber]
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getActivityMetrics(
        aggregation: AggregationResult,
        start: Instant,
        end: Instant
    ): List<Messages.AggregationMetric> {

        val duration =
            aggregation[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL].run {
                var duration = this?.toMillis()
                // calculate by between
                if (this == null) {
                    duration = Duration.between(start, end).toMillis()
                }

                Messages.AggregationMetric.Builder()
                    .setField(stringToMetricField("ActiveTime"))
                    .setType(Messages.AggregationType.TOTAL)
                    .setUnit(stringToMetricUnit("milliseconds"))
                    .setValue(duration.toString())
                    .build()
            }

        val steps = aggregation[StepsRecord.COUNT_TOTAL].run {
            Messages.AggregationMetric.Builder()
                .setField(stringToMetricField("Steps"))
                .setType(Messages.AggregationType.TOTAL)
                .setUnit(stringToMetricUnit("count"))
                .setValue((this ?: 0).toString())
                .build()
        }

        val energy = aggregation[TotalCaloriesBurnedRecord.ENERGY_TOTAL].run {
            Messages.AggregationMetric.Builder()
                .setField(stringToMetricField("TotalCaloriesBurned"))
                .setType(Messages.AggregationType.TOTAL)
                .setUnit(stringToMetricUnit("kiloCalories"))
                .setValue((this?.inKilocalories ?: 0).toString())
                .build()
        }

        val distance = aggregation[DistanceRecord.DISTANCE_TOTAL].run {
            Messages.AggregationMetric.Builder()
                .setField(stringToMetricField("Distance"))
                .setType(Messages.AggregationType.TOTAL)
                .setUnit(stringToMetricUnit("kilometer"))
                .setValue((this?.inKilometers ?: 0).toString())
                .build()
        }

        val speed = aggregation[SpeedRecord.SPEED_AVG].run {
            Messages.AggregationMetric.Builder()
                .setField(stringToMetricField("Speed"))
                .setType(Messages.AggregationType.AVERAGE)
                .setUnit(stringToMetricUnit("kilometersPerHour"))
                .setValue((this?.inKilometersPerHour ?: 0).toString())
                .build()
        }

        return listOf(duration, steps, energy, distance, speed)
    }

    private fun ExerciseSessionRecord.toExerciseId(): Messages.ExerciseId {
        return Messages.ExerciseId.Builder()
            .setType(this.exerciseType.toLong())
            .setName(ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP[this.exerciseType])
            .build()
    }

    private fun stringToMetricField(fieldName: String) : Messages.MetricField {
        return when (fieldName) {
            "ActiveTime" -> {
                Messages.MetricField.ACTIVE_TIME
            }
            "Steps" -> {
                Messages.MetricField.STEPS
            }
            "TotalCaloriesBurned" -> {
                Messages.MetricField.TOTAL_CALORIES_BURNED
            }
            "Distance" -> {
                Messages.MetricField.DISTANCE
            }
            "Speed" -> {
                Messages.MetricField.SPEED
            }
            else -> {
                Messages.MetricField.UNKNOWN
            }
        }
    }

    private fun stringToMetricUnit(fieldName: String) : Messages.MetricUnit {
        return when (fieldName) {
            "minutes" -> {
                Messages.MetricUnit.MINUTES
            }
            "seconds" -> {
                Messages.MetricUnit.SECONDS
            }
            "milliseconds" -> {
                Messages.MetricUnit.MILLISECONDS
            }
            "count" -> {
                Messages.MetricUnit.COUNT
            }
            "kiloCalories" -> {
                Messages.MetricUnit.KILO_CALORIES
            }
            "kilometer" -> {
                Messages.MetricUnit.KILOMETER
            }
            "kilometersPerHour" -> {
                Messages.MetricUnit.KILOMETERS_PER_HOUR
            }
            else -> {
                Messages.MetricUnit.UNKNOWN
            }
        }
    }

    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

}

/**
 * Health Connect requires that the underlying Health Connect APK is installed on the device.
 * [HealthConnectAvailability] represents whether this APK is indeed installed, whether it is not
 * installed but supported on the device, or whether the device is not supported (based on Android
 * version).
 */
enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}
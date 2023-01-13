package kr.balancefriends.health_connect

import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
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
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

/** HealthConnectPlugin */
class HealthConnectPlugin : FlutterPlugin, Messages.HealthConnectApi {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var context: Context
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        Messages.HealthConnectApi.setup(flutterPluginBinding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Messages.HealthConnectApi.setup(binding.binaryMessenger, null)
    }

    override fun checkAvailability(): Messages.ConnectionCheckResult {
        return if (HealthConnectClient.isApiSupported()) {
            if (HealthConnectClient.isProviderAvailable(context)) {
                PermissionController.createRequestPermissionResultContract()
                Messages.ConnectionCheckResult.Builder()
                    .setStatus(Messages.HealthConnectStatus.INSTALLED).build()
            } else {
                Messages.ConnectionCheckResult.Builder()
                    .setStatus(Messages.HealthConnectStatus.NOT_INSTALLED).build()
            }
        } else {
            Messages.ConnectionCheckResult.Builder()
                .setStatus(Messages.HealthConnectStatus.NOT_SUPPORTED).build()
        }
    }

    override fun hasAllPermissions(
        expected: MutableList<Messages.RecordPermission>,
        result: Messages.Result<Messages.PermissionCheckResult>?
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            result?.error(exception)
        }

        coroutineScope.launch(exceptionHandler) {
            val permissions = getPermissions(expected)
            val granted =
                healthConnectClient.permissionController.getGrantedPermissions(permissions)
            Log.d("HEALTH_CONNECT", permissions.toString())

            if (granted == permissions) {
                Log.d("HEALTH_CONNECT", granted.toString())
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.GRANTED).build()
                )
            } else {
                result?.success(
                    Messages.PermissionCheckResult.Builder()
                        .setStatus(Messages.PermissionStatus.DENIED).build()
                )
            }
        }
    }

    override fun openPermissionSetting(expected: MutableList<Messages.RecordPermission>) {
        val permissions = getPermissions(expected)
        val intent = permissionContract.run {
            createIntent(context, permissions)
        }
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private val permissionContract: ActivityResultContract<Set<HealthPermission>, Set<HealthPermission>> =
        PermissionController.createRequestPermissionResultContract()

    override fun getActivities(
        startMillsEpoch: Long,
        endMillsEpoch: Long,
        result: Messages.Result<MutableList<Messages.ActivityRecord>>?
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

    private fun getPermissions(expected: MutableList<Messages.RecordPermission>): Set<HealthPermission> {
        return expected.map {
            val type = it.type.kotlin()

            if (it.readonly) {
                HealthPermission.createReadPermission(type)
            } else {
                HealthPermission.createWritePermission(type)
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
            Messages.RecordType.EXERCISE_EVENT_RECORD -> ExerciseEventRecord::class
            Messages.RecordType.EXERCISE_LAP_RECORD -> ExerciseLapRecord::class
            Messages.RecordType.EXERCISE_REPETITIONS_RECORD -> ExerciseRepetitionsRecord::class
            Messages.RecordType.EXERCISE_SESSION_RECORD -> ExerciseSessionRecord::class
            Messages.RecordType.FLOORS_CLIMBED_RECORD -> FloorsClimbedRecord::class
            Messages.RecordType.HEART_RATE_RECORD -> HeartRateRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_DIFFERENTIAL_INDEX_RECORD -> HeartRateVariabilityDifferentialIndexRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_RMSSD_RECORD -> HeartRateVariabilityRmssdRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_SD2RECORD -> HeartRateVariabilitySd2Record::class
            Messages.RecordType.HEART_RATE_VARIABILITY_SDANN_RECORD -> HeartRateVariabilitySdannRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_SDNN_INDEX_RECORD -> HeartRateVariabilitySdnnIndexRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_SDNN_RECORD -> HeartRateVariabilitySdnnRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_SDSD_RECORD -> HeartRateVariabilitySdsdRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_SRECORD -> HeartRateVariabilitySRecord::class
            Messages.RecordType.HEART_RATE_VARIABILITY_TINN_RECORD -> HeartRateVariabilityTinnRecord::class
            Messages.RecordType.HEIGHT_RECORD -> HeightRecord::class
            Messages.RecordType.HIP_CIRCUMFERENCE_RECORD -> HipCircumferenceRecord::class
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
            Messages.RecordType.SWIMMING_STROKES_RECORD -> SwimmingStrokesRecord::class
            Messages.RecordType.TOTAL_CALORIES_BURNED_RECORD -> TotalCaloriesBurnedRecord::class
            Messages.RecordType.VO2MAX_RECORD -> Vo2MaxRecord::class
            Messages.RecordType.WAIST_CIRCUMFERENCE_RECORD -> WaistCircumferenceRecord::class
            Messages.RecordType.WEIGHT_RECORD -> WeightRecord::class
            Messages.RecordType.WHEELCHAIR_PUSHES_RECORD -> WheelchairPushesRecord::class
        }
    }

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
}

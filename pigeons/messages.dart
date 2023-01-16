import 'package:pigeon/pigeon.dart';

// TODO(danny): abstract this as common one
class ActivityRecord implements IntervalRecord {
  @override
  late final String id;

  late String? title;
  late ExerciseId exerciseType;

  late List<AggregationMetric?> metrics;

  @override
  late int begin;

  @override
  late int end;

  @override
  late Metadata? metadata;
}

class ExerciseId {
  late final int type;
  late final String? name;
}

class IntervalRecord implements Record {
  @override
  late final String id;

  /// milliseconds since epoch
  late int begin;
  late int end;

  @override
  late Metadata? metadata;

}

class Metadata {
  late final String originPackageName;

  /// Optional client supplied record unique data identifier associated with the data.
  /// https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/metadata/Metadata?hl=ko
  late final String? clientRecordId;
  late final int clientRecordVersion;

  // mills since epoch
  late final int lastModifiedTime;
  late final Device? device;
}

class Device {
  late final String? manufacturer;
  late final String? model;
  late final DeviceType type;
}

enum DeviceType {
  unknown,
  watch,
  phone,
  scale,
  ring,
  hmd,
  fitBand,
  chestStrap,
  smartDisplay,
}

class InstantaneousRecord implements Record {
  @override
  late final String id;

  /// milliseconds since epoch
  late int time;

  @override
  late Metadata? metadata;
}

abstract class Record {
  /// uid from health connect
  late final String id;

  late Metadata? metadata;
}

class AggregationMetric {
  /// dataTypeName of AggregateMetric
  late final MetricField field;

  late final MetricUnit unit;
  late final AggregationType type;

  // num value as string
  late final String value;
}

enum AggregationType {
  duration,
  total,
  min,
  max,
  average,
  count,
}

enum MetricField {
  /// milliseconds
  activeTime,
  /// count
  steps,
  /// kcal
  totalCaloriesBurned,
  /// km
  distance,
  /// km/h
  speed,
  unknown,
}

enum MetricUnit {
  count,
  minutes,
  seconds,
  milliseconds,
  kiloCalories,
  kilometer,
  kilometersPerHour,
  unknown,
}

/*enum HealthUnit {
  length,
  energy,
  bloodGlucose,
  temperature,
  power,
  pressure,
  mass,
  percentage,
  velocity,
  volume,
}*/

/*enum HealthDataType {
  activity,
  sleep,
  nutrition,
  bodyMeasurement,
  cycleTracking,
  vitals,
}*/

enum RecordType {
  activeCaloriesBurnedRecord,
  basalBodyTemperatureRecord,
  basalMetabolicRateRecord,
  bloodGlucoseRecord,
  bloodPressureRecord,
  bodyFatRecord,
  bodyTemperatureRecord,
  bodyWaterMassRecord,
  boneMassRecord,
  cervicalMucusRecord,
  cyclingPedalingCadenceRecord,
  distanceRecord,
  elevationGainedRecord,
  exerciseEventRecord,
  exerciseLapRecord,
  exerciseRepetitionsRecord,
  exerciseSessionRecord,
  floorsClimbedRecord,
  heartRateRecord,
  heartRateVariabilityDifferentialIndexRecord,
  heartRateVariabilityRmssdRecord,
  heartRateVariabilitySd2Record,
  heartRateVariabilitySdannRecord,
  heartRateVariabilitySdnnIndexRecord,
  heartRateVariabilitySdnnRecord,
  heartRateVariabilitySdsdRecord,
  heartRateVariabilitySRecord,
  heartRateVariabilityTinnRecord,
  heightRecord,
  hipCircumferenceRecord,
  hydrationRecord,
  intermenstrualBleedingRecord,
  leanBodyMassRecord,
  menstruationFlowRecord,
  nutritionRecord,
  ovulationTestRecord,
  oxygenSaturationRecord,
  powerRecord,
  respiratoryRateRecord,
  restingHeartRateRecord,
  sexualActivityRecord,
  sleepSessionRecord,
  sleepStageRecord,
  speedRecord,
  stepsCadenceRecord,
  stepsRecord,
  swimmingStrokesRecord,
  totalCaloriesBurnedRecord,
  vo2MaxRecord,
  waistCircumferenceRecord,
  weightRecord,
  wheelchairPushesRecord,
}

class RecordPermission {
  late final RecordType type;
  late final bool readonly;
}

enum HealthConnectStatus {
  notSupported,
  notInstalled,
  installed,
}

class ConnectionCheckResult {
  late final HealthConnectStatus status;
}

enum PermissionStatus {
  /// The user fully granted access to the requested feature.
  granted,
  /// The user partially granted access to the requested feature.
  limited,
  /// The user denied access to the requested feature, permission needs to be asked first.
  denied,
  /// The OS denied access to the requested feature.
  /// The user cannot change this app's status.
  restricted,
  /// The user already denied twice.
  /// Permission should be asked via setting screen
  prompt,
}

class PermissionCheckResult {
  late final PermissionStatus status;
}

enum ExerciseType {
  backExtension,
  badminton,
  barbellShoulderPress,
  baseball,
  basketball,
  benchPress,
  benchSitUp,
  biking,
  bikingStationary,
  bootCamp,
  boxing,
  burpee,
  calisthenics,
  cricket,
  crunch,
  dancing,
  deadlift,
  dumbbellCurlLeftArm,
  dumbbellCurlRightArm,
  dumbbellFrontRaise,
  dumbbellLateralRaise,
  dumbbellTricepsExtensionLeftArm,
  dumbbellTricepsExtensionRightArm,
  dumbbellTricepsExtensionTwoArm,
  elliptical,
  exerciseClass,
  fencing,
  footballAmerican,
  footballAustralian,
  forwardTwist,
  frisbeeDisc,
  golf,
  guidedBreathing,
  gymnastics,
  handball,
  highIntensityIntervalTraining,
  hiking,
  iceHockey,
  iceSkating,
  jumpingJack,
  jumpRope,
  latPullDown,
  lunge,
  martialArts,
  paddling,
  paragliding,
  pilates,
  plank,
  racquetball,
  rockClimbing,
  rollerHockey,
  rowing,
  rowingMachine,
  rugby,
  running,
  runningTreadmill,
  sailing,
  scubaDiving,
  skating,
  skiing,
  snowboarding,
  snowshoeing,
  soccer,
  softball,
  squash,
  squat,
  stairClimbing,
  stairClimbingMachine,
  strengthTraining,
  stretching,
  surfing,
  swimmingOpenWater,
  swimmingPool,
  tableTennis,
  tennis,
  upperTwist,
  volleyball,
  walking,
  waterPolo,
  weightlifting,
  wheelchair,
  otherWorkout,
  yoga,
}

@HostApi()
abstract class HealthConnectApi {
  ConnectionCheckResult checkAvailability();

  @async
  PermissionCheckResult hasAllPermissions(List<RecordPermission> expected);

  @async
  PermissionCheckResult requestPermission(List<RecordPermission> permissions);

  @async
  PermissionCheckResult openHealthConnect(List<RecordPermission> permissions);

  @async
  List<ActivityRecord> getActivities(int startMillsEpoch, int endMillsEpoch);
}



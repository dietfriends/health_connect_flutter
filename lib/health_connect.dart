// Autogenerated from Pigeon (v5.0.0), do not edit directly.
// See also: https://pub.dev/packages/pigeon
// ignore_for_file: public_member_api_docs, non_constant_identifier_names, avoid_as, unused_import, unnecessary_parenthesis, prefer_null_aware_operators, omit_local_variable_types, unused_shown_name, unnecessary_import
import 'dart:async';
import 'dart:typed_data' show Float64List, Int32List, Int64List, Uint8List;

import 'package:flutter/foundation.dart' show ReadBuffer, WriteBuffer;
import 'package:flutter/services.dart';

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

enum HealthConnectStatus {
  notSupported,
  notInstalled,
  installed,
}

enum PermissionStatus {
  granted,
  denied,
  restricted,
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

class ActivityRecord {
  ActivityRecord({
    required this.id,
    this.title,
    required this.exerciseType,
    required this.metrics,
    required this.begin,
    required this.end,
    this.metadata,
  });

  String id;

  String? title;

  ExerciseId exerciseType;

  List<AggregationMetric?> metrics;

  int begin;

  int end;

  Metadata? metadata;

  Object encode() {
    return <Object?>[
      id,
      title,
      exerciseType.encode(),
      metrics,
      begin,
      end,
      metadata?.encode(),
    ];
  }

  static ActivityRecord decode(Object result) {
    result as List<Object?>;
    return ActivityRecord(
      id: result[0]! as String,
      title: result[1] as String?,
      exerciseType: ExerciseId.decode(result[2]! as List<Object?>)
,
      metrics: (result[3] as List<Object?>?)!.cast<AggregationMetric?>(),
      begin: result[4]! as int,
      end: result[5]! as int,
      metadata: result[6] != null
          ? Metadata.decode(result[6]! as List<Object?>)
          : null,
    );
  }
}

class ExerciseId {
  ExerciseId({
    required this.type,
    this.name,
  });

  int type;

  String? name;

  Object encode() {
    return <Object?>[
      type,
      name,
    ];
  }

  static ExerciseId decode(Object result) {
    result as List<Object?>;
    return ExerciseId(
      type: result[0]! as int,
      name: result[1] as String?,
    );
  }
}

class Metadata {
  Metadata({
    required this.originPackageName,
    this.clientRecordId,
    required this.clientRecordVersion,
    required this.lastModifiedTime,
    this.device,
  });

  String originPackageName;

  /// Optional client supplied record unique data identifier associated with the data.
  /// https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/metadata/Metadata?hl=ko
  String? clientRecordId;

  int clientRecordVersion;

  int lastModifiedTime;

  Device? device;

  Object encode() {
    return <Object?>[
      originPackageName,
      clientRecordId,
      clientRecordVersion,
      lastModifiedTime,
      device?.encode(),
    ];
  }

  static Metadata decode(Object result) {
    result as List<Object?>;
    return Metadata(
      originPackageName: result[0]! as String,
      clientRecordId: result[1] as String?,
      clientRecordVersion: result[2]! as int,
      lastModifiedTime: result[3]! as int,
      device: result[4] != null
          ? Device.decode(result[4]! as List<Object?>)
          : null,
    );
  }
}

class Device {
  Device({
    this.manufacturer,
    this.model,
    required this.type,
  });

  String? manufacturer;

  String? model;

  DeviceType type;

  Object encode() {
    return <Object?>[
      manufacturer,
      model,
      type.index,
    ];
  }

  static Device decode(Object result) {
    result as List<Object?>;
    return Device(
      manufacturer: result[0] as String?,
      model: result[1] as String?,
      type: DeviceType.values[result[2]! as int]
,
    );
  }
}

class AggregationMetric {
  AggregationMetric({
    required this.field,
    required this.unit,
    required this.type,
    required this.value,
  });

  /// dataTypeName of AggregateMetric
  MetricField field;

  MetricUnit unit;

  AggregationType type;

  String value;

  Object encode() {
    return <Object?>[
      field.index,
      unit.index,
      type.index,
      value,
    ];
  }

  static AggregationMetric decode(Object result) {
    result as List<Object?>;
    return AggregationMetric(
      field: MetricField.values[result[0]! as int]
,
      unit: MetricUnit.values[result[1]! as int]
,
      type: AggregationType.values[result[2]! as int]
,
      value: result[3]! as String,
    );
  }
}

class RecordPermission {
  RecordPermission({
    required this.type,
    required this.readonly,
  });

  RecordType type;

  bool readonly;

  Object encode() {
    return <Object?>[
      type.index,
      readonly,
    ];
  }

  static RecordPermission decode(Object result) {
    result as List<Object?>;
    return RecordPermission(
      type: RecordType.values[result[0]! as int]
,
      readonly: result[1]! as bool,
    );
  }
}

class ConnectionCheckResult {
  ConnectionCheckResult({
    required this.status,
  });

  HealthConnectStatus status;

  Object encode() {
    return <Object?>[
      status.index,
    ];
  }

  static ConnectionCheckResult decode(Object result) {
    result as List<Object?>;
    return ConnectionCheckResult(
      status: HealthConnectStatus.values[result[0]! as int]
,
    );
  }
}

class PermissionCheckResult {
  PermissionCheckResult({
    required this.status,
  });

  PermissionStatus status;

  Object encode() {
    return <Object?>[
      status.index,
    ];
  }

  static PermissionCheckResult decode(Object result) {
    result as List<Object?>;
    return PermissionCheckResult(
      status: PermissionStatus.values[result[0]! as int]
,
    );
  }
}

class _HealthConnectApiCodec extends StandardMessageCodec {
  const _HealthConnectApiCodec();
  @override
  void writeValue(WriteBuffer buffer, Object? value) {
    if (value is ActivityRecord) {
      buffer.putUint8(128);
      writeValue(buffer, value.encode());
    } else if (value is AggregationMetric) {
      buffer.putUint8(129);
      writeValue(buffer, value.encode());
    } else if (value is ConnectionCheckResult) {
      buffer.putUint8(130);
      writeValue(buffer, value.encode());
    } else if (value is Device) {
      buffer.putUint8(131);
      writeValue(buffer, value.encode());
    } else if (value is ExerciseId) {
      buffer.putUint8(132);
      writeValue(buffer, value.encode());
    } else if (value is Metadata) {
      buffer.putUint8(133);
      writeValue(buffer, value.encode());
    } else if (value is PermissionCheckResult) {
      buffer.putUint8(134);
      writeValue(buffer, value.encode());
    } else if (value is RecordPermission) {
      buffer.putUint8(135);
      writeValue(buffer, value.encode());
    } else {
      super.writeValue(buffer, value);
    }
  }

  @override
  Object? readValueOfType(int type, ReadBuffer buffer) {
    switch (type) {
      case 128:       
        return ActivityRecord.decode(readValue(buffer)!);
      
      case 129:       
        return AggregationMetric.decode(readValue(buffer)!);
      
      case 130:       
        return ConnectionCheckResult.decode(readValue(buffer)!);
      
      case 131:       
        return Device.decode(readValue(buffer)!);
      
      case 132:       
        return ExerciseId.decode(readValue(buffer)!);
      
      case 133:       
        return Metadata.decode(readValue(buffer)!);
      
      case 134:       
        return PermissionCheckResult.decode(readValue(buffer)!);
      
      case 135:       
        return RecordPermission.decode(readValue(buffer)!);
      
      default:

        return super.readValueOfType(type, buffer);
      
    }
  }
}

class HealthConnectApi {
  /// Constructor for [HealthConnectApi].  The [binaryMessenger] named argument is
  /// available for dependency injection.  If it is left null, the default
  /// BinaryMessenger will be used which routes to the host platform.
  HealthConnectApi({BinaryMessenger? binaryMessenger})
      : _binaryMessenger = binaryMessenger;
  final BinaryMessenger? _binaryMessenger;

  static const MessageCodec<Object?> codec = _HealthConnectApiCodec();

  Future<ConnectionCheckResult> checkAvailability() async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.HealthConnectApi.checkAvailability', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(null) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else if (replyList[0] == null) {
      throw PlatformException(
        code: 'null-error',
        message: 'Host platform returned null value for non-null return value.',
      );
    } else {
      return (replyList[0] as ConnectionCheckResult?)!;
    }
  }

  Future<PermissionCheckResult> hasAllPermissions(List<RecordPermission?> arg_expected) async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.HealthConnectApi.hasAllPermissions', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(<Object?>[arg_expected]) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else if (replyList[0] == null) {
      throw PlatformException(
        code: 'null-error',
        message: 'Host platform returned null value for non-null return value.',
      );
    } else {
      return (replyList[0] as PermissionCheckResult?)!;
    }
  }

  Future<void> openPermissionSetting(List<RecordPermission?> arg_expected) async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.HealthConnectApi.openPermissionSetting', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(<Object?>[arg_expected]) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else {
      return;
    }
  }

  Future<List<ActivityRecord?>> getActivities(int arg_startMillsEpoch, int arg_endMillsEpoch) async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.HealthConnectApi.getActivities', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(<Object?>[arg_startMillsEpoch, arg_endMillsEpoch]) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else if (replyList[0] == null) {
      throw PlatformException(
        code: 'null-error',
        message: 'Host platform returned null value for non-null return value.',
      );
    } else {
      return (replyList[0] as List<Object?>?)!.cast<ActivityRecord?>();
    }
  }
}

import 'package:logging/logging.dart';

import 'src/messages.g.dart';

final _logger = Logger('health_connect');

class HealthConnect {
  static final HealthConnectApi _api = HealthConnectApi();

  static Future<ConnectionCheckResult> checkAvailability() async {
    try {
      return await _api.checkAvailability();
    } catch (e, s) {
      _logger.warning('checkAvailability failed', e, s);
      rethrow;
    }
  }

  static Future<PermissionCheckResult> hasAllPermissions(
      List<RecordPermission?> expected) async {
    try {
      return await _api.hasAllPermissions(expected);
    } catch (e, s) {
      _logger.warning('hasAllPermissions failed', e, s);
      rethrow;
    }
    return await _api.hasAllPermissions(expected);
  }

  static Future<PermissionCheckResult> requestPermission(
      List<RecordPermission?> permissions) async {
    try {
      return await _api.requestPermission(permissions);
    } catch (e, s) {
      _logger.warning('requestPermission failed', e, s);
      rethrow;
    }
  }

  Future<PermissionCheckResult> openHealthConnect(
      List<RecordPermission?> permissions) async {
    try {
      return await _api.openHealthConnect(permissions);
    } catch (e, s) {
      _logger.warning('openHealthConnect failed', e, s);
      rethrow;
    }
  }

  Future<List<ActivityRecord?>> getActivities(
      int startMillsEpoch, int endMillsEpoch) async {
    try {
      return await _api.getActivities(startMillsEpoch, endMillsEpoch);
    } catch (e, s) {
      _logger.warning('getActivities failed', e, s);
      rethrow;
    }
  }
}

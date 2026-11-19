/// JSON 解析工具: 所有模型手写 fromJson 共用的守卫型 helper (ReqPilotMobile 同款约定)
typedef JsonMap = Map<String, Object?>;

/// 断言值是 JSON 对象, 否则视为服务器数据格式错误
JsonMap asJsonMap(Object? value) {
  if (value is Map<String, Object?>) return value;
  if (value is Map) {
    return value.map((key, item) => MapEntry(key.toString(), item));
  }
  throw const FormatException('服务器数据格式不正确');
}

/// 断言值是 JSON 对象数组
List<JsonMap> asJsonMapList(Object? value) {
  if (value is List) {
    return value.map(asJsonMap).toList(growable: false);
  }
  throw const FormatException('服务器数据格式不正确');
}

String stringOf(JsonMap json, String key, [String fallback = '']) {
  final value = json[key];
  return value is String ? value : fallback;
}

int intOf(JsonMap json, String key, [int fallback = 0]) {
  final value = json[key];
  if (value is int) return value;
  if (value is num) return value.toInt();
  return fallback;
}

bool boolOf(JsonMap json, String key, [bool fallback = false]) {
  final value = json[key];
  return value is bool ? value : fallback;
}

/// 金额分 → 展示字符串 ("19.90"), 币种直接拼后缀 (店面只有一种 currency)
String moneyOf(JsonMap json, String key, String currency) {
  final cents = intOf(json, key);
  final yuan = (cents / 100).toStringAsFixed(2);
  return '$currency $yuan';
}

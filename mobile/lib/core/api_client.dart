import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';

/// 业务/网络失败, message 为可直接展示给用户的文案
class ApiFailure implements Exception {
  const ApiFailure(this.message, {this.code = 0});

  /// R 信封的业务 code (网络错误为 0)
  final int code;
  final String message;

  factory ApiFailure.fromDio(DioException error) {
    final response = error.response;
    if (response != null &&
        response.statusCode != null &&
        response.statusCode! >= 400) {
      final message = _messageOfBody(response.data);
      if (message != null) return ApiFailure(message);
    }
    return switch (error.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout ||
      DioExceptionType.receiveTimeout => const ApiFailure('连接超时, 请稍后重试'),
      DioExceptionType.connectionError => const ApiFailure('无法连接服务器, 请检查地址或网络'),
      _ => const ApiFailure('网络异常, 请稍后重试'),
    };
  }

  static String? _messageOfBody(Object? body) {
    if (body is Map && body['message'] is String) {
      return body['message'] as String;
    }
    return null;
  }

  @override
  String toString() => message;
}

/// 统一 HTTP 网关 (ReqPilotMobile ApiClient 同款骨架 + jade 信封适配):
///
/// - 解包 `R<T>` 信封 `{code, message, data, timestamp}`, code != 0 抛 [ApiFailure]
/// - 挂 [PersistCookieJar] 持久化 `cart_token` (httpOnly cookie, 购物车会话跨启动保留);
///   切换服务器时 DI 链整体重建, 每个 ApiClient 实例自带独立 cookie jar, 互不串用
class ApiClient {
  ApiClient({required String baseUrl, required String cookieDir, Dio? dio})
    : _dio = dio ?? Dio() {
    _dio.options = BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 30),
      sendTimeout: const Duration(seconds: 15),
    );
    _dio.interceptors.add(
      CookieManager(
        PersistCookieJar(ignoreExpires: true, storage: FileStorage(cookieDir)),
      ),
    );
  }

  final Dio _dio;

  Future<Object?> getData(String path, {Map<String, Object?>? query}) {
    return _run(() => _dio.get<Object?>(path, queryParameters: query));
  }

  Future<Object?> postData(String path, {Object? body}) {
    return _run(() => _dio.post<Object?>(path, data: body));
  }

  Future<Object?> putData(String path, {Object? body}) {
    return _run(() => _dio.put<Object?>(path, data: body));
  }

  Future<Object?> deleteData(String path) {
    return _run(() => _dio.delete<Object?>(path));
  }

  /// 关闭底层连接 (服务器切换/测试收尾用)
  void close() => _dio.close();

  Future<Object?> _run(Future<Response<Object?>> Function() request) async {
    try {
      final response = await request();
      return _unwrap(response);
    } on DioException catch (error) {
      throw ApiFailure.fromDio(error);
    }
  }

  Object? _unwrap(Response<Object?> response) {
    final body = response.data;
    if (body is! Map) {
      throw const ApiFailure('服务器响应格式不正确');
    }
    final code = body['code'];
    if (code is! int) {
      throw const ApiFailure('服务器响应格式不正确');
    }
    if (code != 0) {
      throw ApiFailure(body['message']?.toString() ?? '请求失败', code: code);
    }
    return body['data'];
  }
}

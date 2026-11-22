/// 金额展示: 分 → 带币种符号的字符串
library;

const _currencySymbols = <String, String>{
  'USD': r'$',
  'EUR': '€',
  'GBP': '£',
  'CNY': '¥',
  'JPY': 'JP¥',
  'HKD': r'HK$',
  'AUD': r'A$',
  'CAD': r'C$',
};

/// 分 → 展示金额; 无符号映射的币种退化为 "CHF 1.00" 形式
String formatMoney(int cents, {String currency = 'USD'}) {
  final amount = (cents / 100).toStringAsFixed(2);
  final symbol = _currencySymbols[currency];
  return symbol == null ? '$currency $amount' : '$symbol$amount';
}

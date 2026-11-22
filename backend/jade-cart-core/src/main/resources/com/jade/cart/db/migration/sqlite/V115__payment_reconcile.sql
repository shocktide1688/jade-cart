-- 支付对账 (P4): 订单记录网关会话号 (Stripe Checkout Session id / PayPal order id)
-- 用途: webhook 丢失时主动查网关补单 + 迟到款人工排查
ALTER TABLE mall_order ADD COLUMN payment_session_ref VARCHAR(120);

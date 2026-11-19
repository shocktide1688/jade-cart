-- 结算后释放购物车 token (SQLite 方言)
--
-- 背景: mall_cart.token 是唯一约束, 而结算成功后旧购物车转为 CONVERTED 仍占用原 token,
-- 顾客用同 token 再次加购时会新建 ACTIVE 购物车 → 唯一约束冲突 (500)。
-- 新代码在 checkout 时已把 token 改成 'conv-<publicId>' 占位; 本迁移清理历史遗留行。
UPDATE mall_cart
SET token = 'conv-' || public_id
WHERE status = 'CONVERTED'
  AND token NOT LIKE 'conv-%';

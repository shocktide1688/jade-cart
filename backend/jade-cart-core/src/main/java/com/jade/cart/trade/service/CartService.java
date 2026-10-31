package com.jade.cart.trade.service;

import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.settings.entity.StoreSetting;
import com.jade.cart.trade.dto.CartView;
import com.jade.cart.trade.entity.Cart;
import com.jade.cart.trade.entity.CartItem;
import com.jade.common.exception.BizException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 匿名购物车: cookie token 标识, 行不存单价快照 (kartwo 同款)
 * 加购时校验库存上限但不预留 (预留只发生在结算事务)
 */
@ApplicationScoped
public class CartService {

    public static final String CART_COOKIE = "cart_token";

    @Inject
    CatalogService catalogService;

    /** 取 ACTIVE 购物车, 无则新建 (token 保持不变, 结算 CONVERTED 后下次重建) */
    @Transactional
    public Cart getOrCreate(String token) {
        return Cart.activeByToken(token).orElseGet(() -> {
            Cart cart = new Cart();
            cart.token = token;
            cart.persist();
            return cart;
        });
    }

    public CartView view(Cart cart) {
        List<CartView.LineView> lines = new ArrayList<>();
        long subtotal = 0;
        for (CartItem item : cart.items()) {
            Variant variant = Variant.findById(item.variantId);
            // 变体可能已被软删, 跳过脏行
            if (variant == null || variant.deleted) {
                continue;
            }
            Product product = Product.findById(variant.productId);
            int available = Inventory.byVariant(variant.id).map(Inventory::available).orElse(0);
            long unitPrice = variant.priceCents;
            long lineTotal = unitPrice * item.quantity;
            subtotal += lineTotal;
            lines.add(new CartView.LineView(variant.id,
                    product == null ? variant.sku : product.title,
                    product == null ? null : product.slug,
                    variant.sku,
                    catalogService.specText(variant.id),
                    unitPrice, item.quantity, available, lineTotal));
        }
        String currency = StoreSetting.getValue("currency").orElse("USD");
        return new CartView(cart.publicId, currency, lines, subtotal);
    }

    @Transactional
    public CartView addItem(String token, Long variantId, int quantity) {
        if (quantity < 1) {
            throw new BizException("数量必须大于 0");
        }
        Variant variant = Variant.findById(variantId);
        if (variant == null || variant.deleted) {
            throw new BizException("商品变体不存在或已下架");
        }
        Product product = Product.findById(variant.productId);
        if (product == null || !product.isActive()) {
            throw new BizException("商品不存在或已下架");
        }
        int available = Inventory.byVariant(variantId).map(Inventory::available).orElse(0);
        if (available <= 0) {
            throw new BizException("库存不足");
        }

        Cart cart = getOrCreate(token);
        CartItem item = CartItem.byCartAndVariant(cart.id, variantId).orElse(null);
        int newQty = (item == null ? 0 : item.quantity) + quantity;
        int capped = Math.min(newQty, available);
        if (item == null) {
            item = new CartItem();
            item.cartId = cart.id;
            item.variantId = variantId;
            item.quantity = capped;
            item.persist();
        } else {
            item.quantity = capped;
            item.persist();
        }
        return view(cart);
    }

    @Transactional
    public CartView updateItem(String token, Long variantId, int quantity) {
        Cart cart = getOrCreate(token);
        CartItem item = CartItem.byCartAndVariant(cart.id, variantId)
                .orElseThrow(() -> new BizException("购物车中没有该商品"));
        if (quantity <= 0) {
            item.delete();
            return view(cart);
        }
        int available = Inventory.byVariant(variantId).map(Inventory::available).orElse(0);
        item.quantity = Math.min(quantity, Math.max(available, 0));
        item.persist();
        return view(cart);
    }

    @Transactional
    public CartView removeItem(String token, Long variantId) {
        Cart cart = getOrCreate(token);
        CartItem.deleteByCartAndVariant(cart.id, variantId);
        return view(cart);
    }

    /** 新访客 token (32 位随机 hex) */
    public static String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 读 cookie token, 无则生成 (cookie 由控制器写回) */
    public static Optional<String> readToken(jakarta.ws.rs.core.HttpHeaders headers) {
        return Optional.ofNullable(headers.getCookies().get(CART_COOKIE))
                .map(jakarta.ws.rs.core.Cookie::getValue)
                .filter(t -> !t.isBlank());
    }
}

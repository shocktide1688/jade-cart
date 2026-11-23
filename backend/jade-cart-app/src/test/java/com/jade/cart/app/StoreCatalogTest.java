package com.jade.cart.app;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductCategoryLink;
import com.jade.cart.catalog.entity.ProductImage;
import com.jade.cart.catalog.entity.ProductOption;
import com.jade.cart.catalog.entity.ProductOptionValue;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.catalog.service.ProductImageService;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 店面商品目录 JSON API 测试: 列表过滤 / 详情图集变体 / 分类树 / 404 独立库 jade_test, Flyway 自动迁移 */
@QuarkusTest
class StoreCatalogTest {

    @Inject CatalogService catalogService;

    @Inject ProductImageService productImageService;

    @TestHTTPResource URI base;

    @BeforeEach
    void restAssuredBase() {
        RestAssured.baseURI = base.getScheme() + "://" + base.getHost();
        RestAssured.port = base.getPort();
    }

    @BeforeEach
    @Transactional
    void clean() {
        // 先交易表 (购物车行引用变体) 再目录表, 与 OrderFlowTest 同序
        com.jade.cart.trade.entity.WebhookEvent.deleteAll();
        com.jade.cart.trade.entity.OrderItem.deleteAll();
        com.jade.cart.trade.entity.Order.deleteAll();
        com.jade.cart.trade.entity.CartItem.deleteAll();
        com.jade.cart.trade.entity.Cart.deleteAll();
        com.jade.cart.trade.entity.Customer.deleteAll();
        ProductCategoryLink.deleteAll();
        ProductImage.deleteAll();
        ProductOptionValue.deleteAll();
        ProductOption.deleteAll();
        Inventory.deleteAll();
        Variant.deleteAll();
        Product.deleteAll();
        Category.deleteAll();
    }

    /** 建分类, 返回 id */
    @Transactional
    Long seedCategory(String name, String slug, Long parentId) {
        return catalogService.createCategory(name, slug, parentId, 0).id;
    }

    /** 建商品 (自动带一条默认变体), 可选挂分类 */
    @Transactional
    Long seedProduct(
            String title,
            String slug,
            String status,
            boolean featured,
            Long categoryId,
            String mainImagePath) {
        Product product =
                catalogService.createProduct(
                        title,
                        slug,
                        null,
                        null,
                        status,
                        featured,
                        1900L,
                        null,
                        mainImagePath,
                        null,
                        null);
        if (categoryId != null) {
            catalogService.linkCategories(product.id, List.of(categoryId));
        }
        return product.id;
    }

    @Test
    void products_list_filterByCategoryKeywordFeatured() {
        Long apparel = seedCategory("Apparel", "apparel", null);
        Long tees = seedCategory("Tees", "tees", apparel);
        seedProduct("Jade Tee One", "jade-tee-one", Product.STATUS_ACTIVE, true, tees, null);
        seedProduct("Jade Hoodie", "jade-hoodie", Product.STATUS_ACTIVE, false, null, null);

        // 全量 2 件
        given().when()
                .get("/api/store/products")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(2));
        // 分类过滤 (slug → 父分类挂子分类链路)
        given().when()
                .get("/api/store/products?category=tees")
                .then()
                .body("data", hasSize(1))
                .body("data[0].slug", equalTo("jade-tee-one"))
                .body("data[0].minPriceCents", equalTo(1900));
        // 关键词过滤
        given().when()
                .get("/api/store/products?q=Hoodie")
                .then()
                .body("data", hasSize(1))
                .body("data[0].slug", equalTo("jade-hoodie"));
        // 精选过滤
        given().when()
                .get("/api/store/products?featured=true")
                .then()
                .body("data", hasSize(1))
                .body("data[0].featured", equalTo(true));
        // 未知分类 → 空列表 (非错误)
        given().when()
                .get("/api/store/products?category=unknown-slug")
                .then()
                .body("code", equalTo(0))
                .body("data", empty());
    }

    @Test
    void product_detail_galleryVariantsCategories() {
        Long tees = seedCategory("Tees", "tees", null);
        Long productId =
                seedProduct(
                        "Jade Tee Two",
                        "jade-tee-two",
                        Product.STATUS_ACTIVE,
                        false,
                        tees,
                        "img/pdp-main.jpg");
        productImageService.add(productId, "img/pdp-extra.jpg");

        given().when()
                .get("/api/store/products/jade-tee-two")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.product.title", equalTo("Jade Tee Two"))
                .body("data.product.variants", hasSize(1))
                // 主图在前, 补充图在后
                .body("data.imagePaths", hasSize(2))
                .body("data.imagePaths[0]", equalTo("img/pdp-main.jpg"))
                .body("data.imagePaths[1]", equalTo("img/pdp-extra.jpg"))
                .body("data.categorySlugs", hasItem("tees"));
    }

    @Test
    void product_detail_404ForDraftMissingOrArchived() {
        seedProduct("Jade Draft", "jade-draft", Product.STATUS_DRAFT, false, null, null);

        given().when()
                .get("/api/store/products/jade-draft")
                .then()
                .statusCode(200)
                .body("code", equalTo(404));
        given().when()
                .get("/api/store/products/no-such-product")
                .then()
                .statusCode(200)
                .body("code", equalTo(404));
    }

    @Test
    void categories_tree_parentsThenChildren() {
        Long apparel = seedCategory("Apparel", "apparel", null);
        seedCategory("Tees", "tees", apparel);
        seedCategory("Mugs", "mugs", null);

        given().when()
                .get("/api/store/categories")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(2))
                .body("data[0].slug", equalTo("apparel"))
                .body("data[0].children[0].slug", equalTo("tees"))
                .body("data[0].children", hasSize(1))
                .body("data[1].slug", equalTo("mugs"));
    }
}

package com.jade.cart.app.storefront.i18n;

/**
 * 店面 UI 框架文案 (强类型全集): 英文默认值在此内置 (单一事实来源), 其他语言由 i18n/store_&lt;code&gt;.properties 覆盖, 缺 key 自动落英文
 * —— 模板引用的字段编译期保证存在, 不会 Qute Key not found 500。
 *
 * <p>含 {0} 占位符的 seo* 字段供 java.text.MessageFormat 拼装 (值内禁用单引号 ')。 商家内容 (商品/分类/博客/政策) 与装修配置文案不在此层 (阶段
 * 2 的 *_i18n 表)。
 */
public record UiText(
        String htmlLang,
        String homeTitle,
        String poweredBy,
        // 导航
        String navHome,
        String navCategory,
        String navHot,
        String navBlog,
        String navContact,
        String ariaSearch,
        String ariaCart,
        // 页脚
        String footBlurb,
        String ariaEmail,
        String ariaSubscribe,
        String footOurShop,
        String footAllProducts,
        String notFoundTitle,
        String notFoundBody,
        String notFoundBackHome,
        String footBlog,
        String footContact,
        String footPrivacy,
        String footRefund,
        String footTerms,
        String footQuickLinks,
        String footSearch,
        String footCart,
        String footShipping,
        String footRefunds,
        String footBrandTagline,
        String footNewArrivals,
        // 首页
        String welcomeTitle,
        String noProductsYet,
        String featuredTitle,
        String allProducts,
        String newArrivals,
        String from,
        // 面包屑 / 列表
        String crumbHome,
        String crumbProducts,
        String filterAll,
        String searchPlaceholder,
        String nothingHere,
        String soldOut,
        // 商品详情
        String lowStock,
        String stockLabel,
        String left,
        String accShipTitle,
        String accShipBody,
        String accSupportTitle,
        String accSupportBodyPre,
        String contactLink,
        String accBenefitsTitle,
        String accBenefitsBody,
        String unavailable,
        String optionLabel,
        String standard,
        String quantity,
        String addToCart,
        String tabDescription,
        String reviewsTitle,
        String reviewsEmpty,
        String writeReview,
        String faqTitle,
        String faqShipQ,
        String faqShipA,
        String faqUniqueQ,
        String faqUniqueA,
        String faqCareQ,
        String faqCareA,
        String faqReturnQ,
        String faqReturnAPre,
        String refundLink,
        String faqReturnAPost,
        String youMayLike,
        // 购物车 / 结账
        String cartTitle,
        String cartEmptyTitle,
        String cartEmptyBody,
        String continueShopping,
        String crossSell,
        String yourCart,
        String thItem,
        String thPrice,
        String thQty,
        String thSubtotal,
        String update,
        String outOfStock,
        String remove,
        String subtotalLabel,
        String checkout,
        String emailLabel,
        String nameLabel,
        String countryLabel,
        String countryUS,
        String countryCA,
        String countryGB,
        String countryDE,
        String countryAU,
        String continueToPayment,
        String holdNote,
        // 联系页
        String contactTitle,
        String contactIntro,
        String shopNow,
        String sentOk,
        String phName,
        String phEmail,
        String phComment,
        String send,
        String contactErrInvalid,
        String contactErrFailed,
        // 博客
        String journal,
        String noPosts,
        String prevPost,
        String nextPost,
        String backToBlog,
        // 订单
        String orderWord,
        String statusLabel,
        String paidOk,
        String completePayment,
        String thUnitPrice,
        String thTotal,
        String placedAt,
        String paidAt,
        String fulfilledAt,
        String cancelledAt,
        String refundedAt,
        String totalLabel,
        String itemsWord,
        String shippingWord,
        String shipment,
        String carrier,
        String tracking,
        String linkLabel,
        String trackPackage,
        // 收银台
        String mockTitle,
        String mockIntro,
        String thOrder,
        String thAmount,
        String thStatus,
        String payNow,
        String mockConfirmNote,
        String viewOrder,
        String paypalTitle,
        String paypalIntro,
        String paypalMissingToken,
        // SEO 描述模板 (MessageFormat {0})
        String seoHome,
        String seoAllProducts,
        String seoCategory,
        String seoSearch,
        String seoCart,
        String seoBlog,
        String seoContact,
        String seoOrder,
        String seoPay,
        String seoPaypal) {

    /** 从语言覆盖包构造: key 名 = 组件名, 缺 key 传 null → 紧凑构造器落英文默认 (顺序与上方组件声明一一对应) */
    public static UiText from(java.util.Properties p) {
        return new UiText(
                p.getProperty("htmlLang"),
                p.getProperty("homeTitle"),
                p.getProperty("poweredBy"),
                p.getProperty("navHome"),
                p.getProperty("navCategory"),
                p.getProperty("navHot"),
                p.getProperty("navBlog"),
                p.getProperty("navContact"),
                p.getProperty("ariaSearch"),
                p.getProperty("ariaCart"),
                p.getProperty("footBlurb"),
                p.getProperty("ariaEmail"),
                p.getProperty("ariaSubscribe"),
                p.getProperty("footOurShop"),
                p.getProperty("footAllProducts"),
                p.getProperty("notFoundTitle"),
                p.getProperty("notFoundBody"),
                p.getProperty("notFoundBackHome"),
                p.getProperty("footBlog"),
                p.getProperty("footContact"),
                p.getProperty("footPrivacy"),
                p.getProperty("footRefund"),
                p.getProperty("footTerms"),
                p.getProperty("footQuickLinks"),
                p.getProperty("footSearch"),
                p.getProperty("footCart"),
                p.getProperty("footShipping"),
                p.getProperty("footRefunds"),
                p.getProperty("footBrandTagline"),
                p.getProperty("footNewArrivals"),
                p.getProperty("welcomeTitle"),
                p.getProperty("noProductsYet"),
                p.getProperty("featuredTitle"),
                p.getProperty("allProducts"),
                p.getProperty("newArrivals"),
                p.getProperty("from"),
                p.getProperty("crumbHome"),
                p.getProperty("crumbProducts"),
                p.getProperty("filterAll"),
                p.getProperty("searchPlaceholder"),
                p.getProperty("nothingHere"),
                p.getProperty("soldOut"),
                p.getProperty("lowStock"),
                p.getProperty("stockLabel"),
                p.getProperty("left"),
                p.getProperty("accShipTitle"),
                p.getProperty("accShipBody"),
                p.getProperty("accSupportTitle"),
                p.getProperty("accSupportBodyPre"),
                p.getProperty("contactLink"),
                p.getProperty("accBenefitsTitle"),
                p.getProperty("accBenefitsBody"),
                p.getProperty("unavailable"),
                p.getProperty("optionLabel"),
                p.getProperty("standard"),
                p.getProperty("quantity"),
                p.getProperty("addToCart"),
                p.getProperty("tabDescription"),
                p.getProperty("reviewsTitle"),
                p.getProperty("reviewsEmpty"),
                p.getProperty("writeReview"),
                p.getProperty("faqTitle"),
                p.getProperty("faqShipQ"),
                p.getProperty("faqShipA"),
                p.getProperty("faqUniqueQ"),
                p.getProperty("faqUniqueA"),
                p.getProperty("faqCareQ"),
                p.getProperty("faqCareA"),
                p.getProperty("faqReturnQ"),
                p.getProperty("faqReturnAPre"),
                p.getProperty("refundLink"),
                p.getProperty("faqReturnAPost"),
                p.getProperty("youMayLike"),
                p.getProperty("cartTitle"),
                p.getProperty("cartEmptyTitle"),
                p.getProperty("cartEmptyBody"),
                p.getProperty("continueShopping"),
                p.getProperty("crossSell"),
                p.getProperty("yourCart"),
                p.getProperty("thItem"),
                p.getProperty("thPrice"),
                p.getProperty("thQty"),
                p.getProperty("thSubtotal"),
                p.getProperty("update"),
                p.getProperty("outOfStock"),
                p.getProperty("remove"),
                p.getProperty("subtotalLabel"),
                p.getProperty("checkout"),
                p.getProperty("emailLabel"),
                p.getProperty("nameLabel"),
                p.getProperty("countryLabel"),
                p.getProperty("countryUS"),
                p.getProperty("countryCA"),
                p.getProperty("countryGB"),
                p.getProperty("countryDE"),
                p.getProperty("countryAU"),
                p.getProperty("continueToPayment"),
                p.getProperty("holdNote"),
                p.getProperty("contactTitle"),
                p.getProperty("contactIntro"),
                p.getProperty("shopNow"),
                p.getProperty("sentOk"),
                p.getProperty("phName"),
                p.getProperty("phEmail"),
                p.getProperty("phComment"),
                p.getProperty("send"),
                p.getProperty("contactErrInvalid"),
                p.getProperty("contactErrFailed"),
                p.getProperty("journal"),
                p.getProperty("noPosts"),
                p.getProperty("prevPost"),
                p.getProperty("nextPost"),
                p.getProperty("backToBlog"),
                p.getProperty("orderWord"),
                p.getProperty("statusLabel"),
                p.getProperty("paidOk"),
                p.getProperty("completePayment"),
                p.getProperty("thUnitPrice"),
                p.getProperty("thTotal"),
                p.getProperty("placedAt"),
                p.getProperty("paidAt"),
                p.getProperty("fulfilledAt"),
                p.getProperty("cancelledAt"),
                p.getProperty("refundedAt"),
                p.getProperty("totalLabel"),
                p.getProperty("itemsWord"),
                p.getProperty("shippingWord"),
                p.getProperty("shipment"),
                p.getProperty("carrier"),
                p.getProperty("tracking"),
                p.getProperty("linkLabel"),
                p.getProperty("trackPackage"),
                p.getProperty("mockTitle"),
                p.getProperty("mockIntro"),
                p.getProperty("thOrder"),
                p.getProperty("thAmount"),
                p.getProperty("thStatus"),
                p.getProperty("payNow"),
                p.getProperty("mockConfirmNote"),
                p.getProperty("viewOrder"),
                p.getProperty("paypalTitle"),
                p.getProperty("paypalIntro"),
                p.getProperty("paypalMissingToken"),
                p.getProperty("seoHome"),
                p.getProperty("seoAllProducts"),
                p.getProperty("seoCategory"),
                p.getProperty("seoSearch"),
                p.getProperty("seoCart"),
                p.getProperty("seoBlog"),
                p.getProperty("seoContact"),
                p.getProperty("seoOrder"),
                p.getProperty("seoPay"),
                p.getProperty("seoPaypal"));
    }

    /** null → 英文默认 (bundle 缺 key / 未翻译字段的兜底) */
    public UiText {
        htmlLang = or(htmlLang, "en");
        homeTitle = or(homeTitle, "Home");
        poweredBy = or(poweredBy, "Powered by Jade Cart");
        navHome = or(navHome, "Home");
        navCategory = or(navCategory, "Category");
        navHot = or(navHot, "HOT");
        navBlog = or(navBlog, "Blog");
        navContact = or(navContact, "Contact");
        ariaSearch = or(ariaSearch, "Search");
        ariaCart = or(ariaCart, "Cart");
        footBlurb =
                or(
                        footBlurb,
                        "Wood-fired teaware and curated tea, shaped by flame and time"
                                + " — delivered from our kiln workshop to your table.");
        ariaEmail = or(ariaEmail, "Email");
        ariaSubscribe = or(ariaSubscribe, "Subscribe");
        footOurShop = or(footOurShop, "Our Shop");
        footAllProducts = or(footAllProducts, "All Products");
        notFoundTitle = or(notFoundTitle, "Page not found");
        notFoundBody =
                or(notFoundBody, "The page you are looking for may have been moved or removed.");
        notFoundBackHome = or(notFoundBackHome, "Back to home");
        footBlog = or(footBlog, "Blog");
        footContact = or(footContact, "Contact");
        footPrivacy = or(footPrivacy, "Privacy policy");
        footRefund = or(footRefund, "Refund policy");
        footTerms = or(footTerms, "Terms & Condition");
        footQuickLinks = or(footQuickLinks, "Quick links");
        footSearch = or(footSearch, "Search");
        footCart = or(footCart, "Cart");
        footShipping = or(footShipping, "Shipping");
        footRefunds = or(footRefunds, "Refunds");
        footBrandTagline = or(footBrandTagline, "Curated pieces · Worldwide shipping");
        footNewArrivals = or(footNewArrivals, "New Arrivals ➜");
        welcomeTitle = or(welcomeTitle, "Welcome");
        noProductsYet = or(noProductsYet, "No products yet. Admin console:");
        featuredTitle = or(featuredTitle, "Featured");
        allProducts = or(allProducts, "All Products");
        newArrivals = or(newArrivals, "New Arrivals");
        from = or(from, "from");
        crumbHome = or(crumbHome, "Home");
        crumbProducts = or(crumbProducts, "Products");
        filterAll = or(filterAll, "All");
        searchPlaceholder = or(searchPlaceholder, "Search…");
        nothingHere = or(nothingHere, "Nothing here yet.");
        soldOut = or(soldOut, "SOLD OUT");
        lowStock = or(lowStock, "Low stock:");
        stockLabel = or(stockLabel, "Stock");
        left = or(left, "left");
        accShipTitle = or(accShipTitle, "Shipping & Returns");
        accShipBody =
                or(
                        accShipBody,
                        "Orders ship within 2-3 business days. Unopened items can be refunded"
                                + " within 30 days of delivery.");
        accSupportTitle = or(accSupportTitle, "24/7 Support");
        accSupportBodyPre =
                or(accSupportBodyPre, "Questions about this piece? Reach us any time via the");
        contactLink = or(contactLink, "contact form");
        accBenefitsTitle = or(accBenefitsTitle, "Main Benefits");
        accBenefitsBody =
                or(
                        accBenefitsBody,
                        "Handcrafted by artisans · One of a kind wood-fired finish · Secure"
                                + " checkout with Stripe / PayPal.");
        unavailable = or(unavailable, "Currently unavailable.");
        optionLabel = or(optionLabel, "Option");
        standard = or(standard, "Standard");
        quantity = or(quantity, "Quantity");
        addToCart = or(addToCart, "Add to cart");
        tabDescription = or(tabDescription, "Product description");
        reviewsTitle = or(reviewsTitle, "Customer Reviews");
        reviewsEmpty = or(reviewsEmpty, "Be the first to write a review");
        writeReview = or(writeReview, "Write a review");
        faqTitle = or(faqTitle, "FAQ");
        faqShipQ = or(faqShipQ, "How long does shipping take?");
        faqShipA =
                or(
                        faqShipA,
                        "Orders ship within 2-3 business days. Delivery typically takes 7-14"
                                + " business days depending on your region.");
        faqUniqueQ = or(faqUniqueQ, "Is each piece really one of a kind?");
        faqUniqueA =
                or(
                        faqUniqueA,
                        "Yes — every item is shaped by hand and finished in a wood kiln, so glaze"
                                + " patterns and textures vary piece to piece.");
        faqCareQ = or(faqCareQ, "How do I care for wood-fired ware?");
        faqCareA =
                or(
                        faqCareA,
                        "Hand wash with warm water and mild detergent. Avoid dishwasher and"
                                + " microwave to preserve the natural ash glaze.");
        faqReturnQ = or(faqReturnQ, "What is your return policy?");
        faqReturnAPre =
                or(
                        faqReturnAPre,
                        "Unopened items can be refunded within 30 days of delivery. See our");
        refundLink = or(refundLink, "Refund Policy");
        faqReturnAPost = or(faqReturnAPost, ".");
        youMayLike = or(youMayLike, "You may also like");
        cartTitle = or(cartTitle, "Cart");
        cartEmptyTitle = or(cartEmptyTitle, "Your cart is empty");
        cartEmptyBody =
                or(
                        cartEmptyBody,
                        "Why not explore more and find something special? We're sure you'll"
                                + " discover something you'll love!");
        continueShopping = or(continueShopping, "Continue shopping");
        crossSell = or(crossSell, "Cross-Sell");
        yourCart = or(yourCart, "Your Cart");
        thItem = or(thItem, "Item");
        thPrice = or(thPrice, "Price");
        thQty = or(thQty, "Qty");
        thSubtotal = or(thSubtotal, "Subtotal");
        update = or(update, "Update");
        outOfStock = or(outOfStock, "out of stock");
        remove = or(remove, "Remove");
        subtotalLabel = or(subtotalLabel, "Subtotal:");
        checkout = or(checkout, "Checkout");
        emailLabel = or(emailLabel, "Email");
        nameLabel = or(nameLabel, "Name");
        countryLabel = or(countryLabel, "Country");
        countryUS = or(countryUS, "United States");
        countryCA = or(countryCA, "Canada");
        countryGB = or(countryGB, "United Kingdom");
        countryDE = or(countryDE, "Germany");
        countryAU = or(countryAU, "Australia");
        continueToPayment = or(continueToPayment, "Continue to payment");
        holdNote = or(holdNote, "Unpaid orders hold stock for 30 minutes.");
        contactTitle = or(contactTitle, "Contact Us");
        contactIntro =
                or(
                        contactIntro,
                        "We'd be delighted to connect with you! Our team is ready to assist. Reach"
                                + " out by filling in the email form below.");
        shopNow = or(shopNow, "Shop now");
        sentOk =
                or(
                        sentOk,
                        "✓ Thanks! Your message has been received. We reply within one business"
                                + " day.");
        phName = or(phName, "Name");
        phEmail = or(phEmail, "Email *");
        phComment = or(phComment, "Comment");
        send = or(send, "Send ➤");
        contactErrInvalid = or(contactErrInvalid, "Please provide a valid email and message.");
        contactErrFailed = or(contactErrFailed, "Failed to send, please try later.");
        journal = or(journal, "Journal");
        noPosts = or(noPosts, "No posts yet.");
        prevPost = or(prevPost, "Previous post");
        nextPost = or(nextPost, "Next post");
        backToBlog = or(backToBlog, "Back to blog");
        orderWord = or(orderWord, "Order");
        statusLabel = or(statusLabel, "Status:");
        paidOk = or(paidOk, "✓ Paid");
        completePayment = or(completePayment, "Complete payment");
        thUnitPrice = or(thUnitPrice, "Unit price");
        thTotal = or(thTotal, "Total");
        placedAt = or(placedAt, "Placed at");
        paidAt = or(paidAt, "Paid at");
        fulfilledAt = or(fulfilledAt, "Fulfilled at");
        cancelledAt = or(cancelledAt, "Cancelled at");
        refundedAt = or(refundedAt, "Refunded at");
        totalLabel = or(totalLabel, "Total:");
        itemsWord = or(itemsWord, "items");
        shippingWord = or(shippingWord, "shipping");
        shipment = or(shipment, "Shipment");
        carrier = or(carrier, "Carrier");
        tracking = or(tracking, "Tracking");
        linkLabel = or(linkLabel, "Link");
        trackPackage = or(trackPackage, "Track package");
        mockTitle = or(mockTitle, "Mock Payment Gateway");
        mockIntro =
                or(
                        mockIntro,
                        "This is a simulated cashier page (Stripe/PayPal style hosted checkout). No"
                                + " real money involved.");
        thOrder = or(thOrder, "Order");
        thAmount = or(thAmount, "Amount");
        thStatus = or(thStatus, "Status");
        payNow = or(payNow, "Pay");
        mockConfirmNote =
                or(
                        mockConfirmNote,
                        "Confirming simulates the gateway webhook callback (idempotent).");
        viewOrder = or(viewOrder, "View order status");
        paypalTitle = or(paypalTitle, "PayPal Checkout");
        paypalIntro =
                or(
                        paypalIntro,
                        "You returned from PayPal. Complete the payment to finalize your order.");
        paypalMissingToken =
                or(paypalMissingToken, "Missing PayPal order token — please restart checkout.");
        seoHome =
                or(
                        seoHome,
                        "Shop {0} — curated products, fast worldwide shipping and easy returns.");
        seoAllProducts = or(seoAllProducts, "All products at {0}. Curated quality, fast shipping.");
        seoCategory = or(seoCategory, "Browse {0} products at {1}. Worldwide shipping.");
        seoSearch = or(seoSearch, "Search results for \u201C{0}\u201D at {1}.");
        seoCart = or(seoCart, "Your shopping cart at {0}.");
        seoBlog = or(seoBlog, "Blog — news, guides and stories from {0}.");
        seoContact = or(seoContact, "Contact {0} — we reply within one business day.");
        seoOrder = or(seoOrder, "Order {0} status.");
        seoPay = or(seoPay, "Complete your payment.");
        seoPaypal = or(seoPaypal, "Complete your PayPal payment.");
    }

    private static String or(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

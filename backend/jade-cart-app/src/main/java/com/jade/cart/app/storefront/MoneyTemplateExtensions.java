package com.jade.cart.app.storefront;

import io.quarkus.qute.TemplateExtension;

/** 金额格式化: {line.unitPriceCents.money} → "$12.34" */
public final class MoneyTemplateExtensions {

    private MoneyTemplateExtensions() {}

    @TemplateExtension(matchName = "money")
    public static String money(Long cents) {
        if (cents == null) {
            return "";
        }
        long whole = cents / 100;
        long frac = Math.abs(cents % 100);
        return "$" + whole + "." + String.format("%02d", frac);
    }

    /** JSON-LD 等场景用: 1900 → "19.00" (无货币符号) */
    @TemplateExtension(matchName = "decimal")
    public static String decimal(Long cents) {
        if (cents == null) {
            return "0.00";
        }
        return (cents / 100) + "." + String.format("%02d", Math.abs(cents % 100));
    }
}

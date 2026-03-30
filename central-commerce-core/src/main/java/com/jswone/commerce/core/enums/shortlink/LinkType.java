package com.jswone.commerce.core.enums.shortlink;

public enum LinkType {

    LEDGER("ld"),
    INVOICE("iv"),
    SHIPMENT("sh"),
    ORDER("or"),
    DOWNLOAD("dl"),
    CUSTOM("ct");

    private final String prefix;

    LinkType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public static LinkType fromPrefix(String prefix) {
        for (LinkType type : LinkType.values()) {
            if (type.getPrefix().equals(prefix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown prefix: " + prefix);
    }

}

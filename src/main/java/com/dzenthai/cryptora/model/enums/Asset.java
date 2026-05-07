package com.dzenthai.cryptora.model.enums;

import java.util.List;
import java.util.stream.Stream;


public enum Asset {

    /**
     * Indicates the option to incorporate additional cryptocurrencies.
     */

    BTC,
    ETH,
    TON;

    public String getSymbol() {
        return this.name() + "USDT";
    }

    public static List<String> getAllSymbols() {
        return Stream.of(Asset.values())
                .map(Asset::getSymbol)
                .toList();
    }
}

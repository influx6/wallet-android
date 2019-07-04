package com.mycelium.wapi.wallet.bch.coins

import com.mycelium.wapi.wallet.coins.SoftDustPolicy

object BchTest: BchCoin() {

    init {
        id = "bitcoin_cash.test"
        name = "Bitcoin Cash Test"
        symbol = "BCH"
        unitExponent = 8
    }

    override fun getUnitExponent(): Int {
        return 0
    }
}
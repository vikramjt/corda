package com.template

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class InvoiceState (
        val buyer: Party,
        val seller: Party,
        val amount: Int,
        val status: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<AbstractParty> = listOf(buyer, seller)):LinearState{

}
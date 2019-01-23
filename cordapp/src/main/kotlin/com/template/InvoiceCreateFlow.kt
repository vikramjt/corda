package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class InvoiceCreateFlow (val buyer: Party,
                         val amount: Int,
                         val status: String) :FlowLogic<SignedTransaction>(){

    override val progressTracker : ProgressTracker = ProgressTracker();

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We get a reference to our own identity.
        val seller = ourIdentity;

        val outputInvoiceState =InvoiceState(buyer, seller, amount, status);

        // We build our transaction.
        val transactionBuilder = TransactionBuilder()
        transactionBuilder.notary = notary;

        transactionBuilder.addOutputState(outputInvoiceState, InvoiceContract.ID)
        transactionBuilder.addCommand(InvoiceContract.Commands.Create(), seller.owningKey)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our private key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(signedTransaction))
    }
}
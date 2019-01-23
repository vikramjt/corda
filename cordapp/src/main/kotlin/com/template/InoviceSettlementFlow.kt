package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.Id
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class InvoiceSettlementFlow (
        val linearIdentifier: UniqueIdentifier): FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = ProgressTracker();

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities[0];

        //Query invoices state by linear Id
        val valueQueryCriteria = QueryCriteria.LinearStateQueryCriteria(listOf(ourIdentity), listOf(linearIdentifier));
        val inputState = serviceHub.vaultService.queryBy<InvoiceState>(valueQueryCriteria).states.first();

        val outputState = inputState.state.data.copy(status = "SETTLED");

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(inputState)
                .addOutputState(outputState, InvoiceContract.ID)
                .addCommand(InvoiceContract.Commands.Settle(), ourIdentity.owningKey, outputState.seller.owningKey)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        //sign the transaction
        val partiallySignedTrasaction = serviceHub.signInitialTransaction(transactionBuilder);

        //send transaction to the seller node for signing
        val otherPartySession = initiateFlow(outputState.seller);
        val completeTx = subFlow(CollectSignaturesFlow(partiallySignedTrasaction, listOf(otherPartySession)))

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(completeTx))

    }
}

@InitiatedBy(InvoiceSettlementFlow::class)
class InvoiceSettlementResponderFlow(val otherpartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val flow = object : SignTransactionFlow(otherpartySession){
            override fun checkTransaction(stx: SignedTransaction) {
                 // sanity checks on this transaction
            }
        }
        subFlow(flow)
    }
}
package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException


class InvoiceContract : Contract{

    companion object {
        //var ID="com.template.InvoiceContract";
        var ID = InvoiceContract:: class.qualifiedName!!
    }

    interface Commands : CommandData {
        class Create: Commands
        class Settle: Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val inputs = tx.inputsOfType<InvoiceState>()
        val outputs = tx.outputsOfType<InvoiceState>()
        when(command.value) {
            is Commands.Create -> {
                requireThat {
                    "Number of inputs to transaction is 0" using (inputs.isEmpty())
                    "Number of output to transaction is 1 " using (outputs.size == 1)

                    "Amount should be positive " using (outputs[0].amount > 0)

                    "Check that invoice should only signed by seller" using
                            (command.signers.contains(outputs[0].seller.owningKey))

                    "Check that invoice status is Created" using (outputs[0].status.equals("CREATED"));
                }
            }
            is Commands.Settle -> {
                requireThat {
                    "Number of inputs to transaction is 1" using (inputs.size == 1)
                    "Number of output to transaction is 1 " using (outputs.size == 1)

                    "Check that invoice status is Settled" using (inputs[0].status.equals("SETTLED"));

                    "Output should be of type InvoiceState" using (tx.outputStates.first() is InvoiceState)

                    "Transaction should be signed by both seller and buyer" using (
                            command.signers.containsAll(listOf(outputs[0].seller.owningKey, outputs[0].buyer.owningKey)))
                }
            }
            else -> throw IllegalArgumentException();
        }

    }
}
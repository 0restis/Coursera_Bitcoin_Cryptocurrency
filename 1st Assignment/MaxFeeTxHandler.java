import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MaxFeeTxHandler {

    UTXOPool scrgLedger = new UTXOPool();

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        scrgLedger = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double txInputSum = 0;
        double txOutputSum = 0;
        UTXOPool doubleSpend = new UTXOPool();

        for (int i = 0; i < tx.numInputs(); i++) {    
            Transaction.Input in = tx.getInput(i);
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            // currently doesn't take into account internal claims 
            if (!scrgLedger.contains(ut)) return false;
            Transaction.Output out = scrgLedger.getTxOutput(ut);
            if (!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), in.signature)) return false;
            if (doubleSpend.contains(ut)) return false;
            doubleSpend.addUTXO(ut,out);
            txInputSum += out.value;
        }
        for (int i = 0; i < tx.numOutputs(); i++) {
            if (tx.getOutput(i).value < 0) return false;
            txOutputSum += tx.getOutput(i).value;
        }
        return txInputSum >= txOutputSum;
    }

    /**
    * Creates a new class to store each transaction along with its fee.
    * Will then be used for sorting based on Tx fee.
    */
    public class TxWithFee implements Comparable<TxWithFee>{
        Transaction tx;
        double fee;

        public TxWithFee(Transaction tx) {
            this.tx = tx;
            this.fee = txFee(tx);
        }

        public int compareTo(TxWithFee tx)
        {
            if (fee > tx.fee) return -1;
            if (fee < tx.fee) return 1;
            return 0;
        }
    }

    /**
     * Calculates transaction fee for a given transaction i.e. (sum of input values - sum of output values)
     */
    public double txFee(Transaction tx) {
        double txInputSum = 0;
        double txOutputSum = 0;
        
        for (Transaction.Input in : tx.getInputs()) {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = scrgLedger.getTxOutput(ut);
            txInputSum += out.value;
        }
        for (Transaction.Output out : tx.getOutputs()) {
            txOutputSum += out.value;
        }
        return txInputSum - txOutputSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions with
     * maximum total transaction fees, i.e. max{sum of input values - sum of output values} and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        ArrayList<TxWithFee> txWithFee = new ArrayList<TxWithFee>();
        ArrayList<Transaction> txMaxFeeBlockList = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) { 
                txWithFee.add(new TxWithFee(tx));
            }
        }
        Collections.sort(txWithFee);
        for (TxWithFee entry : txWithFee) {
            if (isValidTx(entry.tx)) {
                for (Transaction.Input in : entry.tx.getInputs()) {
                        UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
                        scrgLedger.removeUTXO(ut);
                }
                for (int i = 0; i < entry.tx.numOutputs(); i++) {    
                        UTXO ut = new UTXO(entry.tx.getHash(), i);
                        scrgLedger.addUTXO(ut,entry.tx.getOutput(i));        
                }       
                txMaxFeeBlockList.add(entry.tx);   
            }             
        }
        Transaction txBlock[] = new Transaction[txMaxFeeBlockList.size()];
        return txMaxFeeBlockList.toArray(txBlock);
    }
}

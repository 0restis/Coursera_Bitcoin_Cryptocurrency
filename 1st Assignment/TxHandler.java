import java.util.ArrayList;
import java.util.Arrays;


public class TxHandler {

    UTXOPool scrgLedger = new UTXOPool();

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        
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
        // IMPLEMENT THIS

        double txInputSum = 0;
        double txOutputSum = 0;
        UTXOPool doubleSpend = new UTXOPool();

        for (int i = 0; i < tx.numInputs(); i++) {
        
            Transaction.Input in = tx.getInput(i);
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
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
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        ArrayList<Transaction> txBlockList = new ArrayList<Transaction>();
        
        for (Transaction tx : possibleTxs){

            if (isValidTx(tx)) {

                for (Transaction.Input in : tx.getInputs()) {
                
                    UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
                    scrgLedger.removeUTXO(ut);
                
                }
                
                for (int i = 0; i < tx.numOutputs(); i++) {
                
                    UTXO ut = new UTXO(tx.getHash(), i);
                    scrgLedger.addUTXO(ut,tx.getOutput(i));
                
                } 
                
                txBlockList.add(tx);
            
            }
        
        }

        Transaction txBlock[] = new Transaction[txBlockList.size()];
        return txBlockList.toArray(txBlock);
    }
}

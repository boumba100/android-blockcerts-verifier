package com.nbouma.blockcerts;



import com.nbouma.jsonldjava.core.NormalizeUtils;
import com.nbouma.jsonldjava.utils.JsonNormalizer;
import com.nbouma.jsonldjava.utils.JsonUtils;
import com.nbouma.blockcerts.blockchain.BlockChain;
import com.nbouma.blockcerts.blockchain.BlockChainRequest;
import com.nbouma.blockcerts.model.Certificate;
import com.nbouma.blockcerts.model.Transaction;

import com.nbouma.blockcerts.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by noah on 01/05/17.
 */

abstract class CertificateVerifier {
    private String netWork = BlockChainRequest.BTC;
    private JSONObject certificate;
    private JSONObject signature;
    private JSONObject issuerInfo;
    private JSONObject transactionInfo;
    private JSONObject revocationList;

    public CertificateVerifier(JSONObject certificate) {
        this.certificate = certificate;
    }


    public abstract void onReceiptValidation(boolean result);

    public abstract void onCertHashValidation(boolean result);

    public abstract void onMerkleRootValidation(boolean result);

    public abstract void certificateAuthentic(boolean result);

    public abstract void notRevoked(boolean result);

    public abstract void onError(String errorMessage);

    public void verify() {

        /*
        * Get the transaction id from the certificate
        */
        String transactionId = null;
        String issuerInfoLink = null;
        String revocationListLink = null;
        try {
            transactionId = this.certificate.getJSONObject(Certificate.SIGNATURE)
                    .getJSONArray(Certificate.ANCHORS)
                    .getJSONObject(0)
                    .getString(Certificate.SOURCE_ID);
            signature = this.certificate.getJSONObject(Certificate.SIGNATURE);
            issuerInfoLink = this.certificate.getJSONObject(Certificate.BADGE).getJSONObject(Certificate.ISSUER).getString("id");
            revocationListLink = this.certificate.getJSONObject(Certificate.BADGE)
                    .getJSONObject(Certificate.ISSUER).getString(Certificate.REVOCATION_LIST);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        final String finalIssuerInfoLink = issuerInfoLink;
        final String finalRevocationListLink = revocationListLink;

        BlockChain blockChain = new BlockChain() {
            @Override
            public void onInfoReceived(String name, JSONObject info) { // Get the needed information from the blockChain
                if (info == null) {
                    onError("was not able to fetch " + name);
                } else if (name.equals(Certificate.TRANSACTION)) {
                    transactionInfo = info;
                    fetchIssuerInfo(finalIssuerInfoLink);
                } else if (name.equals(Certificate.ISSUER)) {
                    issuerInfo = info;
                    fetchRevocationInfo(finalRevocationListLink);
                } else if (name.equals(Certificate.REVOCATION_LIST)) {
                    revocationList = info;
                }
                if (issuerInfo != null && transactionInfo != null && revocationList != null) {
                    /*
                     * Validate the certificate integrity
                     */
                    try {
                        validateReceipt();
                        validateCertificateHash();
                        validateMerkleRoot();
                        checkNotRevoked();

                        /*
                        * Validate the certificate authenticity
                        */
                        verifyAuthenticity();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        blockChain.setNetwork(this.netWork);
        blockChain.getTransactionInfo(transactionId);

    }


    public void setTestNet() {
        this.netWork = BlockChainRequest.TEST_NET;
    }

    /*
    * Methods to verify the certificates integrity
    */

    /*
    * 1. Validate the Merkle proof in the certificate.
    */
    private void validateReceipt() throws JSONException {
        JSONObject signature = this.certificate.getJSONObject(Certificate.SIGNATURE);

        JSONArray proof = signature.getJSONArray(Certificate.PROOF);
        byte[] targetHash = Utils.hexStringToBytes(signature.getString(Certificate.TARGET_HASH));
        byte[] merkleRoot = Utils.hexStringToBytes(signature.getString(Certificate.MERKLE_ROOT));
        byte[] proofHash = null;

        if (proof == null || proof.length() == 0) {
            this.onReceiptValidation(targetHash == merkleRoot);
        } else {
            JSONObject jsonObject = proof.getJSONObject(0);
            if (jsonObject.has(Certificate.RIGHT_NODE)) {
                proofHash = NormalizeUtils.sha256Raw(Utils.combineBytes(targetHash, Utils.hexStringToBytes(jsonObject.getString(Certificate.RIGHT_NODE))));
            } else if (jsonObject.has(Certificate.LEFT_NODE)) {
                proofHash = NormalizeUtils.sha256Raw(Utils.combineBytes(Utils.hexStringToBytes(jsonObject.getString(Certificate.LEFT_NODE)), targetHash));
            } else {
                this.onReceiptValidation(false);
            }
        }

        this.onReceiptValidation(Arrays.equals(merkleRoot, proofHash));
    }

    /*
    * 2. Compare the hash of the local certificate with the value in the receipt.
    * Note
    */
    private void validateCertificateHash() { // Compare the hash of the local certificate with the value in the receipt
        try {
            JSONObject jsonObject = new JSONObject(this.certificate.toString());
            jsonObject.remove(Certificate.SIGNATURE);
            Object document = JsonUtils.fromString(jsonObject.toString());

            new JsonNormalizer(document, new JsonNormalizer.OnNormalizedCompleted() {
                @Override
                public void OnNormalizedComplete(Object object) {
                    String normalized = (String) object;
                    String localHash = NormalizeUtils.sha256Hash(normalized);
                    try {
                        String expectedHash = signature.getString(Certificate.TARGET_HASH);
                        onCertHashValidation(localHash.equals(expectedHash));
                    } catch (JSONException e) {
                        onCertHashValidation(false);
                        e.printStackTrace();
                    }

                }
            }).execute();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            this.onCertHashValidation(false);
        }
    }


    /*
    * 3. Compare the merkleRoot value in the certificate with the value in the blockchain transaction.
    */
    private void validateMerkleRoot() {
        try {
            String merkleRoot = this.signature.getString(Certificate.MERKLE_ROOT);
            JSONArray txOuts = this.transactionInfo.getJSONArray(Transaction.OUTPUTS);
            JSONObject opReturnTx = txOuts.getJSONObject(txOuts.length() - 1);
            String opField = opReturnTx.getString(Transaction.SCRIPT);

            byte[] opReturn = Utils.hexStringToBytes(opField.substring(2));
            String hashFromChain = NormalizeUtils.encodeHex(opReturn).substring(2);

            this.onMerkleRootValidation(hashFromChain.equals(merkleRoot));
        } catch (JSONException e) {
            this.onMerkleRootValidation(false);
            e.printStackTrace();
        }
    }

    /*
    * Methods used to verify the certificates authenticity
    */
    private void verifyAuthenticity() { // This is designed for blockchain.info
        try {
            JSONArray inputs = transactionInfo.getJSONArray(Transaction.INPUTS);
            JSONObject prevOut = inputs.getJSONObject(0);

            String issuerAddress = Certificate.PUBLIC_KEY_IDENTIFIER + prevOut.getString(Transaction.ADDRESS);
            JSONObject issuerKeyInfo = this.getIssuerKeyInfo(issuerAddress);

            long transactionTime = Utils.ISO8601ToTime(transactionInfo.getString("block_time")); // time in Unix epoch time format
            long issuerKeyCreationTime = Utils.ISO8601ToTime(issuerKeyInfo.getString("created"));


            boolean validDate;

            // Check if the transaction timeStamp is valid

            if (issuerKeyInfo.has("revoked")) {
                long revokedDate = Utils.ISO8601ToTime(issuerKeyInfo.getString("revoked"));
                if (issuerKeyInfo.has("expires")) {
                    long expiryTime = Utils.ISO8601ToTime(issuerKeyInfo.getString("expires"));
                    validDate = transactionTime >= issuerKeyCreationTime && transactionTime <= revokedDate && transactionTime <= expiryTime;
                } else {
                    validDate = transactionTime >= issuerKeyCreationTime && transactionTime <= revokedDate;
                }
            } else if (issuerKeyInfo.has("expires")) {
                long expiryTime = Utils.ISO8601ToTime(issuerKeyInfo.getString("expires"));
                validDate = transactionTime >= issuerKeyCreationTime && transactionTime < expiryTime;
            } else {
                validDate = transactionTime >= issuerKeyCreationTime;
            }
            this.certificateAuthentic(validDate);


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /*
    / Verify if the certificate has been revoked
    * Current revocation method uses crl
    * NOTE : need to implement blockChain transaction revocation method
    */
    private void checkNotRevoked() {
        try {
            String certId = this.certificate.getString(Certificate.CERT_ID);
            JSONArray revocationAssertions = this.revocationList.getJSONArray("revokedAssertions");
            for(int i = 0; i < revocationAssertions.length(); i ++) {
                JSONObject revocation = revocationAssertions.getJSONObject(i);
                if(revocation.getString("id").equals(certId)) {
                    this.notRevoked(false);
                    return;
                }
            }
            this.notRevoked(true);
        } catch (JSONException e) {
            this.notRevoked(false);
            e.printStackTrace();
        }
    }

    private JSONObject getIssuerKeyInfo(String issuerAddress) {
        JSONArray issuerKeys;
        try {
            issuerKeys = this.issuerInfo.getJSONArray("publicKeys");
            for (int i = 0; i < issuerKeys.length(); i++) {
                JSONObject issuerKeyInfo = issuerKeys.getJSONObject(i);
                if (issuerKeyInfo.getString("publicKey").equals(issuerAddress)) {
                    return issuerKeyInfo;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}

















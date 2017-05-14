package com.nbouma.blockcerts;

/**
 * Created by noah on 05/04/17.
 */



import android.util.Log;

import com.nbouma.blockcerts.blockchain.BlockChainRequest;

import org.json.JSONObject;


/*
* Class to verify blockCert certificate
*/
public abstract class Verifier {

    private String network = BlockChainRequest.BTC;
    private int verificationCount = 0;
    private String message = "";


    public Verifier() {

    }

    public abstract void onResult(boolean result, String message);

    public void setTestNet() {
        this.network = BlockChainRequest.TEST_NET;
    }

    public void verify(JSONObject certificate) {
        CertificateVerifier verifier = new CertificateVerifier(certificate) {
            @Override
            public void onReceiptValidation(boolean result) {
                if (result) {
                    addVerificationStep();
                } else {
                    addVerificationStep("receipt not valid");
                }
            }

            @Override
            public void onCertHashValidation(boolean result) {
                if (result) {
                    addVerificationStep();
                } else {
                    addVerificationStep("certificate hash not valid");
                }
            }

            @Override
            public void onMerkleRootValidation(boolean result) {
                if (result) {
                    addVerificationStep();
                } else {
                    addVerificationStep("merkle root not valid");
                }
            }

            @Override
            public void certificateAuthentic(boolean result) {
                if (result) {
                    addVerificationStep();
                } else {
                    addVerificationStep("certificate is not authentic");
                }
            }

            @Override
            public void notRevoked(boolean result) {
                if (result) {
                    addVerificationStep();
                } else {
                    addVerificationStep("certificate revoked");
                }
            }

            @Override
            public void onError(String errorMessage) {
                onResult(false, errorMessage);
            }
        };
        if(this.network.equals(BlockChainRequest.TEST_NET)) {
            verifier.setTestNet();
        }
        verifier.verify();

    }

    private void addVerificationStep() {
        this.addVerificationStep("");
    }

    private void addVerificationStep(String message) {
        this.verificationCount += 1;
        if (message.length() > 0) {
            this.message += message + " \n ";

        }
        if (this.verificationCount == 5) {
            if (this.message.length() == 0) {
                this.verificationCount = 0;
                this.onResult(true, "The certificate is valid");
            } else {
                this.onResult(false, this.message);
                this.verificationCount = 0;
                this.message = "";
            }
        }
    }

}







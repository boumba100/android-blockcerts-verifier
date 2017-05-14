package com.nbouma.blockcerts.blockchain;


import com.nbouma.blockcerts.Network.HttpGet;
import com.nbouma.blockcerts.model.Certificate;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by noah on 02/05/17.
 */

/* TestNet
* API KEY : 2a527589de4bd917bc09c540974e0e9312bdbf40
* API SECRET : dd6314f0990b31cc172cd6dbe94720a08576e1e7
*/
public abstract class BlockChain {
    private String network = "BTC"; // default network is the bitcoin network. Other option is TBTC for the blockchain test net

    public abstract void onInfoReceived(String name, JSONObject info);

    public void getTransactionInfo(String transactionId) {
        BlockChainRequest blockChainrequest = new BlockChainRequest();
        if(this.network.equals(BlockChainRequest.TEST_NET)) {
            blockChainrequest.setTestNet();
        }
        String transactionRequest = blockChainrequest.transactionInfoRequest(transactionId);

        new HttpGet(transactionRequest, new HttpGet.OnComplete() {
            @Override
            public void OnComplete(String data) {
                if (data != null) {
                    try {
                        JSONObject txInfo = new JSONObject(data);
                        onInfoReceived(Certificate.TRANSACTION, txInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        onInfoReceived(Certificate.TRANSACTION, null);
                    }
                } else {
                    onInfoReceived(Certificate.TRANSACTION, null);
                }
            }
        }).execute();
    }

    public void fetchIssuerInfo(String issuerInfoLink) {
        new HttpGet(issuerInfoLink, new HttpGet.OnComplete() {
            @Override
            public void OnComplete(String data) {
                if(data != null) {
                    try {
                        onInfoReceived(Certificate.ISSUER, new JSONObject(data));
                    } catch (JSONException e) {
                        onInfoReceived(Certificate.ISSUER, null);
                        e.printStackTrace();
                    }
                } else {
                    onInfoReceived(Certificate.ISSUER, null);
                }
            }
        }).execute();
    }

    public void fetchRevocationInfo(String revocationListLink) {
        new HttpGet(revocationListLink, new HttpGet.OnComplete() {
            @Override
            public void OnComplete(String data) {
                if(data != null) {
                    try {
                        JSONObject revocationList = new JSONObject(data);
                        onInfoReceived(Certificate.REVOCATION_LIST, revocationList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        onInfoReceived(Certificate.REVOCATION_LIST, null);
                    }
                } else {
                    onInfoReceived(Certificate.REVOCATION_LIST, null);
                }
            }
        }).execute();
    }

    public void setNetwork(String network) {
        this.network = network;
    }
}

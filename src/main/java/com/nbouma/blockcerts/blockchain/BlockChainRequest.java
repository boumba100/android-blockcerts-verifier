package com.nbouma.blockcerts.blockchain;


/**
 * Created by noah on 02/05/17.
 */


/*
* Class used to build urls to query from the blockchain
* Note : This library uses the blocktrail because it allows access to the testnet
*/
public class BlockChainRequest {


    /*
    * this library currently uses blocktrail
    */

    public static final String BTC = "BTC";
    public static final String TEST_NET = "TBTC";
    private static final String BLOCKCHAIN_DESTINATION = "https://api.blocktrail.com/v1/";
    private static final String API_KEY_PARAM = "?api_key=";
    private static final String API_KEY = "2a527589de4bd917bc09c540974e0e9312bdbf40";
    private static final String TRANSACTION = "transaction";

    private String blockChainNet = BTC; // blockChain network to be used


    private static String TEST_NET_DESTINATION = "";


    public String transactionInfoRequest(String transactionId) {
        StringBuilder builder = new StringBuilder();
        builder.append(BLOCKCHAIN_DESTINATION)
                .append(blockChainNet)
                .append("/")
                .append(TRANSACTION)
                .append("/")
                .append(transactionId)
                .append(API_KEY_PARAM)
                .append(API_KEY);

        return builder.toString();
    }

    public void setTestNet() {
        this.blockChainNet = TEST_NET;
    }

}

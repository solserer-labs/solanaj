package org.p2p.solanaj.core;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Utils;
import org.junit.Ignore;
import org.junit.Test;
import org.p2p.solanaj.programs.MemoProgram;
import org.p2p.solanaj.programs.SystemProgram;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.*;
import org.p2p.solanaj.serum.*;
import org.p2p.solanaj.token.TokenManager;
import org.p2p.solanaj.utils.ByteUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class MainnetTest {

    private static final Logger LOGGER = Logger.getLogger(MainnetTest.class.getName());
    private final RpcClient client = new RpcClient(Cluster.MAINNET);
    private final PublicKey publicKey = new PublicKey("skynetDj29GH6o6bAqoixCpDuYtWqi1rm8ZNx1hB3vq");
    public final TokenManager tokenManager = new TokenManager();

    private static final PublicKey USDC_TOKEN_MINT = new PublicKey("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");

    @Test
    public void getAccountInfoBase64() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey);
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Account data list
            final List<String> accountData = accountInfo.getValue().getData();

            // Verify "base64" string in accountData
            assertTrue(accountData.stream().anyMatch(s -> s.equalsIgnoreCase("base64")));
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoBase58() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey, Map.of("encoding", "base58"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Account data list
            final List<String> accountData = accountInfo.getValue().getData();

            // Verify "base64" string in accountData
            assertTrue(accountData.stream().anyMatch(s -> s.equalsIgnoreCase("base58")));
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoRootCommitment() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(publicKey, Map.of("commitment", "root"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Verify any balance
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    /**
     * Uses a {@link MarketBuilder} class to retrieve data about the BTC/USDC Serum market.
     */
    @Test
    public void marketBuilderBtcUsdcTest() {
        // Pubkey of BTC/USDC market
        final PublicKey publicKey = new PublicKey("CVfYa8RGXnuDBeGmniCcdkBwoLqVxh92xB1JqgRQx3F"); //BTC/USDC

        final Market solUsdcMarket = new MarketBuilder()
                .setPublicKey(publicKey)
                .setRetrieveOrderBooks(true)
                .build();

        final OrderBook bids = solUsdcMarket.getBidOrderBook();
        final OrderBook asks = solUsdcMarket.getAskOrderBook();

        LOGGER.info("Best bid = " + bids.getBestBid());
        LOGGER.info("Best ask = " + asks.getBestAsk());

        // Verify at least 1 bid and 1 ask (should always be for BTC/USDC)
        assertTrue(bids.getOrders().size() > 0);
        assertTrue(asks.getOrders().size() > 0);
    }

    /**
     * Verifies that {@link OrderBook} headers are properly read by {@link OrderBook#readOrderBook(byte[])}
     */
    @Test
    public void orderBookTest() {
        byte[] data = new byte[0];

        try {
            data = Files.readAllBytes(Paths.get("orderbook.dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrderBook bidOrderBook = OrderBook.readOrderBook(data);

        LOGGER.info(bidOrderBook.getAccountFlags().toString());

        Slab slab = bidOrderBook.getSlab();

        assertNotNull(slab);
        assertEquals(141, slab.getBumpIndex());
        assertEquals(78, slab.getFreeListLen());
        assertEquals(56, slab.getFreeListHead());
        assertEquals(32, slab.getLeafCount());
    }

    /**
     * Will verify {@link ByteUtils} or {@link SerumUtils} can read seqNum and price.
     * Currently just reads price and logs it.
     */
    @Test
    public void testPriceDeserialization() {
        /* C:\apps\solanaj\orderbook3.dat (1/12/2021 8:55:59 AM)
   StartOffset(d): 00001277, EndOffset(d): 00001292, Length(d): 00000016 */

        byte[] rawData = {
                (byte)0xDB, (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };

        long seqNum = Utils.readInt64(rawData, 0);
        long price = Utils.readInt64(rawData, 8);

        LOGGER.info("Price = " + price);
        LOGGER.info("seqNum = " + seqNum);

        assertEquals(1, price);
        assertEquals(seqNum, -293L);
    }

    /**
     * Uses a {@link MarketBuilder} class to retrieve data about the SOL/USDC Serum market.
     */
    @Test
    public void marketBuilderSolUsdcTest() {
        final PublicKey solUsdcPublicKey = new PublicKey("7xMDbYTCqQEcK2aM9LbetGtNFJpzKdfXzLL5juaLh4GJ");

        final Market solUsdcMarket = new MarketBuilder()
                .setPublicKey(solUsdcPublicKey)
                .setRetrieveOrderBooks(true)
                .build();

        final OrderBook bids = solUsdcMarket.getBidOrderBook();
        LOGGER.info("Market = " + solUsdcMarket.toString());

        final ArrayList<Order> orders = bids.getOrders();
        orders.sort(Comparator.comparingLong(Order::getPrice).reversed());
        orders.forEach(order -> {
            LOGGER.info(order.toString());
        });

        // Verify that an order exists
        assertTrue(orders.size() > 0);
    }

    /**
     * Calls sendTransaction with a call to the Memo program included.
     */
    @Test
    @Ignore
    public void transactionMemoTest() {
        final int lamports = 1337;
        final PublicKey destination = new PublicKey("8xCxNLSdjheuC4EvVNmG77ViTjVcLDmTmqK5zboUu5Nt");

        // Build account from secretkey.dat
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(Paths.get("secretkey.dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create account from private key
        final Account feePayer = new Account(Base58.decode(new String(data)));

        final Transaction transaction = new Transaction();
        transaction.addInstruction(
                SystemProgram.transfer(
                        feePayer.getPublicKey(),
                        destination,
                        lamports
                )
        );

        // Add instruction to write memo
        transaction.addInstruction(
                MemoProgram.writeUtf8(feePayer,"Hello from SolanaJ :)")
        );

        // Call sendTransaction
        String result = null;
        try {
            result = client.getApi().sendTransaction(transaction, feePayer);
            LOGGER.info("Result = " + result);
        } catch (RpcException e) {
            e.printStackTrace();
        }

        assertNotNull(result);
    }

    @Test
    public void getBlockCommitmentTest() {
        // Block 5 used for testing - matches docs
        long block = 5;

        try {
            final BlockCommitment blockCommitment = client.getApi().getBlockCommitment(block);

            LOGGER.info(String.format("block = %d, totalStake = %d", block, blockCommitment.getTotalStake()));

            assertNotNull(blockCommitment);
            assertTrue(blockCommitment.getTotalStake() > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getClusterNodesTest() {
        try {
            final List<ClusterNode> clusterNodes = client.getApi().getClusterNodes();

            // Make sure we got some nodes
            assertNotNull(clusterNodes);
            assertTrue(clusterNodes.size() > 0);

            // Output the nodes
            LOGGER.info("Cluster Nodes:");
            clusterNodes.forEach(clusterNode -> {
                LOGGER.info(clusterNode.toString());
            });
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getEpochInfoTest() {
        try {
            final EpochInfo epochInfo = client.getApi().getEpochInfo();
            assertNotNull(epochInfo);

            LOGGER.info(epochInfo.toString());

            // Validate the returned data
            assertTrue(epochInfo.getAbsoluteSlot() > 0);
            assertTrue(epochInfo.getEpoch() > 0);
            assertTrue(epochInfo.getSlotsInEpoch() > 0);
            assertTrue(epochInfo.getBlockHeight() > 0);
            assertTrue(epochInfo.getSlotIndex() > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getEpochScheduleTest() {
        try {
            final EpochSchedule epochSchedule = client.getApi().getEpochSchedule();
            assertNotNull(epochSchedule);

            LOGGER.info(epochSchedule.toString());

            // Validate the returned data
            assertTrue(epochSchedule.getSlotsPerEpoch() > 0);
            assertTrue(epochSchedule.getLeaderScheduleSlotOffset() > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendTokenTest() {
        final PublicKey source = new PublicKey("A71WvME6ZhR4SFG3Ara7zQK5qdRSB97jwTVmB3sr7XiN"); // Private key's USDC token account
        final PublicKey destination = new PublicKey("9A1anCYGg98tUB8LUhtmjq4STqfJ8Qc3vxCDB5TQhXAw"); // Test destination, skynet's USDC account
        final int tokenAmount = 10; // 0.000100 USDC

        // Build account from secretkey.dat
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(Paths.get("secretkey.dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create account from private key
        final Account owner = new Account(Base58.decode(new String(data)));

        // "10" = 0.0000001 (or similar)
        final String txId = tokenManager.transfer(
                owner,
                source,
                destination,
                USDC_TOKEN_MINT,
                tokenAmount
        );

        assertNotNull(txId);

    }

    @Test
    public void transferCheckedTest() {
        final PublicKey source = new PublicKey("A71WvME6ZhR4SFG3Ara7zQK5qdRSB97jwTVmB3sr7XiN"); // Private key's USDC token account
        final PublicKey destination = new PublicKey("skynetDj29GH6o6bAqoixCpDuYtWqi1rm8ZNx1hB3vq"); // Test destination, skynet's USDC account

        /*
            amount = "0.0001" usdc
            amount = 100
            decimals = 6
         */

        final long tokenAmount = 100;
        final byte decimals = 6;

        // TODO - build util for building this private key in tests
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(Paths.get("secretkey.dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create account from private key
        final Account owner = new Account(Base58.decode(new String(data)));

        final String txId = tokenManager.transferCheckedToSolAddress(
                owner,
                source,
                destination,
                USDC_TOKEN_MINT,
                tokenAmount,
                decimals
        );

        // TODO - actually verify something
        assertNotNull(txId);

    }
}
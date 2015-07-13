package com.cigolabs.timeseries.cassandra;

/**
 * Created by akarmaka on 7/12/15.
 */
import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

public class PureJavaClient {
    private Cluster cluster;
    private Session session;

    public void connect(String... nodes) {
        cluster = Cluster.builder()
                .addContactPoint(nodes[0])
                .withLoadBalancingPolicy(new DCAwareRoundRobinPolicy("US_EAST"))
                // For automatic failover
                .withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
                .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
                .build();
        // Setting LZ4 compression scheme
        cluster.getConfiguration()
                .getProtocolOptions()
                .setCompression(ProtocolOptions.Compression.LZ4);
        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n",
                metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n",
                    host.getDatacenter(), host.getAddress(), host.getRack());
        }
        session = cluster.connect();
    }

    public ResultSetFuture getRows(String schema, String columnFamilyNm) {
        Select query = QueryBuilder.select().all().from(schema, columnFamilyNm);
        return session.executeAsync(query);
    }


    public void close() {
        cluster.close();
    }

    public static void main(String[] args) {
        PureJavaClient client = new PureJavaClient();
        //client.connect("127.0.0.1");
        client.connect(args[0]);
        ResultSetFuture results = client.getRows(args[1],args[2]);
        /*for (Row row : results.getUninterruptibly()) {
            System.out.printf( "%s: %s / %s\n",
                    row.getString()
                    row.getString("title"),
                    row.getString("album") );
        }*/
        client.close();
    }
}
package models.services;

import models.Account;
import models.Group;
import models.Post;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import play.Logger;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by Iven on 22.12.2014.
 */
public class ElasticsearchService {
    private static ElasticsearchService instance = null;
    private static Client client = null;

    private ElasticsearchService() {
        client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

    }

    public static ElasticsearchService getInstance() {
        if (instance == null) {
            instance = new ElasticsearchService();
        }
        return instance;
    }

    public Client getClient() {
        return client;
    }

    public void closeClient() {
        client.close();
    }

    public static void indexPost(Post post) throws IOException {
        IndexResponse indexResponse = client.prepareIndex("htwplus","post", post.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("content", post.content)
                        .endObject())
                .execute()
                .actionGet();

        Logger.info(post.id+" indexed? "+indexResponse.isCreated());
    }

    public static void indexGroup(Group group) throws IOException {
        IndexResponse indexResponse = getInstance().getClient().prepareIndex("htwplus", "group", group.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("title", group.title)
                        .endObject())
                .execute()
                .actionGet();
        Logger.info(group.title + " indexed? " + indexResponse.isCreated());
    }

    public static void indexAccount(Account account) throws IOException {
        IndexResponse response = client.prepareIndex("htwplus", "user", account.id.toString())
                .setSource(jsonBuilder()
                                .startObject()
                                .field("name", account.name)
                                .endObject()
                )
                .execute()
                .actionGet();

        Logger.info(account.name+" indexiert? "+response.isCreated());
    }
}

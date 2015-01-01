package models.services;

import com.typesafe.config.ConfigFactory;
import models.Account;
import models.Group;
import models.Post;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import play.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by Iven on 22.12.2014.
 */
public class ElasticsearchService {
    private static ElasticsearchService instance = null;
    private static Client client = null;
    private static final String ES_SETTINGS = ConfigFactory.load().getString("elasticsearch.settings");
    private static final String ES_USER_MAPPING = ConfigFactory.load().getString("elasticsearch.userMapping");
    private static final String ES_GROUP_MAPPING = ConfigFactory.load().getString("elasticsearch.groupMapping");
    private static final String ES_POST_MAPPING = ConfigFactory.load().getString("elasticsearch.postMapping");
    private static final String ES_INDEX = ConfigFactory.load().getString("elasticsearch.index");

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

    public static void createAnalyzer() throws IOException {
        getInstance().getClient().admin().indices().prepareCreate(ES_INDEX)
                .setSettings(ES_SETTINGS)
                .execute().actionGet();
    }

    public static void createMapping() throws IOException {
        getInstance().getClient().admin().indices().preparePutMapping(ES_INDEX).setType("user")
                .setSource(ES_USER_MAPPING)
                .execute().actionGet();

        getInstance().getClient().admin().indices().preparePutMapping(ES_INDEX).setType("post")
                .setSource(ES_POST_MAPPING)
                .execute().actionGet();

        getInstance().getClient().admin().indices().preparePutMapping(ES_INDEX).setType("group")
                .setSource(ES_GROUP_MAPPING)
                .execute().actionGet();
    }

    public static void indexPost(Post post) throws IOException {
        IndexResponse indexResponse = getInstance().getClient().prepareIndex(ES_INDEX, "post", post.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("content", post.content)
                        .endObject())
                .execute()
                .actionGet();

        Logger.info(post.id + " indexiert? "+indexResponse.isCreated());
    }

    public static void indexGroup(Group group) throws IOException {
        IndexResponse indexResponse = getInstance().getClient().prepareIndex(ES_INDEX, "group", group.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("title", group.title)
                        .endObject())
                .execute()
                .actionGet();
        Logger.info(group.title + " indexiert? " + indexResponse.isCreated());
    }

    public static void indexAccount(Account account) throws IOException {
        IndexResponse response = getInstance().getClient().prepareIndex(ES_INDEX, "user", account.id.toString())
                .setSource(jsonBuilder()
                                .startObject()
                                .field("name", account.name)
                                .endObject()
                )
                .execute()
                .actionGet();

        Logger.info(account.name + " indexiert? "+response.isCreated());
    }

    public static SearchResponse doSearch(String query, String... fields) throws ExecutionException, InterruptedException {
        /**
         * Build ES Query.
         * Search with query on given fields.
         */
        QueryBuilder qb = QueryBuilders.multiMatchQuery(query, fields).operator(MatchQueryBuilder.Operator.AND);
        SearchResponse response = ElasticsearchService.getInstance().getClient().prepareSearch(ES_INDEX)
                .setQuery(qb)
                .execute()
                .get();

        return response;
    }
}

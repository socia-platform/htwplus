package models.services;

import com.typesafe.config.ConfigFactory;
import models.*;
import models.enums.LinkType;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import play.Logger;

import java.io.IOException;
import java.util.List;
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
                        .field("owner", post.owner.id)
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
                        .field("grouptype", group.groupType)
                        .field("owner", group.owner.id)
                        .field("member", GroupAccount.findAccountsIdByGroup(group, LinkType.establish))
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
                                .field("avatar", account.avatar)
                                .field("friends", Friendship.findFriendsId(account))
                                .endObject()
                )
                .execute()
                .actionGet();

        Logger.info(account.name + " indexiert? "+response.isCreated());
    }

    /**
     * Build search query based on all provided fields
     * @param query - Terms to search for (e.g. 'informatik')
     * @param currentAccountId - AccountId from user who is logged in (for scoring)
     * @param mustFields - All fields to search on
     * @param scoringFields - All fields which affect the scoring
     * @return - JSON response from Elasticsearch
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static SearchResponse doSearch(String query, int page, String currentAccountId, List<String> mustFields, List<String> scoringFields) throws ExecutionException, InterruptedException {

         // Build searchQuery by provided fields (mustFields) to search on
        QueryBuilder searchQuery = QueryBuilders.multiMatchQuery(query, mustFields.toArray(new String[mustFields.size()]));

        // Build scoringQuery by provided fields (shouldFields) to increase the scoring of a better matching hit
        QueryBuilder scoringQuery = QueryBuilders.multiMatchQuery(currentAccountId, scoringFields.toArray(new String[scoringFields.size()])).operator(MatchQueryBuilder.Operator.OR);

        // Build completeQuery with search- and scoringQuery
        QueryBuilder completeQuery = QueryBuilders.boolQuery().must(searchQuery).should(scoringQuery);

        // Build searchRequest which will be executed after fields to highlight are added.
        SearchRequestBuilder searchRequest = ElasticsearchService.getInstance().getClient().prepareSearch(ES_INDEX)
                .setFetchSource(new String[]{"title", "grouptype", "name", "avatar", "content"}, new String[]{})
                .setQuery(completeQuery);

        // Add highlighting on all fields to search on
        for (String field : mustFields) {
            searchRequest.addHighlightedField(field);
        }

        // Define html tags for highlighting
        searchRequest = searchRequest.setHighlighterPreTags("<strong>").setHighlighterPostTags("</strong>");

        // Enable pagination
        searchRequest = searchRequest.setFrom((page * 10) - 10);

        // Execute searchRequest
        SearchResponse response = searchRequest.execute().get();

        return response;
    }
}

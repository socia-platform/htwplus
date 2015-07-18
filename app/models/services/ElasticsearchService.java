package models.services;

import com.typesafe.config.ConfigFactory;
import models.*;
import models.enums.LinkType;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.Logger;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.FilterBuilders.*;

/**
 * Created by Iven on 22.12.2014.
 */
public class ElasticsearchService {
    private static ElasticsearchService instance = null;
    private static Client client = null;
    private static final String ES_SERVER = ConfigFactory.load().getString("elasticsearch.server");
    private static final String ES_SETTINGS = ConfigFactory.load().getString("elasticsearch.settings");
    private static final String ES_USER_MAPPING = ConfigFactory.load().getString("elasticsearch.userMapping");
    private static final String ES_GROUP_MAPPING = ConfigFactory.load().getString("elasticsearch.groupMapping");
    private static final String ES_POST_MAPPING = ConfigFactory.load().getString("elasticsearch.postMapping");
    private static final String ES_INDEX = ConfigFactory.load().getString("elasticsearch.index");
    private static final String ES_TYPE_USER = ConfigFactory.load().getString("elasticsearch.userType");
    private static final String ES_TYPE_GROUP = ConfigFactory.load().getString("elasticsearch.groupType");
    private static final String ES_TYPE_POST = ConfigFactory.load().getString("elasticsearch.postType");
    private static final int ES_RESULT_SIZE = ConfigFactory.load().getInt("elasticsearch.search.limit");

    private ElasticsearchService() {
        client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(ES_SERVER, 9300));
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

    public static boolean isClientAvailable() {
        if(((TransportClient) getInstance().getClient()).connectedNodes().size() == 0)
            return false;
        return true;
    }

    public static boolean isIndexExists() {
        return getInstance().getClient().admin().indices().exists(new IndicesExistsRequest(ES_INDEX)).actionGet().isExists();
    }

    public static void deleteIndex() {
        if(isClientAvailable()) getInstance().getClient().admin().indices().delete(new DeleteIndexRequest(ES_INDEX)).actionGet();
    }

    public static void createAnalyzer() {
        if(isClientAvailable()) getInstance().getClient().admin().indices().prepareCreate(ES_INDEX)
                .setSettings(ES_SETTINGS)
                .execute().actionGet();
    }

    public static void createMapping() {
        if(isClientAvailable()) getInstance().getClient().admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_USER)
                .setSource(ES_USER_MAPPING)
                .execute().actionGet();

        if(isClientAvailable()) getInstance().getClient().admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_POST)
                .setSource(ES_POST_MAPPING)
                .execute().actionGet();

        if(isClientAvailable()) getInstance().getClient().admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_GROUP)
                .setSource(ES_GROUP_MAPPING)
                .execute().actionGet();
    }

    public static void indexPost(Post post) throws IOException {
        if(isClientAvailable()) getInstance().getClient().prepareIndex(ES_INDEX, ES_TYPE_POST, post.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("content", post.content)
                        .field("owner", post.owner.id)
                        .field("public", post.isPublic())
                        .field("viewable", post.findAllowedToViewAccountIds())
                        .endObject())
                .execute()
                .actionGet();
    }

    public static void indexGroup(Group group) throws IOException {
        if(isClientAvailable()) getInstance().getClient().prepareIndex(ES_INDEX, ES_TYPE_GROUP, group.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("title", group.title)
                        .field("grouptype", group.groupType)
                        .field("public", true)
                        .field("owner", group.owner.id)
                        .field("member", GroupAccount.findAccountIdsByGroup(group, LinkType.establish))
                        .endObject())
                .execute()
                .actionGet();
    }

    public static void indexAccount(Account account) throws IOException {
        if(isClientAvailable()) getInstance().getClient().prepareIndex(ES_INDEX, ES_TYPE_USER, account.id.toString())
                .setSource(jsonBuilder()
                                .startObject()
                                .field("name", account.name)
                                .field("studycourse", account.studycourse != null ? account.studycourse.title : "")
                                .field("degree", account.degree != null ? account.degree : "")
                                .field("semester", account.semester != null ? String.valueOf(account.semester) : "")
                                .field("role", account.role != null ? account.role.getDisplayName() : "")
                                .field("initial", account.getInitials())
                                .field("avatar", account.avatar)
                                .field("public", true)
                                .field("friends", Friendship.findFriendsId(account))
                                .endObject())
                .execute()
                .actionGet();
    }

    /**
     * Build search query based on all provided fields
     * @param caller - Define normal search or autocomplete
     * @param query - Terms to search for (e.g. 'informatik')
     * @param filter - Filter for searchfacets (e.g. user, group, comment)
     * @param page - Which results should be shown (e.g. 1: 1-10 ; 2: 11-20 etc.)
     * @param currentAccountId - AccountId from user who is logged in (for scoring)
     * @param mustFields - All fields to search on
     * @param scoringFields - All fields which affect the scoring
     * @return - JSON response from Elasticsearch
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static SearchResponse doSearch(String caller, String query, String filter, HashMap<String, String[]> userFacets, int page, String currentAccountId, List<String> mustFields, List<String> scoringFields) throws ExecutionException, InterruptedException {

        QueryBuilder searchQuery;

        if(query.isEmpty() || query == null) {
            // Build searchQuery to search for everything
            searchQuery = QueryBuilders.matchAllQuery();
        } else {
            // Build searchQuery by provided fields (mustFields) to search on
            searchQuery = QueryBuilders.multiMatchQuery(query, mustFields.toArray(new String[mustFields.size()]));
        }

        // Build scoringQuery by provided fields (shouldFields) to increase the scoring of a better matching hit
        QueryBuilder scoringQuery = QueryBuilders.multiMatchQuery(currentAccountId, scoringFields.toArray(new String[scoringFields.size()]));

        // Build completeQuery with search- and scoringQuery
        QueryBuilder completeQuery = QueryBuilders.boolQuery().must(searchQuery).should(scoringQuery);

        // Build viewableFilter to show authorized posts only
        final BoolFilterBuilder boolFilterBuilder = FilterBuilders.boolFilter();

        boolFilterBuilder.should(FilterBuilders.termFilter("viewable", currentAccountId),FilterBuilders.termFilter("public", true));

        if (!filter.equals("all")) {
            boolFilterBuilder.must(typeFilter(filter));
        }

        if(userFacets != null) {
            if(userFacets.get("studycourse").length != 0) {
                boolFilterBuilder.must(termsFilter("user.studycourse", userFacets.get("studycourse")));
            }

            if(userFacets.get("degree").length != 0) {
                boolFilterBuilder.must(termsFilter("user.degree", userFacets.get("degree")));
            }

            if(userFacets.get("semester").length != 0) {
                boolFilterBuilder.must(termsFilter("user.semester", userFacets.get("semester")));
            }

            if(userFacets.get("role").length != 0) {
                boolFilterBuilder.must(termsFilter("user.role", userFacets.get("role")));
            }
        }

        // Build filteredQuery to apply viewableFilter to completeQuery
        QueryBuilder filteredQuery = QueryBuilders.filteredQuery(completeQuery, boolFilterBuilder);

        // Build searchRequest which will be executed after fields to highlight are added.
        SearchRequestBuilder searchRequest = ElasticsearchService.getInstance().getClient().prepareSearch(ES_INDEX)
                .setQuery(filteredQuery);

        // Add highlighting on all fields to search on
        for (String field : mustFields) {
            searchRequest.addHighlightedField(field);
        }

        // Define html tags for highlighting
        if (caller.equals("search"))
            searchRequest = searchRequest.setHighlighterPreTags("[startStrong]").setHighlighterPostTags("[endStrong]").setHighlighterNumOfFragments(0);

        // Enable pagination
        searchRequest = searchRequest.setFrom((page * ES_RESULT_SIZE) - ES_RESULT_SIZE);

        // Add term aggregation for facet count
        searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("types").field("_type"));


        if (filter.equals("user")) {
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("studycourse").field("user.studycourse"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("degree").field("user.degree"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("semester").field("user.semester"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("role").field("user.role"));
        }

        // Apply PostFilter if request mode is not 'all'
        final BoolFilterBuilder boolFilterBuilder2 = boolFilter();

        if(boolFilterBuilder2.hasClauses()) {
            searchRequest.setPostFilter(boolFilterBuilder2);
        }



        //Logger.info(searchRequest.toString());
        // Execute searchRequest
        SearchResponse response = searchRequest.execute().get();

        return response;
    }

    public static void deleteGroup(Group group) {
        if(isClientAvailable()) getInstance().getClient().prepareDelete(ES_INDEX, ES_TYPE_GROUP, group.id.toString())
                .execute()
                .actionGet();
    }

    public static void deletePost(Post post) {
        if(isClientAvailable()) getInstance().getClient().prepareDelete(ES_INDEX, ES_TYPE_POST, post.id.toString())
                .execute()
                .actionGet();
    }

    public static void deleteAccount(Account account) {
        if(isClientAvailable()) getInstance().getClient().prepareDelete(ES_INDEX, ES_TYPE_USER, account.id.toString())
                .execute()
                .actionGet();
    }
}

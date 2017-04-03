
package models.services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import managers.FriendshipManager;
import managers.GroupAccountManager;
import managers.PostManager;
import models.Account;
import models.Group;
import models.Post;
import models.enums.LinkType;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import play.Environment;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


/**
 * Created by Iven on 22.12.2014.
 */
@Singleton
public class ElasticsearchService implements IElasticsearchService {

    @Inject
    PostManager postManger;
    @Inject
    GroupAccountManager groupAccountManager;
    @Inject
    FriendshipManager friendshipManager;
    @Inject
    Environment environment;

    private Client client = null;
    private Config conf = ConfigFactory.load().getConfig("elasticsearch");

    private final String ES_SERVER = conf.getString("server");
    private final String ES_INDEX = conf.getString("index");
    private final String ES_TYPE_USER = conf.getString("userType");
    private final String ES_TYPE_GROUP = conf.getString("groupType");
    private final String ES_TYPE_POST = conf.getString("postType");
    private final int ES_RESULT_SIZE = conf.getInt("searchLimit");
    private final String ES_SETTINGS = "elasticsearch/settings.json";
    private final String ES_USER_MAPPING = "elasticsearch/user_mapping.json";
    private final String ES_GROUP_MAPPING = "elasticsearch/group_mapping.json";
    private final String ES_POST_MAPPING = "elasticsearch/post_mapping.json";


    public ElasticsearchService() {
        try {
            client = TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ES_SERVER), 9300));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Client getClient() {
        return client;
    }

    public void closeClient() {
        Logger.info("closing ES client...");
        client.close();
        Logger.info("ES client closed");
    }

    public boolean isClientAvailable() {
        if (((TransportClient) client).connectedNodes().size() == 0)
            return false;
        return true;
    }

    public boolean isIndexExists() {
        return client.admin().indices().exists(new IndicesExistsRequest(ES_INDEX)).actionGet().isExists();
    }

    public void deleteIndex() {
        if (isClientAvailable()) client.admin().indices().delete(new DeleteIndexRequest(ES_INDEX)).actionGet();
    }

    public void createAnalyzer() {
        if (isClientAvailable()) client.admin().indices().prepareCreate(ES_INDEX)
                .setSettings(loadFromFile(ES_SETTINGS))
                .execute().actionGet();
    }

    public void createMapping() {
        if (isClientAvailable()) client.admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_USER)
                .setSource(loadFromFile(ES_USER_MAPPING))
                .execute().actionGet();

        if (isClientAvailable()) client.admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_POST)
                .setSource(loadFromFile(ES_POST_MAPPING))
                .execute().actionGet();

        if (isClientAvailable()) client.admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_GROUP)
                .setSource(loadFromFile(ES_GROUP_MAPPING))
                .execute().actionGet();
    }

    public void index(Object model) throws IOException {
        if (model instanceof Post) indexPost(((Post) model));
        if (model instanceof Group) indexGroup(((Group) model));
        if (model instanceof Account) indexAccount(((Account) model));
    }

    private void indexPost(Post post) throws IOException {
        if (isClientAvailable()) client.prepareIndex(ES_INDEX, ES_TYPE_POST, post.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("content", post.content)
                        .field("owner", post.owner.id)
                        .field("public", postManger.isPublic(post))
                        .field("viewable", postManger.findAllowedToViewAccountIds(post))
                        .endObject())
                .execute()
                .actionGet();
    }

    private void indexGroup(Group group) throws IOException {
        if (isClientAvailable()) client.prepareIndex(ES_INDEX, ES_TYPE_GROUP, group.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("title", group.title)
                        .field("grouptype", group.groupType)
                        .field("public", true)
                        .field("owner", group.owner.id)
                        .field("avatar", group.hasAvatar)
                        .field("member", groupAccountManager.findAccountIdsByGroup(group, LinkType.establish))
                        .endObject())
                .execute()
                .actionGet();
    }

    private void indexAccount(Account account) throws IOException {
        if (isClientAvailable()) client.prepareIndex(ES_INDEX, ES_TYPE_USER, account.id.toString())
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
                        .field("friends", friendshipManager.findFriendsId(account))
                        .endObject())
                .execute()
                .actionGet();
    }

    /**
     * Build search query based on all provided fields
     *
     * @param caller           - Define normal search or autocomplete
     * @param query            - Terms to search for (e.g. 'informatik')
     * @param filter           - Filter for searchfacets (e.g. user, group, comment)
     * @param page             - Which results should be shown (e.g. 1: 1-10 ; 2: 11-20 etc.)
     * @param currentAccountId - AccountId from user who is logged in (for scoring)
     * @param mustFields       - All fields to search on
     * @param scoringFields    - All fields which affect the scoring
     * @return - JSON response from Elasticsearch
     * @throws ExecutionException
     * @throws InterruptedException
     */

    @Override
    public SearchResponse doSearch(String caller, String query, String filter, HashMap<String, String[]> facets, int page, String currentAccountId, List<String> mustFields, List<String> scoringFields) throws ExecutionException, InterruptedException {

        QueryBuilder searchQuery;

        if (query.isEmpty() || query == null) {
            // Build searchQuery to search for everything
            searchQuery = QueryBuilders.matchAllQuery();
        } else {
            // Build searchQuery by provided fields (mustFields) to search on
            searchQuery = QueryBuilders.multiMatchQuery(query, mustFields.toArray(new String[mustFields.size()]));
        }

        // Build scoringQuery by provided fields (shouldFields) to increase the scoring of a better matching hit
        QueryBuilder scoringQuery = QueryBuilders.multiMatchQuery(currentAccountId, scoringFields.toArray(new String[scoringFields.size()]));

        // Build boolQuery to enable filter possibilities
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Add should filter to show authorized posts only
        boolQuery.should(QueryBuilders.termQuery("viewable", currentAccountId)).should(QueryBuilders.termQuery("public", true));

        // Add mode-filter to filter only for users/group or posts
        if (!filter.equals("all")) {
            boolQuery.must(QueryBuilders.typeQuery(filter));
        }

        // Add facet-filter to filter for mode related stuff (eg. user -> students or group -> open)
        if (facets != null) {
            if (facets.get("studycourse").length != 0) {
                for (String facet : facets.get("studycourse")) {
                    boolQuery.must(QueryBuilders.termQuery("studycourse", facet));
                }
            }

            if (facets.get("degree").length != 0) {
                for (String facet : facets.get("degree")) {
                    boolQuery.must(QueryBuilders.termQuery("degree", facet));
                }
            }

            if (facets.get("semester").length != 0) {
                for (String facet : facets.get("semester")) {
                    boolQuery.must(QueryBuilders.termQuery("semester", facet));
                }

            }

            if (facets.get("role").length != 0) {
                for (String facet : facets.get("role")) {
                    boolQuery.must(QueryBuilders.termQuery("role", facet));
                }

            }

            if (facets.get("grouptype").length != 0) {
                for (String facet : facets.get("grouptype")) {
                    boolQuery.must(QueryBuilders.termQuery("grouptype", facet));
                }
            }
        }

        // Build completeQuery with search- and scoringQuery
        QueryBuilder completeQuery = QueryBuilders.boolQuery().must(searchQuery).should(scoringQuery).filter(boolQuery);

        // Build searchRequest which will be executed after fields to highlight are added.
        SearchRequestBuilder searchRequest = client.prepareSearch(ES_INDEX)
                .setQuery(completeQuery);

        // Add highlighting on all fields to search on
        for (String field : mustFields) {
            searchRequest.addHighlightedField(field);
        }

        // Define html tags for highlighting
        searchRequest = searchRequest.setHighlighterPreTags("[startStrong]").setHighlighterPostTags("[endStrong]").setHighlighterNumOfFragments(0);

        // Enable pagination
        searchRequest = searchRequest.setFrom((page * ES_RESULT_SIZE) - ES_RESULT_SIZE);

        // Add term aggregation for facet count
        searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("types").field("_type"));

        // Add user aggregations
        if (filter.equals("user")) {
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("studycourse").field("studycourse"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("degree").field("degree"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("semester").field("semester"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("role").field("role"));
        }

        // Add group aggregations
        if (filter.equals("group")) {
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("grouptype").field("grouptype"));
        }

        // Apply PostFilter if request mode is not 'all'
        /**final BoolFilterBuilder boolFilterBuilder2 = boolFilter();

         if(boolFilterBuilder2.hasClauses()) {
         searchRequest.setPostFilter(boolFilterBuilder2);
         }*/

        //Logger.info(searchRequest.toString());

        // Execute searchRequest
        SearchResponse response = searchRequest.execute().get();

        //Logger.info(response.toString());

        return response;
    }

    public void delete(Object model) {
        if (model instanceof Post) deletePost(((Post) model));
        if (model instanceof Group) deleteGroup(((Group) model));
        if (model instanceof Account) deleteAccount(((Account) model));
    }

    private void deleteGroup(Group group) {
        if (isClientAvailable()) client.prepareDelete(ES_INDEX, ES_TYPE_GROUP, group.id.toString())
                .execute()
                .actionGet();
    }

    private void deletePost(Post post) {
        if (isClientAvailable()) client.prepareDelete(ES_INDEX, ES_TYPE_POST, post.id.toString())
                .execute()
                .actionGet();
    }

    private void deleteAccount(Account account) {
        if (isClientAvailable()) client.prepareDelete(ES_INDEX, ES_TYPE_USER, account.id.toString())
                .execute()
                .actionGet();
    }

    private String loadFromFile(String filePath) {
        try {
            return IOUtils.toString(environment.resourceAsStream(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}

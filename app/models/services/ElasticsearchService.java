
package models.services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import managers.FriendshipManager;
import managers.GroupAccountManager;
import managers.MediaManager;
import managers.PostManager;
import models.Account;
import models.Group;
import models.Media;
import models.Post;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import play.Environment;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


/**
 * Created by Iven on 22.12.2014.
 */
@Singleton
public class ElasticsearchService implements IElasticsearchService {

    final Logger.ALogger logger = Logger.of(ElasticsearchService.class);

    Environment environment;

    private Client client = null;
    private Config conf = ConfigFactory.load().getConfig("elasticsearch");

    private final String ES_SERVER = conf.getString("server");
    private final String ES_INDEX = conf.getString("index");
    private final String ES_TYPE_USER = conf.getString("userType");
    private final String ES_TYPE_GROUP = conf.getString("groupType");
    private final String ES_TYPE_POST = conf.getString("postType");
    private final String ES_TYPE_MEDIUM = conf.getString("mediumType");
    private final int ES_RESULT_SIZE = conf.getInt("searchLimit");
    private final String ES_SETTINGS = "elasticsearch/settings.json";
    private final String ES_USER_MAPPING = "elasticsearch/user_mapping.json";
    private final String ES_GROUP_MAPPING = "elasticsearch/group_mapping.json";
    private final String ES_POST_MAPPING = "elasticsearch/post_mapping.json";
    private final String ES_MEDIUM_MAPPING = "elasticsearch/medium_mapping.json";

    @Inject
    public ElasticsearchService(Environment environment) {
        this.environment = environment;

        try {
            client = new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ES_SERVER), 9300));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Client getClient() {
        return client;
    }

    public void closeClient() {
        logger.info("closing ES client...");
        client.close();
        logger.info("ES client closed");
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

        if (isClientAvailable()) client.admin().indices().preparePutMapping(ES_INDEX).setType(ES_TYPE_MEDIUM)
                .setSource(loadFromFile(ES_MEDIUM_MAPPING))
                .execute().actionGet();
    }

    public void indexPost(Post post, boolean isPublic, Set<Long> allowedToViewAccountIds) throws IOException {
        if (isClientAvailable()) client.prepareIndex(ES_INDEX, ES_TYPE_POST, post.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("content", post.content)
                        .field("owner", post.owner.id)
                        .field("public", isPublic)
                        .field("viewable", allowedToViewAccountIds)
                        .endObject())
                .execute()
                .actionGet();
    }

    public void indexGroup(Group group, List<Long> accountIdsByGroup) throws IOException {
        if (isClientAvailable()) client.prepareIndex(ES_INDEX, ES_TYPE_GROUP, group.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("title", group.title)
                        .field("grouptype", group.groupType)
                        .field("public", true)
                        .field("owner", group.owner.id)
                        .field("avatar", group.hasAvatar)
                        .field("member", accountIdsByGroup)
                        .endObject())
                .execute()
                .actionGet();
    }

    public void indexAccount(Account account, List<Long> friendIds) throws IOException {
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
                        .field("friends", friendIds)
                        .endObject())
                .execute()
                .actionGet();
    }

    public void indexMedium(Media medium, boolean isPublic, Set<Long> allowdToViewAccountIds) throws IOException {
        if (isClientAvailable()) client.prepareIndex(ES_INDEX, ES_TYPE_MEDIUM, medium.id.toString())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("owner", medium.owner.id)
                        .field("filename", medium.fileName)
                        .field("viewable", allowdToViewAccountIds)
                        .field("public", isPublic)
                        .field("ownerName", medium.owner.name)
                        .field("folderName", medium.folder.name)
                        .field("createdAt", medium.createdAt)
                        .field("mimeType", medium.fileName.lastIndexOf(".") > 0 ? medium.fileName.substring(medium.fileName.lastIndexOf(".")) : "unbekannt")
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

            if (facets.get("ownerName").length > 0) {
                for (String facet : facets.get("ownerName")) {
                    boolQuery.must(QueryBuilders.termQuery("ownerName", facet));
                }
            }

            if (facets.get("folderName").length > 0) {
                for (String facet : facets.get("folderName")) {
                    boolQuery.must(QueryBuilders.termQuery("folderName", facet));
                }
            }

            if (facets.get("createdAt").length > 0) {
                for (String facet : facets.get("createdAt")) {
                    boolQuery.must(QueryBuilders.rangeQuery("createdAt").gt(Integer.parseInt(facet)).lt(Integer.parseInt(facet)+1));
                }
            }

            if (facets.get("mimeType").length > 0) {
                for (String facet : facets.get("mimeType")) {
                    boolQuery.must(QueryBuilders.termQuery("mimeType", facet));
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
            //searchRequest.addHighlightedField(field);
        }

        // Define html tags for highlighting
        //searchRequest = searchRequest.setHighlighterPreTags("[startStrong]").setHighlighterPostTags("[endStrong]").setHighlighterNumOfFragments(0);

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

        // Add medium aggregations
        if (filter.equals("medium")) {
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("ownerName").field("ownerName"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("folderName").field("folderName"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.terms("mimeType").field("mimeType"));
            searchRequest = searchRequest.addAggregation(AggregationBuilders.dateHistogram("createdAt").field("createdAt").dateHistogramInterval(DateHistogramInterval.YEAR).format("yyyy"));
        }

        // Apply PostFilter if request mode is not 'all'
        /**final BoolFilterBuilder boolFilterBuilder2 = boolFilter();

         if(boolFilterBuilder2.hasClauses()) {
         searchRequest.setPostFilter(boolFilterBuilder2);
         }*/

        //logger.info(searchRequest.toString());

        // Execute searchRequest
        SearchResponse response = searchRequest.execute().get();

        //logger.info(response.toString());

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
        Scanner s = new Scanner(environment.resourceAsStream(filePath)).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}

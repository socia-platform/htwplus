package models.services;

import com.google.inject.ImplementedBy;
import com.typesafe.config.ConfigFactory;
import models.*;
import models.enums.LinkType;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by Iven on 22.12.2014.
 */
@ImplementedBy(ElasticsearchService.class)
public interface IElasticsearchService {

    Client getClient();

    void closeClient();

    void deleteIndex();

    void createAnalyzer() throws IOException ;

    void createMapping() throws IOException;

    void indexPost(Post post) throws IOException;

    void indexGroup(Group group) throws IOException;

    void indexAccount(Account account) throws IOException;

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
    SearchResponse doSearch(String caller, String query, String filter, HashMap<String, String[]> facets, int page, String currentAccountId, List<String> mustFields, List<String> scoringFields) throws ExecutionException, InterruptedException;

    void deleteGroup(Group group);

    void deletePost(Post post);

    void deleteAccount(Account account);
}

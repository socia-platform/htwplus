package models.services;

import com.google.inject.ImplementedBy;
import models.Account;
import models.Group;
import models.Post;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Iven on 22.12.2014.
 */
@ImplementedBy(ElasticsearchService.class)
public interface IElasticsearchService {

    Client getClient();

    void closeClient();

    void deleteIndex();

    void createAnalyzer() throws IOException;

    void createMapping() throws IOException;

    void delete(Object model);

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
    SearchResponse doSearch(String caller, String query, String filter, HashMap<String, String[]> facets, int page, String currentAccountId, List<String> mustFields, List<String> scoringFields) throws ExecutionException, InterruptedException;
}

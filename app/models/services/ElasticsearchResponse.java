package models.services;

import managers.AccountManager;
import managers.GroupManager;
import managers.MediaManager;
import managers.PostManager;
import models.Account;
import models.Group;
import models.Media;
import models.Post;
import org.apache.commons.lang3.StringEscapeUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Iven on 16.07.2015.
 */
@Singleton
public class ElasticsearchResponse {


    @Inject
    PostManager postManager;

    @Inject
    GroupManager groupManager;

    @Inject
    AccountManager accountManager;

    @Inject
    MediaManager mediaManager;

    SearchResponse elasticsearchResponse;
    public List<Object> resultList;
    public String keyword;
    public String searchMode;

    public long lUserDocuments;
    public long lGroupDocuments;
    public long lPostDocuments;
    public long lMediumDocuments;

    public HashMap studycoursesMap;
    public HashMap degreeMap;
    public HashMap semesterMap;
    public HashMap roleMap;
    public HashMap grouptypeMap;

    public void create(final SearchResponse response, final String keyword, final String mode) {
        this.keyword = keyword;
        this.searchMode = mode;
        this.elasticsearchResponse = response;
        this.elasticserchReponseToList();
        this.aggregationStuff();
    }

    /**
     * Iterate over response and add each searchHit to one list.
     * Pay attention to view rights for post.content.
     */
    private void elasticserchReponseToList() {
        resultList = new ArrayList<>();
        for (SearchHit searchHit : elasticsearchResponse.getHits().getHits()) {
            switch (searchHit.type()) {
                case "user":
                    Account account = accountManager.findById(Long.parseLong(searchHit.getId()));
                    if (account != null)
                        resultList.add(account);
                    break;
                case "post":
                    Post post = postManager.findById(Long.parseLong(searchHit.getId()));
                    if (post != null) {
                        String searchContent = post.content;
                        if (!searchHit.getHighlightFields().isEmpty())
                            searchContent = searchHit.getHighlightFields().get("content").getFragments()[0].string();
                        post.searchContent = StringEscapeUtils.escapeHtml4(searchContent)
                                .replace("[startStrong]", "**")
                                .replace("[endStrong]", "**");
                        resultList.add(post);
                    }
                    break;
                case "group":
                    Group group = groupManager.findById(Long.parseLong(searchHit.getId()));
                    if (group != null)
                        resultList.add(group);
                    break;
                case "medium":
                    Media medium = mediaManager.findById(Long.parseLong(searchHit.getId()));
                    if (medium != null) {
                        medium.sizeInByte = mediaManager.bytesToString(medium.size, false);
                        resultList.add(medium);
                    }
                    break;
                default:
                    Logger.info("no matching case for ID: " + searchHit.getId());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void aggregationStuff() {
        lUserDocuments = new Long(0L);
        lGroupDocuments = new Long(0L);
        lPostDocuments = new Long(0L);
        lMediumDocuments = new Long(0L);

        studycoursesMap = new HashMap<String, Long>();
        degreeMap = new HashMap<String, Long>();
        semesterMap = new HashMap<String, Long>();
        roleMap = new HashMap<String, Long>();
        grouptypeMap = new HashMap<String, Long>();

        Terms terms = this.elasticsearchResponse.getAggregations().get("types");

        Collection<Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            switch (bucket.getKey().toString()) {
                case "user":
                    lUserDocuments = bucket.getDocCount();
                    break;
                case "group":
                    lGroupDocuments = bucket.getDocCount();
                    break;
                case "post":
                    lPostDocuments = bucket.getDocCount();
                    break;
                case "medium":
                    lMediumDocuments = bucket.getDocCount();
                    break;
            }
        }

        if (searchMode.equals("user")) {
            Terms termAggregation = elasticsearchResponse.getAggregations().get("studycourse");
            buckets = termAggregation.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                studycoursesMap.put(bucket.getKey(), bucket.getDocCount());
            }
            termAggregation = elasticsearchResponse.getAggregations().get("degree");
            buckets = termAggregation.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                degreeMap.put(bucket.getKey(), bucket.getDocCount());
            }
            termAggregation = elasticsearchResponse.getAggregations().get("semester");
            buckets = termAggregation.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                semesterMap.put(bucket.getKey(), bucket.getDocCount());
            }
            termAggregation = elasticsearchResponse.getAggregations().get("role");
            buckets = termAggregation.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                roleMap.put(bucket.getKey(), bucket.getDocCount());
            }
        }

        if (searchMode.equals("group")) {
            Terms termAggregation = elasticsearchResponse.getAggregations().get("grouptype");
            buckets = termAggregation.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                grouptypeMap.put(bucket.getKey(), bucket.getDocCount());
            }
        }

    }

    public long getDocumentCount() {
        return lUserDocuments + lGroupDocuments + lPostDocuments + lMediumDocuments;
    }
}

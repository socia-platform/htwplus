package services;

import models.services.ElasticsearchService;
import play.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class ElasticsearchInit implements DatabaseService {

    private ElasticsearchService elasticsearchService;

    @Inject
    public ElasticsearchInit(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
        initialization();
    }

    @Override
    public void initialization() {
        Logger.info("trying to connect to Elasticsearch");
        if (elasticsearchService.isClientAvailable()) {
            Logger.info("... success");
            Logger.info("trying to create HTWPlus index and mapping");
            if (!elasticsearchService.isIndexExists()) {
                elasticsearchService.createAnalyzer();
                elasticsearchService.createMapping();
                Logger.info("... success");
            } else {
                Logger.info("... failed (it already exists?)");
            }
        } else {
            Logger.info("... failed");
        }
    }

}

package services;

import models.services.ElasticsearchService;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class ElasticsearchInit implements DatabaseService {

    private JPAApi jpaApi;
    private ElasticsearchService elasticsearchService;

    @Inject
    public ElasticsearchInit(JPAApi jpaApi, ElasticsearchService elasticsearchService) {
        this.jpaApi = jpaApi;
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

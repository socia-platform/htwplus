package services;

import models.services.ElasticsearchService;
import play.Logger;
import play.api.inject.ApplicationLifecycle;
import play.libs.F;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class ElasticsearchInit implements DatabaseService {

    private ElasticsearchService elasticsearchService;

    @Inject
    public ElasticsearchInit(ElasticsearchService elasticsearchService, ApplicationLifecycle lifecycle) {
        this.elasticsearchService = elasticsearchService;
        initialization();

        // close Elasticsearch connection before shutdown
        lifecycle.addStopHook(() -> {
            elasticsearchService.closeClient();
            return F.Promise.pure(null);
        });
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

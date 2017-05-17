package services;

import models.services.ElasticsearchService;
import play.Logger;
import play.api.inject.ApplicationLifecycle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Iven on 02.12.2015.
 */
@Singleton
public class ElasticsearchInit implements DatabaseService {

    final Logger.ALogger LOG = Logger.of(ElasticsearchInit.class);

    private final ElasticsearchService elasticsearchService;

    @Inject
    public ElasticsearchInit(ElasticsearchService elasticsearchService, ApplicationLifecycle lifecycle) {
        this.elasticsearchService = elasticsearchService;

        // create Elasticsearch connection and do some initialization stuff
        initialization();

        // close Elasticsearch connection before Play shutdown
        lifecycle.addStopHook(() -> {
            elasticsearchService.closeClient();
            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public void initialization() {
        LOG.info("trying to connect to Elasticsearch");
        if (elasticsearchService.isClientAvailable()) {
            LOG.info("... success");
            LOG.info("trying to create HTWPlus index and mapping");
            if (!elasticsearchService.isIndexExists()) {
                elasticsearchService.createAnalyzer();
                elasticsearchService.createMapping();
                LOG.info("... success");
            } else {
                LOG.info("... failed (it already exists?)");
            }
        } else {
            LOG.info("... failed");
        }
    }

}

package modules;

import com.google.inject.AbstractModule;
import models.services.ElasticsearchService;
import models.services.IElasticsearchService;

/**
 * Created by Iven on 28.11.2015.
 */
public class StartupModul extends AbstractModule {
    @Override
    protected void configure() {

         bind(IElasticsearchService.class).to(ElasticsearchService.class).asEagerSingleton();
    }
}

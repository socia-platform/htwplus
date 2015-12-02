package modules;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import models.services.ElasticsearchService;
import services.DatabaseService;
import services.ElasticsearchInit;
import services.PostgresInit;

/**
 * Created by Iven on 28.11.2015.
 */
public class StartupModul extends AbstractModule {
    @Override
    protected void configure() {
        bind(DatabaseService.class).annotatedWith(Names.named("postgres")).to(PostgresInit.class).asEagerSingleton();
        bind(DatabaseService.class).annotatedWith(Names.named("elasticsearch")).to(ElasticsearchInit.class).asEagerSingleton();
    }
}

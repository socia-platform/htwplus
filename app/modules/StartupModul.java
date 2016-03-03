package modules;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import models.services.NotificationService;
import services.DatabaseService;
import services.ElasticsearchInit;
import services.PostgresInit;
import services.ScheduleService;

/**
 * Created by Iven on 28.11.2015.
 */
public class StartupModul extends AbstractModule {
    @Override
    protected void configure() {
        bind(NotificationService.class).asEagerSingleton();
        bind(ScheduleService.class).asEagerSingleton();
        bind(DatabaseService.class).annotatedWith(Names.named("elasticsearch")).to(ElasticsearchInit.class).asEagerSingleton();
        bind(DatabaseService.class).annotatedWith(Names.named("postgres")).to(PostgresInit.class).asEagerSingleton();
    }
}

package services;

import models.services.ElasticsearchService;
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
        try {
            elasticsearchService.createAnalyzer();
            elasticsearchService.createMapping();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

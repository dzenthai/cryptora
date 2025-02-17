package com.dzenthai.cryptora.repository;

import com.dzenthai.cryptora.model.entity.Quote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface QuoteRepository extends MongoRepository<Quote, String> {
}

package com.neoflex.SpringReactiveProject.repo;

import com.neoflex.SpringReactiveProject.domain.Company;
import com.neoflex.SpringReactiveProject.domain.Person;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface CompanyRepository extends R2dbcRepository<Company, Long> {

    Flux<Company> findByCompanyName(String name);
}

package com.neoflex.SpringReactiveProject.controller;

import com.neoflex.SpringReactiveProject.domain.Company;
import com.neoflex.SpringReactiveProject.domain.Person;
import com.neoflex.SpringReactiveProject.repo.CompanyRepository;
import com.neoflex.SpringReactiveProject.repo.PersonRepository;
import com.neoflex.SpringReactiveProject.service.NameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company")
public class CompanyController {
    @Autowired
    private final CompanyRepository companyRepository;
    private final NameGenerator nameGenerator;

    @Autowired
    public CompanyController(CompanyRepository companyRepository,
                             NameGenerator nameGenerator) {
        this.companyRepository = companyRepository;
        this.nameGenerator = nameGenerator;
    }

    @GetMapping
    public Flux<Company> list(@RequestParam(defaultValue = "0") Long start,
                             @RequestParam(defaultValue = "3") Long count) {
        return companyRepository.findAll()
                .skip(start).take(count);
    }

    // find company for each company_name
    @GetMapping(path = "/all", produces = "text/event-stream")
    public Flux<List<String>> getAll() {
        return getStreamOfCompanies()
                .flatMap(companyRepository::findByCompanyName)
                .map(Company::toString)
                .buffer(2);
    }

    @GetMapping(path = "/streamofcompanies", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getStreamOfCompanies() {
        return Flux
                .just("Intel","Microsoft","Oracle", "Apple")
                .delayElements(Duration.ofSeconds(2));
    }


    @GetMapping("/companynames")
    public Flux<String> names(@RequestParam(defaultValue = "0") Long start,
                             @RequestParam(defaultValue = "3") Long count) {
        return companyRepository
                .findAll()
                .skip(start)
                .map(p->p.getCompany_name()+" ");
    }

    @GetMapping(path = "/companies"/*, produces = "text/event-stream"*/)
    public Flux<Company> companies() {
        return nameGenerator.companies()
                .delayElements(Duration.ofSeconds(1))
                .take(Duration.ofSeconds(10));
    }

    @GetMapping(path = "/companystats")
    public Mono<Map<String, Long>> stats() {
        return nameGenerator
                .companies()
                .take(100)
                .groupBy(Company::getCompany_name)
                .flatMap(group -> Mono.zip(
                        Mono.just(group.key()),
                        group.count()))
                .collectMap(k->k.getT1(), v->v.getT2());
    }

    @PostMapping
    public Mono<Company> add(@RequestBody Company company) {
        return companyRepository.save(company);
    }
}

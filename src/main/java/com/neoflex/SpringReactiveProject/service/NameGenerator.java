package com.neoflex.SpringReactiveProject.service;

import com.neoflex.SpringReactiveProject.domain.Company;
import com.neoflex.SpringReactiveProject.domain.Person;
import com.neoflex.SpringReactiveProject.repo.CompanyRepository;
import com.neoflex.SpringReactiveProject.repo.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Random;

@Component
public class NameGenerator {
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CompanyRepository companyRepository;

    static String[] names = {
            "Vasya", "Dima", "Misha", "Sasha"
    };
    static String[] companyNames = {
            "Apple", "Microsoft", "Intel", "Oracle"
    };

    static Random random = new Random();

    public Flux<String> companyNames() {
        Flux<String> streamOfCompanyNames = Flux.generate(fluxSink -> {
            String comnapyName = companyNames[random.nextInt(companyNames.length)];
            fluxSink.next(comnapyName);
        });
        return streamOfCompanyNames;
    }

    public Flux<Company> companies() {
        return names()
                .flatMap(s->companyRepository.findByCompanyName(s));
    }

    public Flux<String> names() {
        Flux<String> stream = Flux.generate(fluxSink -> {
            String name = names[random.nextInt(names.length)];
            fluxSink.next(name);
        });
        return stream;
    }

    public Flux<Person> persons() {
        return names()
                .flatMap(s->personRepository.findByName(s)
                .defaultIfEmpty(new Person(s,"not found")));
    }


}
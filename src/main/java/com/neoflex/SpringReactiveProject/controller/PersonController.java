package com.neoflex.SpringReactiveProject.controller;

import com.neoflex.SpringReactiveProject.domain.Person;
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
@RequestMapping("/person")
public class PersonController {
    private final PersonRepository personRepository;
    private final NameGenerator nameGenerator;

    @Autowired
    public PersonController(PersonRepository personRepository,
                            NameGenerator nameGenerator) {
        this.personRepository = personRepository;
        this.nameGenerator = nameGenerator;
    }

    @GetMapping
    public Flux<Person> list(@RequestParam(defaultValue = "0") Long start,
                             @RequestParam(defaultValue = "3") Long count) {
        return personRepository.findAll()
                .skip(start).take(count);
    }

    // find person for each name
    @GetMapping(path = "/all", produces = "text/event-stream")
    public Flux<List<String>> getAll() {
        return getStream()
                .flatMap(s->personRepository.findByName(s)
                        .defaultIfEmpty(new Person(s,"not found")))
                .map(Person::toString)
                .buffer(2);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getStream() {
        return Flux
                .just("Sasha","Misha","Dima", "Vasya")
                .delayElements(Duration.ofSeconds(2));
    }


    @GetMapping("/names")
    public Flux<String> names(@RequestParam(defaultValue = "0") Long start,
                             @RequestParam(defaultValue = "3") Long count) {
        return personRepository
                .findAll()
                .skip(start)
                .map(p->p.getName()+" ");
    }

    @GetMapping(path = "/persons"/*, produces = "text/event-stream"*/)
    public Flux<Person> persons() {
        return nameGenerator.persons()
                .delayElements(Duration.ofSeconds(1))
                .take(Duration.ofSeconds(10));
    }

    @GetMapping(path = "/stats")
    public Mono<Map<String, Long>> stats() {
        return nameGenerator
                .persons()
                .take(100)
                .groupBy(Person::getName)
                .flatMap(group -> Mono.zip(
                        Mono.just(group.key()),
                        group.count()))
                .collectMap(k->k.getT1(), v->v.getT2());
    }

    @PostMapping
    public Mono<Person> add(@RequestBody Person person) {
        return personRepository.save(person);
    }
}

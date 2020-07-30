package tez.reactive.spring.lernreactiveclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tez.reactive.spring.lernreactiveclient.domain.Item;

@RestController
@Slf4j
public class CatalogController {

    WebClient webClient = WebClient.create("http://localhost:8080");

    @GetMapping("/catalog/retrieve")
    public Flux<Item> getAllItemsUsingRetrieve(){
        return webClient.get().uri("/v1/items")
                .retrieve()//gives body directly
                .bodyToFlux(Item.class)
                .log("Items in 8080 project using retrieve(): ");
    }

    @GetMapping("/catalog/exchange")
    public Flux<Item> getAllItemsUsingExchange(){
        return webClient.get().uri("/v1/items")
                .exchange()//gives response object that would have lot more response detaiils
                .flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class))
                .log("Items in 8080 project using exchange(): ");
    }

    @GetMapping("/catalog/retrieve/item")
    public Mono<Item> getOneItemUsingRetrieve(){
        return webClient.get().uri("/v1/items/{id}", "SAMTAB")
                .retrieve()//gives body directly
                .bodyToMono(Item.class)
                .log("Items in 8080 project using retrieve(): ");
    }

    @GetMapping("/catalog/exchange/item")
    public Mono<Item> getOneItemUsingExchange(){
        return webClient.get().uri("/v1/items/{id}", "SAMTAB")
                .exchange()//gives response object that would have lot more response detaiils
                .flatMap(clientResponse -> clientResponse.bodyToMono(Item.class))
                .log("Items in 8080 project using exchange(): ");
    }

    @PostMapping("/catalog/items")
    public Mono<Item> createItem(@RequestBody Item item){
        return webClient.post().uri("/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(item), Item.class)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Created item is: ");
    }

    @PutMapping("/catalog/items/{id}")
    public Mono<Item> createItem(@PathVariable String id, @RequestBody Item item){
        return webClient.put().uri("/v1/items/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(item), Item.class)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Updated item is: ");
    }

    @DeleteMapping("/catalog/items/{id}")
    public Mono<Void> deleteItem(@PathVariable String id){
        return webClient.delete().uri("/v1/items/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .log("Deleted item is: ");
    }

    @GetMapping("/catalog/retrieve/runtimeException")
    public Flux<Item> errorRetrieve(){
        return webClient.get().uri("/v1/items/runtimeException")
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
                    Mono<String> errorMono = clientResponse.bodyToMono(String.class);
                    return errorMono.flatMap(errorMessage -> {
                        log.error("The error message from server is: " + errorMessage);
                        throw new RuntimeException(errorMessage);
                    });
                })
                .bodyToFlux(Item.class);
    }

    @GetMapping("/catalog/exchange/runtimeException")
    public Flux<Item> errorExchange(){
        return webClient.get().uri("/v1/items/runtimeException")
                .exchange()
                .flatMapMany(clientResponse -> {
                    if(clientResponse.statusCode().is5xxServerError()){
                        return clientResponse.bodyToMono(String.class)
                            .flatMap(errorMessage -> {
                            log.error("The error message from server is - errorExchange: " + errorMessage);
                            throw new RuntimeException(errorMessage);
                        });
                    } else {
                        return clientResponse.bodyToFlux(Item.class);
                    }
                });
    }

}

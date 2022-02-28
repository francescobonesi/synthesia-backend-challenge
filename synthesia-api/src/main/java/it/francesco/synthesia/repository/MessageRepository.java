package it.francesco.synthesia.repository;

import it.francesco.synthesia.tech.challenge.model.Message;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CrudRepository<Message, String> {
}

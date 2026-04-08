package demo.quarkus.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComplaintRepository implements PanacheRepository<Complaint> {
}

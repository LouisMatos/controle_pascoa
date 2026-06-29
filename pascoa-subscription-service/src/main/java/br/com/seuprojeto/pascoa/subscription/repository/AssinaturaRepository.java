package br.com.seuprojeto.pascoa.subscription.repository;

import br.com.seuprojeto.pascoa.subscription.domain.Assinatura;
import br.com.seuprojeto.pascoa.subscription.domain.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, String> {
    Optional<Assinatura> findByStripeSubscriptionId(String stripeSubscriptionId);
    List<Assinatura> findByStatusAndTrialTerminaEmBetween(StatusAssinatura status, LocalDate de, LocalDate ate);
    List<Assinatura> findByStatus(StatusAssinatura status);
}

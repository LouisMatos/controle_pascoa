package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.common.entity.TenantEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public class TenantAwareRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager em;
    private final JpaEntityInformation<T, ?> info;
    private final boolean porTenant;

    public TenantAwareRepository(JpaEntityInformation<T, ?> info, EntityManager em) {
        super(info, em);
        this.em = em;
        this.info = info;
        this.porTenant = TenantEntity.class.isAssignableFrom(info.getJavaType());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        if (!porTenant) {
            return super.findById(id);
        }
        String atributoId = info.getRequiredIdAttribute().getName();
        return em.createQuery("select e from " + info.getEntityName() + " e where e." + atributoId + " = :id",
                info.getJavaType())
            .setParameter("id", id)
            .getResultStream()
            .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public T getReferenceById(ID id) {
        if (!porTenant) {
            return super.getReferenceById(id);
        }
        return findById(id).orElseThrow(
            () -> new EntityNotFoundException(info.getEntityName() + " não encontrado: " + id));
    }
}

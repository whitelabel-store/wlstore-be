package com.wlstore.pricing_service.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    /**
     * Pega e TRAVA os próximos eventos ainda não publicados, do mais antigo para o mais novo.
     * SKIP LOCKED: se outra instância já travou uma linha, pula para a próxima em vez de esperar.
     * Só funciona dentro de uma transação (o lock dura até o commit).
     * Usa o índice parcial idx_outbox_unpublished criado na V4.
     */
    @Query(value = """
            select * from tb_outbox_event
            where published_at is null
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockNextUnpublished(@Param("limit") int limit);
}

package br.com.seuprojeto.pascoa.crm.service;

import br.com.seuprojeto.pascoa.crm.entity.CampanhaItem;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CampanhaQueue {

    private final ConcurrentLinkedQueue<CampanhaItem> fila = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalEnfileirados = new AtomicLong();
    private final AtomicLong totalEnviados     = new AtomicLong();
    private final AtomicLong totalFalhas       = new AtomicLong();

    public void enqueue(CampanhaItem item) {
        fila.offer(item);
        totalEnfileirados.incrementAndGet();
    }

    public CampanhaItem poll() { return fila.poll(); }

    public int pendentes()           { return fila.size(); }
    public long totalEnfileirados()  { return totalEnfileirados.get(); }
    public long totalEnviados()      { return totalEnviados.get(); }
    public long totalFalhas()        { return totalFalhas.get(); }

    public void registrarEnvio()  { totalEnviados.incrementAndGet(); }
    public void registrarFalha()  { totalFalhas.incrementAndGet(); }
}

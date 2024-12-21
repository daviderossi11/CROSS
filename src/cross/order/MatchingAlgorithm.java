package cross.order;

import java.util.*;

public class MatchingAlgorithm {

    public static void matchOrder(Order order, List<LimitOrder> orders) {
        int remainingSize = order.getSize();
        Iterator<LimitOrder> iterator = orders.iterator();

        while (iterator.hasNext() && remainingSize > 0) {
            LimitOrder limitOrder = iterator.next();

            // Salta ordini già eseguiti o cancellati
            if (limitOrder.getStatus() == OrderStatus.FILLED || limitOrder.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }

            // Controlla compatibilità di prezzo
            boolean priceMatch = (order.getType().equals("bid") && order.getSize() >= limitOrder.getLimitPrice()) ||
                                 (order.getType().equals("ask") && order.getSize() <= limitOrder.getLimitPrice());

            if (priceMatch) {
                if (remainingSize >= limitOrder.getSize()) {
                    // Esecuzione completa
                    remainingSize -= limitOrder.getSize();
                    limitOrder.setStatus(OrderStatus.FILLED);
                    iterator.remove();
                } else {
                    // Esecuzione completa per il limitOrder e uscita
                    limitOrder.setStatus(OrderStatus.FILLED);
                    iterator.remove();
                    remainingSize = 0; // Ordine soddisfatto, esci dal ciclo
                }
            }
        }

        // Aggiorna lo stato finale dell'ordine
        if (remainingSize == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }
}

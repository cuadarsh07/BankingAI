package in.ashish.combined.service;

import in.ashish.combined.dto.TransferRequest;
import in.ashish.combined.dto.TransferResponse;

/**
 * Service interface for funds transfer operations between accounts.
 */
public interface TransferService {

    /**
     * Executes an atomic, transactional money transfer between two accounts.
     *
     * @param request the validated transfer request
     * @return transfer confirmation and updated source balance
     */
    TransferResponse transferMoney(TransferRequest request);
}

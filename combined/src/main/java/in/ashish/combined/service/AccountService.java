package in.ashish.combined.service;

import in.ashish.combined.dto.AccountResponse;
import in.ashish.combined.dto.CreateAccountRequest;

import java.util.List;

/**
 * Service interface defining business operations for Bank Accounts.
 */
public interface AccountService {

    /**
     * Creates a new bank account after validating uniqueness and business rules.
     *
     * @param request the validated account creation payload
     * @return the created Account details
     */
    AccountResponse createAccount(CreateAccountRequest request);

    /**
     * Retrieves an account by its database primary key.
     *
     * @param id the unique ID of the account
     * @return the Account details
     * @throws in.ashish.combined.exception.ResourceNotFoundException if account does not exist
     */
    AccountResponse getAccountById(Long id);

    /**
     * Retrieves an account by its unique account number.
     *
     * @param accountNumber the unique account number
     * @return the Account details
     * @throws in.ashish.combined.exception.ResourceNotFoundException if account does not exist
     */
    AccountResponse getAccountByAccountNumber(String accountNumber);

    /**
     * Retrieves all accounts in the system.
     *
     * @return list of all accounts
     */
    List<AccountResponse> getAllAccounts();

    /**
     * Full replacement update of account mutable profile details (PUT).
     *
     * @param id the unique ID of the account
     * @param request the validated update payload
     * @return updated Account details
     */
    AccountResponse updateAccount(Long id, in.ashish.combined.dto.UpdateAccountRequest request);

    /**
     * Partial update of account details (PATCH).
     *
     * @param id the unique ID of the account
     * @param request the partial update payload
     * @return updated Account details
     */
    AccountResponse patchAccount(Long id, in.ashish.combined.dto.PatchAccountRequest request);

    /**
     * Deposit funds into an account.
     *
     * @param id the unique ID of the account
     * @param request the validated transaction payload
     * @return transaction confirmation and updated balances
     */
    in.ashish.combined.dto.TransactionResponse deposit(Long id, in.ashish.combined.dto.TransactionRequest request);

    /**
     * Withdraw funds from an account after verifying available balance.
     *
     * @param id the unique ID of the account
     * @param request the validated transaction payload
     * @return transaction confirmation and updated balances
     */
    in.ashish.combined.dto.TransactionResponse withdraw(Long id, in.ashish.combined.dto.TransactionRequest request);

    /**
     * Retrieve the current balance of an account.
     *
     * @param id the unique ID of the account
     * @return balance details
     */
    in.ashish.combined.dto.BalanceResponse getBalance(Long id);
}

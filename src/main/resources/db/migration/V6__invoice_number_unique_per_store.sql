-- Invoice numbers are generated as a per-store daily sequence (INVyyMMddNNNNN, see
-- InvoiceNumberService, which uses a per-store Redis counter). The same invoice number therefore
-- legitimately recurs across different stores, so a GLOBAL unique constraint on invoice_number is
-- incorrect: the second store to record a sale on a given day fails with a duplicate-key error.
--
-- Replace the global unique constraint with a composite unique constraint on (store_id, invoice_number),
-- which matches the per-store numbering semantics and still prevents duplicates within a single store.
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_invoice_number_key;
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_store_invoice UNIQUE (store_id, invoice_number);
